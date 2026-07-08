package org.learning.games.infra;

import org.learning.games.domain.IdempotencyOperation;
import org.learning.games.domain.IdempotencyRecordWriter;
import org.learning.games.domain.exception.IdempotencyWriteConflictException;
import org.learning.games.domain.model.IdempotentResult;
import org.learning.games.entity.IdempotencyRecord;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class JpaIdempotencyRecordWriter implements IdempotencyRecordWriter {

	@Inject
	JpaIdempotencyRepository repository;

	@Override
	@Transactional(Transactional.TxType.REQUIRES_NEW)
	public IdempotentResult write(
			String userId,
			String idempotencyKey,
			IdempotencyOperation operation,
			Long gameId,
			String requestHash,
			int responseStatus,
			String responseBody) {
		IdempotencyRecord record = new IdempotencyRecord();
		record.idempotencyKey = idempotencyKey.trim();
		record.userId = userId;
		record.operation = operation.name();
		record.gameId = gameId;
		record.requestHash = requestHash;
		record.responseStatus = responseStatus;
		record.responseBody = responseBody;
		try {
			repository.persist(record);
		} catch (PersistenceException ex) {
			if (isUniqueConstraintViolation(ex)) {
				throw new IdempotencyWriteConflictException(ex);
			}
			throw ex;
		}
		return new IdempotentResult(responseStatus, responseBody);
	}

	private boolean isUniqueConstraintViolation(PersistenceException ex) {
		Throwable cause = ex;
		while (cause != null) {
			String message = cause.getMessage();
			if (message != null
					&& message.contains("duplicate key")
					&& message.contains("idempotency_key")) {
				return true;
			}
			if (message != null && message.contains("uk_idempotency_user_key")) {
				return true;
			}
			cause = cause.getCause();
		}
		return false;
	}
}
