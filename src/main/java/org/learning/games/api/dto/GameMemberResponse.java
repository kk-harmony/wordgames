package org.learning.games.api.dto;

import org.learning.games.domain.model.MemberRole;

public class GameMemberResponse {
	public Long id;
	public String userId;
	public String displayName;
	public MemberRole role;
	public boolean turnCompleted;
	public boolean eliminated;
}
