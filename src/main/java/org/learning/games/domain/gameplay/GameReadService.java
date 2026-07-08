package org.learning.games.domain.gameplay;

import org.learning.games.domain.GameMemberRepository;
import org.learning.games.domain.exception.BadRequestException;
import org.learning.games.domain.exception.NotFoundException;
import org.learning.games.domain.model.AssignedWord;
import org.learning.games.domain.model.GameStatus;
import org.learning.games.domain.model.WordType;
import org.learning.games.entity.Game;
import org.learning.games.entity.GameMember;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GameReadService {

	@Inject
	GameAccess gameAccess;

	@Inject
	GameMemberRepository memberRepository;

	public Game getGameForMember(Long gameId, String userId) {
		Game game = gameAccess.requireGameWithMembers(gameId);
		gameAccess.requireMember(gameId, userId);
		return game;
	}

	public AssignedWord getMyWord(Long gameId, String userId) {
		Game game = gameAccess.requireGame(gameId);
		gameAccess.requireMember(gameId, userId);

		if (game.status == GameStatus.WAITING) {
			throw new BadRequestException("Game has not started");
		}

		GameMember member = memberRepository.findByGameAndUser(gameId, userId)
				.orElseThrow(() -> new NotFoundException("User is not a member of this game"));

		if (member.assignedWord == null) {
			throw new BadRequestException("No word assigned");
		}

		WordType type = member.userId.equals(game.impostorUserId)
				? WordType.IMPOSED
				: WordType.AUTHENTIC;

		return new AssignedWord(member.assignedWord, type);
	}
}
