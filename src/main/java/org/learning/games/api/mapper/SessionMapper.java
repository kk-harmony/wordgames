package org.learning.games.api.mapper;

import org.learning.games.api.dto.SessionMemberResponse;
import org.learning.games.api.dto.SessionResponse;
import org.learning.games.domain.gameplay.SessionRules;
import org.learning.games.domain.model.GameStatus;
import org.learning.games.entity.GameSession;
import org.learning.games.entity.SessionMember;

public final class SessionMapper {

	private SessionMapper() {
	}

	public static SessionResponse toResponse(GameSession session, GameStatus currentGameStatus) {
		SessionResponse response = new SessionResponse();
		response.id = session.joinCode;
		response.name = session.name;
		response.adminUserId = session.adminUserId;
		response.status = session.status;
		response.gamesStartedCount = session.gamesStartedCount;
		response.maxGames = SessionRules.MAX_GAMES;
		response.currentGameId = session.currentGameId;
		response.currentGameStatus = currentGameStatus;
		if (session.members != null) {
			response.members = session.members.stream().map(SessionMapper::toMemberResponse).toList();
		}
		return response;
	}

	public static SessionMemberResponse toMemberResponse(SessionMember member) {
		SessionMemberResponse response = new SessionMemberResponse();
		response.id = member.id;
		response.userId = member.userId;
		response.displayName = member.displayName;
		response.role = member.role;
		return response;
	}
}
