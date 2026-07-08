package org.learning.games.domain.gameplay;

import java.util.List;
import java.util.logging.Logger;

import org.learning.games.domain.GameMemberRepository;
import org.learning.games.domain.GameMetrics;
import org.learning.games.domain.exception.BadRequestException;
import org.learning.games.domain.exception.NotFoundException;
import org.learning.games.domain.model.GameOutcome;
import org.learning.games.domain.model.GameStatus;
import org.learning.games.entity.Game;
import org.learning.games.entity.GameMember;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GameKickService {

	private static final Logger LOG = Logger.getLogger(GameKickService.class.getName());

	@Inject
	GameAccess gameAccess;

	@Inject
	GameMemberRepository memberRepository;

	@Inject
	GameMetrics gameMetrics;

	@Inject
	GameResolutionService resolutionService;

	public Game kickMember(Long gameId, String adminUserId, String targetUserId) {
		Game game = gameAccess.requireGame(gameId);
		gameAccess.requireAdmin(game, adminUserId);

		if (game.status == GameStatus.WAITING) {
			throw new BadRequestException("Cannot kick before the game has started");
		}

		if (game.status == GameStatus.FINISHED) {
			throw new BadRequestException("Cannot kick after the game has finished");
		}

		List<GameMember> members = memberRepository.findByGame(gameId);
		if (members.size() < GameRules.MIN_PLAYERS) {
			throw new BadRequestException(
					"Removal is only allowed when at least " + GameRules.MIN_PLAYERS + " players are in the game");
		}

		if (targetUserId.equals(game.adminUserId)) {
			throw new BadRequestException("Cannot kick the game admin");
		}

		GameMember target = memberRepository.findByGameAndUser(gameId, targetUserId)
				.orElseThrow(() -> new NotFoundException("User is not a member of this game"));

		if (targetUserId.equals(game.impostorUserId)) {
			memberRepository.delete(target);
			resolutionService.finishGame(game, GameOutcome.IMPOSTOR_IDENTIFIED);
			gameMetrics.recordMemberKicked();
			LOG.info(() -> "member.kicked gameId=" + gameId + " targetUserId=" + targetUserId + " impostor=true");
			return gameAccess.reloadGame(gameId);
		}

		boolean wasCurrentTurn = targetUserId.equals(game.currentTurnUserId);
		boolean wasEliminated = target.eliminated;

		for (GameMember member : members) {
			if (targetUserId.equals(member.votedForUserId)) {
				member.votedForUserId = null;
			}
		}

		memberRepository.delete(target);
		gameMetrics.recordMemberKicked();
		LOG.info(() -> "member.kicked gameId=" + gameId + " targetUserId=" + targetUserId);

		List<GameMember> activeRemaining = gameAccess.activeMembers(gameId);
		if (activeRemaining.size() < GameRules.MIN_ACTIVE_PLAYERS_FOR_NEXT_ROUND) {
			resolutionService.finishGame(game, GameOutcome.IMPOSTOR_SURVIVED);
			return gameAccess.reloadGame(gameId);
		}

		if (!wasEliminated) {
			if (game.status == GameStatus.IN_PROGRESS && wasCurrentTurn) {
				resolutionService.transitionToVotingOrNextTurn(game);
			} else if (game.status == GameStatus.VOTING) {
				if (activeRemaining.stream().allMatch(m -> m.votedForUserId != null)) {
					resolutionService.resolveVotes(game, activeRemaining);
				}
			}
		}

		return gameAccess.reloadGame(gameId);
	}
}
