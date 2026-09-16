package org.learning.games.domain.gameplay;

import java.util.function.Predicate;

/**
 * Creates unique public session join codes.
 */
public interface SessionJoinCodeGenerator {

	/**
	 * @param alreadyExists returns true when the candidate is already assigned
	 */
	String nextCode(Predicate<String> alreadyExists);
}
