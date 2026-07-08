package org.learning.games.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateSecretWordRequest {
	@NotBlank
	@Size(min = 1, max = 50)
	public String authentic;

	@NotBlank
	@Size(min = 1, max = 50)
	public String imposed;
}
