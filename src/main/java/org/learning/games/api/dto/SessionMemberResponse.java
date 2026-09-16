package org.learning.games.api.dto;

import org.learning.games.domain.model.MemberRole;

public class SessionMemberResponse {
	public Long id;
	public String userId;
	public String displayName;
	public MemberRole role;
}
