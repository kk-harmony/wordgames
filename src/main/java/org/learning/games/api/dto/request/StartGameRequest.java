package org.learning.games.api.dto.request;

import jakarta.validation.constraints.NotNull;

public class StartGameRequest {
	@NotNull
	public Long secretWordId;
}
