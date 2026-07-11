package org.learning.games.api.dto;

import org.learning.games.domain.model.GameOutcome;
import org.learning.games.domain.model.GameStatus;

public class GameResponse {
	public Long id;
	public String name;
	public String adminUserId;
	public GameStatus status;
	public GameOutcome outcome;
	public int currentRound;
	public int voteResetCount;
	public String currentTurnUserId;
	public java.util.List<GameMemberResponse> members;
	public String impostorUserId;
	/** Present only when finished so members can verify both words (not mid-game). */
	public SecretWordResponse secretWord;
}
