package org.learning.games.domain.gameplay;

import java.util.List;

import org.learning.games.domain.GameMemberRepository;
import org.learning.games.domain.GameMetrics;
import org.learning.games.domain.exception.BadRequestException;
import org.learning.games.domain.exception.NotFoundException;
import org.learning.games.domain.model.GameStatus;
import org.learning.games.entity.Game;
import org.learning.games.entity.GameMember;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GameVoteService {

	@Inject
	GameAccess gameAccess;

	@Inject
	GameMemberRepository memberRepository;

	@Inject
	GameMetrics gameMetrics;

	@Inject
	GameResolutionService resolutionService;

	public Game castVote(Long gameId, String userId, String votedUserId) {
		Game game = gameAccess.requireGame(gameId);
		gameAccess.requireMember(gameId, userId);

		if (game.status != GameStatus.VOTING) {
			throw new BadRequestException("Game is not in voting phase");
		}

		GameMember voter = memberRepository.findByGameAndUser(gameId, userId)
				.orElseThrow(() -> new NotFoundException("User is not a member of this game"));

		if (voter.eliminated) {
			throw new BadRequestException("Eliminated players cannot vote");
		}

		if (voter.votedForUserId != null) {
			throw new BadRequestException("Vote already cast");
		}

		GameMember target = memberRepository.findByGameAndUser(gameId, votedUserId)
				.orElseThrow(() -> new NotFoundException("Voted user is not a member of this game"));

		if (target.eliminated) {
			throw new BadRequestException("Cannot vote for an eliminated player");
		}

		if (votedUserId.equals(userId)) {
			throw new BadRequestException("Cannot vote for yourself");
		}

		voter.votedForUserId = votedUserId;
		gameMetrics.recordVoteCast();

		List<GameMember> activeMembers = gameAccess.activeMembers(gameId);
		if (activeMembers.stream().allMatch(m -> m.votedForUserId != null)) {
			resolutionService.resolveVotes(game, activeMembers);
		}

		return gameAccess.reloadGame(gameId);
	}
}
