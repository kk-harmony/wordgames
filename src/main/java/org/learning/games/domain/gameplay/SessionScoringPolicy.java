package org.learning.games.domain.gameplay;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.learning.games.domain.model.GameOutcome;

/**
 * Awards points for a finished session-linked game (Interpretation A).
 * Cumulative totals live on {@code SessionMember}; this policy only computes deltas.
 */
public final class SessionScoringPolicy {

	public static final int IMPOSTOR_WIN_POINTS = 3;
	public static final int IDENTIFY_CAUGHT_POINTS = 2;
	public static final int IDENTIFY_MISSED_POINTS = 1;

	private SessionScoringPolicy() {
	}

	/**
	 * @param outcome        game outcome
	 * @param impostorUserId impostor for the finished game
	 * @param votes          final-round map of voterUserId → votedForUserId (empty for kick finishes)
	 * @return userId → points to add (only positive deltas; impostor never earns identify points)
	 */
	public static Map<String, Integer> deltas(GameOutcome outcome, String impostorUserId, Map<String, String> votes) {
		Objects.requireNonNull(outcome, "outcome");
		Map<String, Integer> result = new HashMap<>();
		if (impostorUserId == null || impostorUserId.isBlank()) {
			return result;
		}

		Map<String, String> safeVotes = votes == null ? Map.of() : votes;

		if (outcome == GameOutcome.IMPOSTOR_SURVIVED) {
			result.put(impostorUserId, IMPOSTOR_WIN_POINTS);
			for (Map.Entry<String, String> vote : safeVotes.entrySet()) {
				String voter = vote.getKey();
				if (impostorUserId.equals(voter)) {
					continue;
				}
				if (impostorUserId.equals(vote.getValue())) {
					result.merge(voter, IDENTIFY_MISSED_POINTS, Integer::sum);
				}
			}
			return result;
		}

		if (outcome == GameOutcome.IMPOSTOR_IDENTIFIED) {
			for (Map.Entry<String, String> vote : safeVotes.entrySet()) {
				String voter = vote.getKey();
				if (impostorUserId.equals(voter)) {
					continue;
				}
				if (impostorUserId.equals(vote.getValue())) {
					result.merge(voter, IDENTIFY_CAUGHT_POINTS, Integer::sum);
				}
			}
		}

		return result;
	}
}
