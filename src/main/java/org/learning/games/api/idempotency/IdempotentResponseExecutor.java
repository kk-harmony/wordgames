package org.learning.games.api.idempotency;

import java.util.function.Supplier;

import org.learning.games.api.dto.GameResponse;
import org.learning.games.domain.IdempotencyOperation;
import org.learning.games.domain.IdempotencyService;
import org.learning.games.domain.exception.ConflictException;
import org.learning.games.domain.model.IdempotentResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class IdempotentResponseExecutor {

	@Inject
	IdempotencyService idempotencyService;

	@Inject
	ObjectMapper objectMapper;

	public Response execute(
			Long gameId,
			String userId,
			String idempotencyKey,
			IdempotencyOperation operation,
			String requestHash,
			Supplier<GameResponse> action) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			return Response.ok(action.get()).build();
		}

		idempotencyService.validateKey(idempotencyKey);
		var replay = idempotencyService.findReplay(userId, idempotencyKey, operation, requestHash);
		if (replay.isPresent()) {
			return toResponse(replay.get());
		}

		GameResponse response;
		try {
			response = action.get();
		} catch (RuntimeException ex) {
			replay = idempotencyService.findReplay(userId, idempotencyKey, operation, requestHash);
			if (replay.isPresent()) {
				return toResponse(replay.get());
			}
			throw ex;
		}
		try {
			String body = objectMapper.writeValueAsString(response);
			IdempotentResult stored = idempotencyService.store(
					userId, idempotencyKey, operation, gameId, requestHash, 200, body);
			return toResponse(stored);
		} catch (JsonProcessingException e) {
			throw new ConflictException("Response could not be serialized for idempotency");
		}
	}

	private Response toResponse(IdempotentResult result) {
		try {
			GameResponse response = objectMapper.readValue(result.responseBody(), GameResponse.class);
			return Response.status(result.status()).entity(response).build();
		} catch (JsonProcessingException e) {
			throw new ConflictException("Stored idempotent response could not be read");
		}
	}
}
