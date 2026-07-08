package org.learning.games.domain;

import java.util.Optional;

import org.learning.games.domain.exception.BadRequestException;
import org.learning.games.domain.exception.ConflictException;
import org.learning.games.domain.exception.DomainException;
import org.learning.games.domain.exception.IdempotencyWriteConflictException;
import org.learning.games.domain.model.IdempotentResult;
import org.learning.games.domain.model.StoredIdempotency;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class IdempotencyService {

	private static final int MAX_KEY_LENGTH = 128;
	private static final int MAX_REPLAY_ATTEMPTS = 10;
	private static final long INITIAL_BACKOFF_MS = 5;
	private static final long MAX_BACKOFF_MS = 100;

	@Inject
	IdempotencyRepository repository;

	@Inject
	IdempotencyRecordWriter recordWriter;

	@Inject
	IdempotencyReplayReader replayReader;

	public void validateKey(String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw new BadRequestException("Idempotency-Key must not be blank");
		}
		if (idempotencyKey.length() > MAX_KEY_LENGTH) {
			throw new BadRequestException("Idempotency-Key must be at most " + MAX_KEY_LENGTH + " characters");
		}
	}

	@Transactional
	public Optional<IdempotentResult> findReplay(
			String userId,
			String idempotencyKey,
			IdempotencyOperation operation,
			String requestHash) {
		return repository.findByUserAndKey(userId, idempotencyKey.trim())
				.map(record -> toResult(record, operation, requestHash));
	}

	@Transactional
	public IdempotentResult store(
			String userId,
			String idempotencyKey,
			IdempotencyOperation operation,
			Long gameId,
			String requestHash,
			int responseStatus,
			String responseBody) {
		Optional<IdempotentResult> existing = findReplay(userId, idempotencyKey, operation, requestHash);
		if (existing.isPresent()) {
			return existing.get();
		}

		try {
			return recordWriter.write(
					userId, idempotencyKey, operation, gameId, requestHash, responseStatus, responseBody);
		} catch (IdempotencyWriteConflictException ex) {
			return waitForReplay(userId, idempotencyKey, operation, requestHash)
					.orElseThrow(() -> new ConflictException("Idempotency record conflict could not be resolved"));
		}
	}

	private Optional<IdempotentResult> waitForReplay(
			String userId,
			String idempotencyKey,
			IdempotencyOperation operation,
			String requestHash) {
		long backoffMs = INITIAL_BACKOFF_MS;
		for (int attempt = 0; attempt < MAX_REPLAY_ATTEMPTS; attempt++) {
			Optional<IdempotentResult> replay = replayReader.findRecord(userId, idempotencyKey)
					.map(record -> toResult(record, operation, requestHash));
			if (replay.isPresent()) {
				return replay;
			}
			if (attempt == MAX_REPLAY_ATTEMPTS - 1) {
				break;
			}
			try {
				Thread.sleep(backoffMs);
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				break;
			}
			backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
		}
		return Optional.empty();
	}

	private IdempotentResult toResult(StoredIdempotency record, IdempotencyOperation operation, String requestHash) {
		if (!record.operation().equals(operation.name()) || !record.requestHash().equals(requestHash)) {
			throw new DomainException("IDEMPOTENCY_KEY_REUSED", "Idempotency key was already used with a different request");
		}
		return new IdempotentResult(record.responseStatus(), record.responseBody());
	}
}
