package org.learning.games.domain.gameplay;

import java.util.List;

import org.learning.games.domain.GameMemberRepository;
import org.learning.games.domain.GameRepository;
import org.learning.games.domain.exception.ForbiddenException;
import org.learning.games.domain.exception.NotFoundException;
import org.learning.games.entity.Game;
import org.learning.games.entity.GameMember;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GameAccess {

	@Inject
	GameRepository gameRepository;

	@Inject
	GameMemberRepository memberRepository;

	public Game requireGame(Long gameId) {
		return gameRepository.findById(gameId)
				.orElseThrow(() -> new NotFoundException("Game " + gameId + " not found"));
	}

	public void requireAdmin(Game game, String userId) {
		if (!game.adminUserId.equals(userId)) {
			throw new ForbiddenException("Only the game admin can perform this action");
		}
	}

	public void requireMember(Long gameId, String userId) {
		if (memberRepository.findByGameAndUser(gameId, userId).isEmpty()) {
			throw new ForbiddenException("Not a member of this game");
		}
	}

	public Game reloadGame(Long gameId) {
		return gameRepository.findByIdWithMembers(gameId)
				.orElseThrow(() -> new NotFoundException("Game " + gameId + " not found"));
	}

	public Game requireGameWithMembers(Long gameId) {
		return gameRepository.findByIdWithMembers(gameId)
				.orElseThrow(() -> new NotFoundException("Game " + gameId + " not found"));
	}

	public List<GameMember> activeMembers(Long gameId) {
		return memberRepository.findByGame(gameId).stream()
				.filter(member -> !member.eliminated)
				.toList();
	}

	public List<GameMember> orderedActiveMembers(Long gameId) {
		return activeMembers(gameId);
	}
}
