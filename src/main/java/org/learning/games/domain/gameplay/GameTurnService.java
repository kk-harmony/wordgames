package org.learning.games.domain.gameplay;

import org.learning.games.domain.GameMemberRepository;
import org.learning.games.domain.exception.BadRequestException;
import org.learning.games.domain.exception.NotFoundException;
import org.learning.games.domain.model.GameStatus;
import org.learning.games.entity.Game;
import org.learning.games.entity.GameMember;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GameTurnService {

	@Inject
	GameAccess gameAccess;

	@Inject
	GameMemberRepository memberRepository;

	@Inject
	GameResolutionService resolutionService;

	public Game completeTurn(Long gameId, String userId) {
		Game game = gameAccess.requireGame(gameId);
		gameAccess.requireMember(gameId, userId);

		if (game.status != GameStatus.IN_PROGRESS) {
			throw new BadRequestException("Game is not in progress");
		}

		if (game.currentTurnUserId == null) {
			resolutionService.setCurrentTurnToFirst(game);
		}

		if (!userId.equals(game.currentTurnUserId)) {
			throw new BadRequestException("Not your turn");
		}

		GameMember member = memberRepository.findByGameAndUser(gameId, userId)
				.orElseThrow(() -> new NotFoundException("User is not a member of this game"));

		if (member.eliminated) {
			throw new BadRequestException("Eliminated players cannot complete a turn");
		}

		if (member.turnCompleted) {
			throw new BadRequestException("Turn already completed");
		}

		member.turnCompleted = true;
		resolutionService.transitionToVotingOrNextTurn(game);

		return gameAccess.reloadGame(gameId);
	}
}
