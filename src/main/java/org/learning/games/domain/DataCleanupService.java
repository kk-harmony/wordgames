package org.learning.games.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Logger;

import org.learning.games.domain.model.CleanupResult;
import org.learning.games.domain.model.GameStatus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DataCleanupService {

	private static final Logger LOG = Logger.getLogger(DataCleanupService.class.getName());

	@Inject
	DataCleanupRepository cleanupRepository;

	@Inject
	DataCleanupSettings settings;

	@Inject
	CleanupMetrics metrics;

	@Transactional
	public CleanupResult run() {
		try (CleanupMetrics.RunTimer ignored = metrics.startRunTimer()) {
			boolean dryRun = settings.isDryRun();
			Instant idempotencyCutoff = Instant.now().minus(settings.getIdempotencyRetentionDays(), ChronoUnit.DAYS);
			Instant waitingCutoff = Instant.now().minus(settings.getWaitingRetentionDays(), ChronoUnit.DAYS);
			Instant finishedCutoff = Instant.now().minus(settings.getFinishedRetentionDays(), ChronoUnit.DAYS);

			CleanupResult result = runIdempotencyCleanup(idempotencyCutoff, dryRun)
					.add(runGameCleanup(GameStatus.WAITING, waitingCutoff, dryRun))
					.add(runGameCleanup(GameStatus.FINISHED, finishedCutoff, dryRun));

			recordDeletedRows("idempotency_record", result.idempotencyRecordsDeleted());
			recordDeletedRows("game", result.gamesDeleted());
			recordDeletedRows("gamemember", result.gameMembersDeleted());
			recordDeletedRows("idempotency_record_by_game", result.gameIdempotencyRecordsDeleted());

			LOG.info(() -> String.format(
					"cleanup.run completed dryRun=%s idempotencyDeleted=%d gamesDeleted=%d membersDeleted=%d gameIdempotencyDeleted=%d total=%d",
					dryRun,
					result.idempotencyRecordsDeleted(),
					result.gamesDeleted(),
					result.gameMembersDeleted(),
					result.gameIdempotencyRecordsDeleted(),
					result.totalRowsAffected()));

			return result;
		}
	}

	private CleanupResult runIdempotencyCleanup(Instant cutoff, boolean dryRun) {
		if (dryRun) {
			int count = cleanupRepository.countIdempotencyRecordsOlderThan(cutoff);
			LOG.info(() -> String.format(
					"cleanup.phase idempotency dryRun=true wouldDelete=%d cutoff=%s",
					count,
					cutoff));
			return new CleanupResult(count, 0, 0, 0, true);
		}

		int totalDeleted = 0;
		for (int batch = 0; batch < settings.getMaxBatchesPerRun(); batch++) {
			int deleted = cleanupRepository.deleteIdempotencyRecordsOlderThan(cutoff, settings.getBatchSize());
			if (deleted == 0) {
				break;
			}
			totalDeleted += deleted;
			int batchNumber = batch + 1;
			int batchDeleted = deleted;
			int runningTotal = totalDeleted;
			LOG.info(() -> String.format(
					"cleanup.phase idempotency batch=%d deleted=%d total=%d",
					batchNumber,
					batchDeleted,
					runningTotal));
			if (deleted < settings.getBatchSize()) {
				break;
			}
			sleepBetweenBatches();
		}
		return new CleanupResult(totalDeleted, 0, 0, 0, false);
	}

	private CleanupResult runGameCleanup(GameStatus status, Instant cutoff, boolean dryRun) {
		if (dryRun) {
			int gameCount = cleanupRepository.countEligibleGames(status, cutoff);
			List<Long> gameIds = cleanupRepository.findEligibleGameIds(status, cutoff, Integer.MAX_VALUE);
			int memberCount = cleanupRepository.countGameMembersByGameIds(gameIds);
			int idempotencyCount = cleanupRepository.countIdempotencyByGameIds(gameIds);
			LOG.info(() -> String.format(
					"cleanup.phase games status=%s dryRun=true wouldDeleteGames=%d wouldDeleteMembers=%d wouldDeleteIdempotency=%d cutoff=%s",
					status,
					gameCount,
					memberCount,
					idempotencyCount,
					cutoff));
			return new CleanupResult(0, gameCount, memberCount, idempotencyCount, true);
		}

		int totalGames = 0;
		int totalMembers = 0;
		int totalIdempotency = 0;

		for (int batch = 0; batch < settings.getMaxBatchesPerRun(); batch++) {
			List<Long> gameIds = cleanupRepository.findEligibleGameIds(status, cutoff, settings.getBatchSize());
			if (gameIds.isEmpty()) {
				break;
			}

			int idempotencyDeleted = cleanupRepository.deleteIdempotencyByGameIds(gameIds);
			int membersDeleted = cleanupRepository.deleteGameMembersByGameIds(gameIds);
			int gamesDeleted = cleanupRepository.deleteGamesByIds(gameIds);

			totalGames += gamesDeleted;
			totalMembers += membersDeleted;
			totalIdempotency += idempotencyDeleted;

			int batchNumber = batch + 1;
			int batchGamesDeleted = gamesDeleted;
			int batchMembersDeleted = membersDeleted;
			int batchIdempotencyDeleted = idempotencyDeleted;
			LOG.info(() -> String.format(
					"cleanup.phase games status=%s batch=%d deletedGames=%d deletedMembers=%d deletedIdempotency=%d",
					status,
					batchNumber,
					batchGamesDeleted,
					batchMembersDeleted,
					batchIdempotencyDeleted));

			if (gameIds.size() < settings.getBatchSize()) {
				break;
			}
			sleepBetweenBatches();
		}

		return new CleanupResult(0, totalGames, totalMembers, totalIdempotency, false);
	}

	private void sleepBetweenBatches() {
		long sleepMs = settings.getInterBatchSleepMs();
		if (sleepMs <= 0) {
			return;
		}
		try {
			Thread.sleep(sleepMs);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOG.warning("cleanup interrupted during inter-batch sleep");
		}
	}

	private void recordDeletedRows(String table, int count) {
		metrics.recordDeletedRows(table, count);
	}
}
