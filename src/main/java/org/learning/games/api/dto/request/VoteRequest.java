package org.learning.games.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public class VoteRequest {
	@NotBlank
	public String votedUserId;
}
