package org.learning.games.api.dto;

import org.learning.games.domain.model.GameStatus;
import org.learning.games.domain.model.SessionStatus;

public class SessionResponse {
	/** Public 5-character join code shared with players. */
	public String id;
	public String name;
	public String adminUserId;
	public SessionStatus status;
	public int gamesStartedCount;
	public int maxGames;
	public Long currentGameId;
	public GameStatus currentGameStatus;
	public java.util.List<SessionMemberResponse> members;
}
