package org.learning.games.api.mapper;

import org.learning.games.api.dto.GameMemberResponse;
import org.learning.games.api.dto.GameResponse;
import org.learning.games.api.dto.MyWordResponse;
import org.learning.games.domain.model.AssignedWord;
import org.learning.games.domain.model.GameStatus;
import org.learning.games.entity.Game;
import org.learning.games.entity.GameMember;

public final class GameMapper {

	private GameMapper() {
	}

	public static GameResponse toResponse(Game game) {
		GameResponse response = new GameResponse();
		response.id = game.id;
		response.name = game.name;
		response.adminUserId = game.adminUserId;
		response.status = game.status;
		response.outcome = game.outcome;
		response.currentRound = game.currentRound;
		response.voteResetCount = game.voteResetCount;
		response.currentTurnUserId = game.currentTurnUserId;
		response.impostorUserId = game.status == GameStatus.FINISHED ? game.impostorUserId : null;
		// Mid-game: word pair stays admin-only via /secret-words. Finished: all members see both.
		response.secretWord = game.status == GameStatus.FINISHED && game.secretWord != null
				? SecretWordMapper.toResponse(game.secretWord)
				: null;
		if (game.members != null) {
			response.members = game.members.stream().map(GameMapper::toMemberResponse).toList();
		}
		return response;
	}

	public static GameMemberResponse toMemberResponse(GameMember member) {
		GameMemberResponse response = new GameMemberResponse();
		response.id = member.id;
		response.userId = member.userId;
		response.displayName = member.displayName;
		response.role = member.role;
		response.turnCompleted = member.turnCompleted;
		response.eliminated = member.eliminated;
		return response;
	}

	public static MyWordResponse toMyWordResponse(AssignedWord assignedWord) {
		return new MyWordResponse(assignedWord.word(), assignedWord.type());
	}
}
