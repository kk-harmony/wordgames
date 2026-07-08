package org.learning.games.domain;

import java.time.Instant;

import org.learning.games.domain.exception.BadRequestException;
import org.learning.games.domain.exception.NotFoundException;
import org.learning.games.domain.model.GameStatus;
import org.learning.games.domain.model.MemberRole;
import org.learning.games.entity.Game;
import org.learning.games.entity.GameMember;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class GameLifecycleService {

	@Inject
	GameRepository gameRepository;

	@Inject
	GameMemberRepository memberRepository;

	@Transactional
	public Game createGame(String name, String displayName, String userId) {
		Game game = new Game();
		game.name = name;
		game.adminUserId = userId;
		game.createdAt = Instant.now();
		gameRepository.persist(game);

		GameMember admin = new GameMember();
		admin.game = game;
		admin.userId = userId;
		admin.role = MemberRole.ADMIN;
		admin.displayName = normalizeDisplayName(displayName);
		memberRepository.persist(admin);

		return gameRepository.findByIdWithMembers(game.id)
				.orElseThrow(() -> new NotFoundException("Game " + game.id + " not found"));
	}

	@Transactional
	public Game joinGame(Long gameId, String userId, String displayName) {
		Game game = gameRepository.findById(gameId)
				.orElseThrow(() -> new NotFoundException("Game " + gameId + " not found"));

		if (memberRepository.findByGameAndUser(game.id, userId).isPresent()) {
			throw new BadRequestException("User is already a member of this game");
		}

		GameMember member = new GameMember();
		member.game = game;
		member.userId = userId;
		member.role = MemberRole.MEMBER;
		member.displayName = normalizeDisplayName(displayName);
		memberRepository.persist(member);

		return gameRepository.findByIdWithMembers(game.id)
				.orElseThrow(() -> new NotFoundException("Game " + gameId + " not found"));
	}

	@Transactional
	public void leaveGame(Long gameId, String userId) {
		Game game = gameRepository.findById(gameId)
				.orElseThrow(() -> new NotFoundException("Game " + gameId + " not found"));

		if (game.adminUserId.equals(userId)) {
			throw new BadRequestException("Game admin cannot leave the game");
		}

		if (game.status != GameStatus.WAITING) {
			throw new BadRequestException("Cannot leave after the game has started");
		}

		GameMember member = memberRepository.findByGameAndUser(game.id, userId)
				.orElseThrow(() -> new NotFoundException("User is not a member of this game"));

		memberRepository.delete(member);
	}

	private static String normalizeDisplayName(String displayName) {
		if (displayName == null) {
			return null;
		}
		String trimmed = displayName.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		if (trimmed.length() > 30) {
			throw new BadRequestException("Display name must be at most 30 characters");
		}
		return trimmed;
	}
}
