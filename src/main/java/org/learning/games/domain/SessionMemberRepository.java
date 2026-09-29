package org.learning.games.domain;

import java.util.List;
import java.util.Optional;

import org.learning.games.entity.SessionMember;

public interface SessionMemberRepository {

	void persist(SessionMember member);

	void delete(SessionMember member);

	Optional<SessionMember> findBySessionAndUser(Long sessionId, String userId);

	List<SessionMember> findBySession(Long sessionId);
}
