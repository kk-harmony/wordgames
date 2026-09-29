package org.learning.games.domain.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.learning.games.domain.model.GameOutcome;
import org.junit.jupiter.api.Test;

class SessionScoringPolicyTest {

	@Test
	void impostorWinAwardsThreeAndCorrectVotersOne() {
		Map<String, Integer> deltas = SessionScoringPolicy.deltas(
				GameOutcome.IMPOSTOR_SURVIVED,
				"imp",
				Map.of(
						"a", "imp",
						"b", "a",
						"imp", "a"));

		assertEquals(3, deltas.get("imp"));
		assertEquals(1, deltas.get("a"));
		assertEquals(null, deltas.get("b"));
	}

	@Test
	void impostorIdentifiedAwardsTwoToCorrectVoters() {
		Map<String, Integer> deltas = SessionScoringPolicy.deltas(
				GameOutcome.IMPOSTOR_IDENTIFIED,
				"imp",
				Map.of(
						"a", "imp",
						"b", "imp",
						"c", "a",
						"imp", "a"));

		assertEquals(2, deltas.get("a"));
		assertEquals(2, deltas.get("b"));
		assertEquals(null, deltas.get("c"));
		assertEquals(null, deltas.get("imp"));
	}

	@Test
	void kickIdentifiedWithNoVotesAwardsNothing() {
		Map<String, Integer> deltas = SessionScoringPolicy.deltas(
				GameOutcome.IMPOSTOR_IDENTIFIED,
				"imp",
				Map.of());

		assertTrue(deltas.isEmpty());
	}

	@Test
	void kickSurvivedWithNoVotesAwardsImpostorOnly() {
		Map<String, Integer> deltas = SessionScoringPolicy.deltas(
				GameOutcome.IMPOSTOR_SURVIVED,
				"imp",
				Map.of());

		assertEquals(Map.of("imp", 3), deltas);
	}

	@Test
	void blankImpostorAwardsNothing() {
		assertTrue(SessionScoringPolicy.deltas(GameOutcome.IMPOSTOR_SURVIVED, null, Map.of("a", "b")).isEmpty());
		assertTrue(SessionScoringPolicy.deltas(GameOutcome.IMPOSTOR_IDENTIFIED, "  ", Map.of("a", "b")).isEmpty());
	}
}
