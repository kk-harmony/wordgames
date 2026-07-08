package org.learning.games.domain;

import java.util.List;
import java.util.Optional;

import org.learning.games.entity.Game;

public interface GameRepository {

	void persist(Game game);

	Optional<Game> findById(Long id);

	Optional<Game> findByIdWithMembers(Long id);

	List<Long> findSecretWordIdsUsedByAdmin(String adminUserId);

	boolean hasWaitingGameAsAdmin(String adminUserId);

	boolean isSecretWordUsedByAdmin(Long secretWordId, String adminUserId);
}
