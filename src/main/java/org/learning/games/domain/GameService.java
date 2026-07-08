package org.learning.games.domain;

import org.learning.games.domain.gameplay.GameKickService;
import org.learning.games.domain.gameplay.GameReadService;
import org.learning.games.domain.gameplay.GameStartService;
import org.learning.games.domain.gameplay.GameTurnService;
import org.learning.games.domain.gameplay.GameVoteService;
import org.learning.games.domain.model.AssignedWord;
import org.learning.games.entity.Game;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class GameService {

	@Inject
	GameStartService startService;

	@Inject
	GameTurnService turnService;

	@Inject
	GameVoteService voteService;

	@Inject
	GameKickService kickService;

	@Inject
	GameReadService readService;

	@Transactional
	public Game startGame(Long gameId, String userId, Long secretWordId) {
		return startService.startGame(gameId, userId, secretWordId);
	}

	@Transactional
	public Game kickMember(Long gameId, String adminUserId, String targetUserId) {
		return kickService.kickMember(gameId, adminUserId, targetUserId);
	}

	@Transactional
	public Game completeTurn(Long gameId, String userId) {
		return turnService.completeTurn(gameId, userId);
	}

	@Transactional
	public Game getGameForMember(Long gameId, String userId) {
		return readService.getGameForMember(gameId, userId);
	}

	@Transactional
	public AssignedWord getMyWord(Long gameId, String userId) {
		return readService.getMyWord(gameId, userId);
	}

	@Transactional
	public Game castVote(Long gameId, String userId, String votedUserId) {
		return voteService.castVote(gameId, userId, votedUserId);
	}
}
