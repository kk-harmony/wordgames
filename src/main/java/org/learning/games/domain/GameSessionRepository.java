package org.learning.games.domain;

import java.util.Optional;

import org.learning.games.entity.GameSession;

public interface GameSessionRepository {

	void persist(GameSession session);

	Optional<GameSession> findById(Long id);

	Optional<GameSession> findByJoinCode(String joinCode);

	Optional<GameSession> findByIdWithMembers(Long id);

	Optional<GameSession> findByJoinCodeWithMembers(String joinCode);

	Optional<GameSession> findByCurrentGameId(Long gameId);

	boolean existsByJoinCode(String joinCode);
}
