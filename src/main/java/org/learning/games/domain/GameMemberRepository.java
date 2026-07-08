package org.learning.games.domain;

import java.util.List;
import java.util.Optional;

import org.learning.games.entity.GameMember;

public interface GameMemberRepository {

	void persist(GameMember member);

	void delete(GameMember member);

	Optional<GameMember> findByGameAndUser(Long gameId, String userId);

	List<GameMember> findByGame(Long gameId);
}
