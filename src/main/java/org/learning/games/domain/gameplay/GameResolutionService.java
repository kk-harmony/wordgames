package org.learning.games.domain.gameplay;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.learning.games.domain.GameMemberRepository;
import org.learning.games.domain.GameMetrics;
import org.learning.games.domain.GameRandom;
import org.learning.games.domain.exception.NotFoundException;
import org.learning.games.domain.model.GameOutcome;
import org.learning.games.domain.model.GameStatus;
import org.learning.games.entity.Game;
import org.learning.games.entity.GameMember;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GameResolutionService {

	private static final Logger LOG = Logger.getLogger(GameResolutionService.class.getName());

	@Inject
	GameAccess gameAccess;

	@Inject
	GameMemberRepository memberRepository;

	@Inject
	GameRandom gameRandom;

	@Inject
	GameMetrics gameMetrics;

	public void finishGame(Game game, GameOutcome outcome) {
		game.status = GameStatus.FINISHED;
		game.outcome = outcome;
		game.currentTurnUserId = null;
		gameMetrics.recordGameFinished();
		LOG.info(() -> "game.finished gameId=" + game.id + " outcome=" + outcome);
	}

	public void setCurrentTurnToFirst(Game game) {
		List<GameMember> members = gameAccess.orderedActiveMembers(game.id);
		if (members.isEmpty()) {
			game.currentTurnUserId = null;
			return;
		}
		game.currentTurnUserId = members.get(0).userId;
	}

	public void transitionToVotingOrNextTurn(Game game) {
		List<GameMember> members = gameAccess.orderedActiveMembers(game.id);
		GameMember next = members.stream()
				.filter(member -> !member.turnCompleted)
				.findFirst()
				.orElse(null);

		if (next != null) {
			game.currentTurnUserId = next.userId;
			return;
		}

		game.status = GameStatus.VOTING;
		game.voteResetCount = 0;
		game.currentTurnUserId = null;
		resetVotes(members);
	}

	public void resolveVotes(Game game, List<GameMember> activeMembers) {
		Map<String, Long> voteCounts = activeMembers.stream()
				.collect(Collectors.groupingBy(m -> m.votedForUserId, Collectors.counting()));

		long maxVotes = voteCounts.values().stream().max(Comparator.naturalOrder()).orElse(0L);
		List<String> tiedUserIds = voteCounts.entrySet().stream()
				.filter(entry -> entry.getValue() == maxVotes)
				.map(Map.Entry::getKey)
				.toList();

		if (tiedUserIds.size() > 1) {
			if (game.voteResetCount < GameRules.MAX_VOTE_RESETS) {
				game.voteResetCount++;
				resetVotes(activeMembers);
				LOG.info(() -> "vote.tie_reset gameId=" + game.id + " resetCount=" + game.voteResetCount);
				return;
			}

			String eliminatedUserId = tiedUserIds.get(gameRandom.nextInt(tiedUserIds.size()));
			eliminatePlayer(game, eliminatedUserId);
			return;
		}

		eliminatePlayer(game, tiedUserIds.get(0));
	}

	public void eliminatePlayer(Game game, String eliminatedUserId) {
		GameMember eliminated = memberRepository.findByGameAndUser(game.id, eliminatedUserId)
				.orElseThrow(() -> new NotFoundException("User is not a member of this game"));
		eliminated.eliminated = true;
		LOG.info(() -> "player.eliminated gameId=" + game.id + " eliminatedUserId=" + eliminatedUserId);

		if (eliminatedUserId.equals(game.impostorUserId)) {
			finishGame(game, GameOutcome.IMPOSTOR_IDENTIFIED);
			return;
		}

		List<GameMember> remaining = gameAccess.activeMembers(game.id);
		if (remaining.size() < GameRules.MIN_ACTIVE_PLAYERS_FOR_NEXT_ROUND) {
			finishGame(game, GameOutcome.IMPOSTOR_SURVIVED);
			return;
		}

		startNextRound(game);
	}

	public void startNextRound(Game game) {
		game.currentRound++;
		game.voteResetCount = 0;
		game.status = GameStatus.IN_PROGRESS;

		for (GameMember member : gameAccess.activeMembers(game.id)) {
			member.turnCompleted = false;
			member.votedForUserId = null;
		}

		setCurrentTurnToFirst(game);
	}

	private void resetVotes(List<GameMember> members) {
		for (GameMember member : members) {
			member.votedForUserId = null;
		}
	}
}
