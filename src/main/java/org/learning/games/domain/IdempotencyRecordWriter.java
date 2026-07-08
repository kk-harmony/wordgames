package org.learning.games.domain;

import org.learning.games.domain.model.IdempotentResult;

public interface IdempotencyRecordWriter {

	IdempotentResult write(
			String userId,
			String idempotencyKey,
			IdempotencyOperation operation,
			Long gameId,
			String requestHash,
			int responseStatus,
			String responseBody);
}
