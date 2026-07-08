package org.learning.games.api.mapper;

import org.learning.games.api.dto.SecretWordResponse;
import org.learning.games.entity.SecretWord;

public final class SecretWordMapper {

	private SecretWordMapper() {
	}

	public static SecretWordResponse toResponse(SecretWord secretWord) {
		SecretWordResponse response = new SecretWordResponse();
		response.id = secretWord.id;
		response.authentic = secretWord.authentic;
		response.imposed = secretWord.imposed;
		return response;
	}
}
