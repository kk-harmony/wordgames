package org.learning.games.domain.gameplay;

import java.util.List;
import java.util.logging.Logger;

import org.learning.games.domain.GameMemberRepository;
import org.learning.games.domain.GameMetrics;
import org.learning.games.domain.GameRandom;
import org.learning.games.domain.SecretWordRepository;
import org.learning.games.domain.exception.BadRequestException;
import org.learning.games.domain.exception.NotFoundException;
import org.learning.games.domain.model.GameStatus;
import org.learning.games.entity.Game;
import org.learning.games.entity.GameMember;
import org.learning.games.entity.SecretWord;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GameStartService {

	private static final Logger LOG = Logger.getLogger(GameStartService.class.getName());

	@Inject
	GameAccess gameAccess;

	@Inject
	GameMemberRepository memberRepository;

	@Inject
	SecretWordRepository secretWordRepository;

	@Inject
	GameRandom gameRandom;

	@Inject
	GameMetrics gameMetrics;

	@Inject
	GameResolutionService resolutionService;

	public Game startGame(Long gameId, String userId, Long secretWordId) {
		Game game = gameAccess.requireGame(gameId);
		gameAccess.requireAdmin(game, userId);

		if (game.status != GameStatus.WAITING) {
			throw new BadRequestException("Game has already started");
		}

		List<GameMember> members = memberRepository.findByGame(gameId);
		if (members.size() < GameRules.MIN_PLAYERS) {
			throw new BadRequestException(
					"At least " + GameRules.MIN_PLAYERS + " players are required to start the game");
		}

		SecretWord secretWord = secretWordRepository.findById(secretWordId)
				.orElseThrow(() -> new NotFoundException("SecretWord " + secretWordId + " not found"));

		int impostorIndex = gameRandom.nextInt(members.size());
		GameMember impostor = members.get(impostorIndex);

		for (GameMember member : members) {
			if (member.userId.equals(impostor.userId)) {
				member.assignedWord = secretWord.imposed;
			} else {
				member.assignedWord = secretWord.authentic;
			}
			member.turnCompleted = false;
			member.votedForUserId = null;
			member.eliminated = false;
		}

		game.secretWord = secretWord;
		game.impostorUserId = impostor.userId;
		game.status = GameStatus.IN_PROGRESS;
		game.currentRound = 1;
		game.voteResetCount = 0;
		game.outcome = null;
		resolutionService.setCurrentTurnToFirst(game);

		gameMetrics.recordGameStarted();
		LOG.info(() -> "game.started gameId=" + gameId + " adminUserId=" + userId + " players=" + members.size());

		return gameAccess.reloadGame(gameId);
	}
}
