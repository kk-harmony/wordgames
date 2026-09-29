package org.learning.games.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.learning.games.domain.exception.BadRequestException;
import org.learning.games.domain.exception.ForbiddenException;
import org.learning.games.domain.exception.NotFoundException;
import org.learning.games.domain.gameplay.GameRules;
import org.learning.games.domain.gameplay.GameStartService;
import org.learning.games.domain.gameplay.SessionJoinCode;
import org.learning.games.domain.gameplay.SessionJoinCodeGenerator;
import org.learning.games.domain.gameplay.SessionRules;
import org.learning.games.domain.model.GameStatus;
import org.learning.games.domain.model.MemberRole;
import org.learning.games.domain.model.SessionStatus;
import org.learning.games.entity.Game;
import org.learning.games.entity.GameMember;
import org.learning.games.entity.GameSession;
import org.learning.games.entity.SecretWord;
import org.learning.games.entity.SessionMember;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SessionLifecycleService {

	@Inject
	GameSessionRepository sessionRepository;

	@Inject
	SessionMemberRepository sessionMemberRepository;

	@Inject
	GameRepository gameRepository;

	@Inject
	GameMemberRepository gameMemberRepository;

	@Inject
	GameStartService gameStartService;

	@Inject
	SessionJoinCodeGenerator joinCodeGenerator;

	@Inject
	GameMetrics gameMetrics;

	@Transactional
	public GameSession createSession(String name, String displayName, String userId) {
		GameSession session = new GameSession();
		session.name = name;
		session.adminUserId = userId;
		session.status = SessionStatus.OPEN;
		session.createdAt = Instant.now();
		session.joinCode = joinCodeGenerator.nextCode(sessionRepository::existsByJoinCode);
		sessionRepository.persist(session);

		SessionMember admin = new SessionMember();
		admin.session = session;
		admin.userId = userId;
		admin.role = MemberRole.ADMIN;
		admin.displayName = normalizeDisplayName(displayName);
		sessionMemberRepository.persist(admin);

		return requireSessionWithMembersByJoinCode(session.joinCode);
	}

	@Transactional
	public GameSession getSessionForMember(String rawJoinCode, String userId) {
		GameSession session = requireSessionWithMembersByJoinCode(rawJoinCode);
		requireSessionMember(session.id, userId);
		return session;
	}

	@Transactional
	public GameSession joinSession(String rawJoinCode, String userId, String displayName) {
		GameSession session = requireSessionByJoinCode(rawJoinCode);
		requireOpen(session);
		requireBetweenGames(session);

		if (sessionMemberRepository.findBySessionAndUser(session.id, userId).isPresent()) {
			throw new BadRequestException("User is already a member of this session");
		}

		SessionMember member = new SessionMember();
		member.session = session;
		member.userId = userId;
		member.role = MemberRole.MEMBER;
		member.displayName = normalizeDisplayName(displayName);
		sessionMemberRepository.persist(member);

		return requireSessionWithMembersByJoinCode(session.joinCode);
	}

	@Transactional
	public void leaveSession(String rawJoinCode, String userId) {
		GameSession session = requireSessionByJoinCode(rawJoinCode);

		if (session.adminUserId.equals(userId)) {
			throw new BadRequestException("Session admin cannot leave the session");
		}

		requireBetweenGames(session);

		SessionMember member = sessionMemberRepository.findBySessionAndUser(session.id, userId)
				.orElseThrow(() -> new NotFoundException("User is not a member of this session"));

		sessionMemberRepository.delete(member);
	}

	@Transactional
	public GameSession kickMember(String rawJoinCode, String adminUserId, String targetUserId) {
		GameSession session = requireSessionByJoinCode(rawJoinCode);
		requireSessionAdmin(session, adminUserId);
		requireBetweenGames(session);

		if (session.adminUserId.equals(targetUserId)) {
			throw new BadRequestException("Cannot kick the session admin");
		}

		SessionMember member = sessionMemberRepository.findBySessionAndUser(session.id, targetUserId)
				.orElseThrow(() -> new NotFoundException("User is not a member of this session"));
		sessionMemberRepository.delete(member);

		return requireSessionWithMembersByJoinCode(session.joinCode);
	}

	@Transactional
	public Game startGame(String rawJoinCode, String userId, Long secretWordId) {
		GameSession session = requireSessionByJoinCode(rawJoinCode);
		requireSessionAdmin(session, userId);
		requireOpen(session);
		requireBetweenGames(session);

		if (session.gamesStartedCount >= SessionRules.MAX_GAMES) {
			throw new BadRequestException("Session allows at most " + SessionRules.MAX_GAMES + " games");
		}

		List<SessionMember> sessionMembers = sessionMemberRepository.findBySession(session.id);
		if (sessionMembers.size() < GameRules.MIN_PLAYERS) {
			throw new BadRequestException(
					"At least " + GameRules.MIN_PLAYERS + " players are required to start the game");
		}

		SecretWord secretWord = gameStartService.requireSecretWord(secretWordId);

		Game game = new Game();
		game.name = session.name;
		game.adminUserId = session.adminUserId;
		game.session = session;
		game.createdAt = Instant.now();
		gameRepository.persist(game);

		List<GameMember> gameMembers = new ArrayList<>();
		for (SessionMember sessionMember : sessionMembers) {
			GameMember gameMember = new GameMember();
			gameMember.game = game;
			gameMember.userId = sessionMember.userId;
			gameMember.displayName = sessionMember.displayName;
			gameMember.role = sessionMember.role;
			gameMemberRepository.persist(gameMember);
			gameMembers.add(gameMember);
		}

		gameStartService.beginPlay(game, gameMembers, secretWord);

		session.currentGameId = game.id;
		session.gamesStartedCount++;

		gameMetrics.recordGameStarted();

		return gameRepository.findByIdWithMembers(game.id)
				.orElseThrow(() -> new NotFoundException("Game " + game.id + " not found"));
	}

	public GameStatus currentGameStatus(GameSession session) {
		if (session.currentGameId == null) {
			return null;
		}
		return gameRepository.findById(session.currentGameId)
				.map(game -> game.status)
				.orElse(null);
	}

	private GameSession requireSessionByJoinCode(String rawJoinCode) {
		String joinCode = SessionJoinCode.parse(rawJoinCode).value();
		return sessionRepository.findByJoinCode(joinCode)
				.orElseThrow(() -> new NotFoundException("Session " + joinCode + " not found"));
	}

	private GameSession requireSessionWithMembersByJoinCode(String rawJoinCode) {
		String joinCode = SessionJoinCode.parse(rawJoinCode).value();
		return sessionRepository.findByJoinCodeWithMembers(joinCode)
				.orElseThrow(() -> new NotFoundException("Session " + joinCode + " not found"));
	}

	private void requireSessionMember(Long sessionId, String userId) {
		if (sessionMemberRepository.findBySessionAndUser(sessionId, userId).isEmpty()) {
			throw new ForbiddenException("Not a member of this session");
		}
	}

	private void requireSessionAdmin(GameSession session, String userId) {
		if (!session.adminUserId.equals(userId)) {
			throw new ForbiddenException("Only the session admin can perform this action");
		}
	}

	private void requireOpen(GameSession session) {
		if (session.status != SessionStatus.OPEN) {
			throw new BadRequestException("Session is closed");
		}
	}

	private void requireBetweenGames(GameSession session) {
		if (session.currentGameId == null) {
			return;
		}
		Game current = gameRepository.findById(session.currentGameId)
				.orElse(null);
		if (current == null) {
			session.currentGameId = null;
			return;
		}
		if (current.status == GameStatus.IN_PROGRESS || current.status == GameStatus.VOTING) {
			throw new BadRequestException("Cannot modify session while a game is in progress");
		}
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
