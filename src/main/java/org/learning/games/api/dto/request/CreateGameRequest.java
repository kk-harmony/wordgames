package org.learning.games.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateGameRequest {
	@NotBlank
	@Size(min = 1, max = 100)
	public String name;

	@Size(max = 30)
	public String displayName;
}
