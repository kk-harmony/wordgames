package org.learning.games.resource.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.learning.games.domain.GameService;
import org.learning.games.domain.gameplay.SessionScoringPolicy;
import org.learning.games.domain.model.GameOutcome;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;

@QuarkusTest
public class SessionFlowTest {

	@Inject
	GameService gameService;

	private static final String JOIN_CODE_PATTERN = "[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{5}";

	private static long secretWordId;

	private static long secretWordId() {
		if (secretWordId == 0) {
			secretWordId = given()
					.header("Content-Type", "application/json")
					.body("{\"authentic\": \"session-apple\", \"imposed\": \"session-orange\"}")
					.when()
					.post("/secretwords")
					.then()
					.statusCode(201)
					.extract()
					.jsonPath()
					.getLong("id");
		}
		return secretWordId;
	}

	@Test
	@TestSecurity(user = "admin")
	void createSessionReturnsAdminMemberAndJoinCode() {
		given()
				.header("Content-Type", "application/json")
				.body("{\"name\": \"Session Room\", \"displayName\": \"Host\"}")
				.when()
				.post("/sessions")
				.then()
				.statusCode(201)
				.body("id", matchesPattern(JOIN_CODE_PATTERN))
				.body("name", is("Session Room"))
				.body("adminUserId", is("admin"))
				.body("status", is("OPEN"))
				.body("gamesStartedCount", is(0))
				.body("maxGames", is(20))
				.body("members.size()", is(1))
				.body("members[0].role", is("ADMIN"))
				.body("members[0].displayName", is("Host"));
	}

	@Test
	@TestSecurity(user = "admin")
	void startGameRequiresThreePlayers() {
		String code = given()
				.header("Content-Type", "application/json")
				.body("{\"name\": \"Small Session\"}")
				.when()
				.post("/sessions")
				.then()
				.statusCode(201)
				.extract()
				.jsonPath()
				.getString("id");

		given()
				.header("Content-Type", "application/json")
				.body("{\"secretWordId\": " + secretWordId() + "}")
				.when()
				.post("/sessions/{code}/games", code)
				.then()
				.statusCode(400);
	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class MultiGameSessionFlow {

		private static String sessionCode;
		private static long gameId;
		private static long secondWordId;

		@Test
		@Order(1)
		@TestSecurity(user = "admin")
		void adminCreatesSession() {
			sessionCode = given()
					.header("Content-Type", "application/json")
					.body("{\"name\": \"Multi Game Session\"}")
					.when()
					.post("/sessions")
					.then()
					.statusCode(201)
					.body("id", matchesPattern(JOIN_CODE_PATTERN))
					.extract()
					.jsonPath()
					.getString("id");
		}

		@Test
		@Order(2)
		@TestSecurity(user = "player2")
		void playerTwoJoins() {
			given()
					.when()
					.post("/sessions/{code}/members", sessionCode)
					.then()
					.statusCode(201)
					.body("members.size()", is(2));
		}

		@Test
		@Order(3)
		@TestSecurity(user = "player3")
		void playerThreeJoins() {
			given()
					.when()
					.post("/sessions/{code}/members", sessionCode)
					.then()
					.statusCode(201)
					.body("members.size()", is(3));
		}

		@Test
		@Order(4)
		@TestSecurity(user = "player2")
		void nonAdminCannotStartGame() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"secretWordId\": " + secretWordId() + "}")
					.when()
					.post("/sessions/{code}/games", sessionCode)
					.then()
					.statusCode(403);
		}

		@Test
		@Order(5)
		@TestSecurity(user = "admin")
		void adminStartsFirstGame() {
			gameId = given()
					.header("Content-Type", "application/json")
					.body("{\"secretWordId\": " + secretWordId() + "}")
					.when()
					.post("/sessions/{code}/games", sessionCode)
					.then()
					.statusCode(201)
					.body("status", is("IN_PROGRESS"))
					.body("members.size()", is(3))
					.extract()
					.jsonPath()
					.getLong("id");
		}

		@Test
		@Order(6)
		@TestSecurity(user = "admin")
		void sessionShowsCurrentGame() {
			given()
					.when()
					.get("/sessions/{code}", sessionCode)
					.then()
					.statusCode(200)
					.body("currentGameId", is((int) gameId))
					.body("currentGameStatus", is("IN_PROGRESS"))
					.body("gamesStartedCount", is(1));
		}

		@Test
		@Order(7)
		@TestSecurity(user = "late")
		void rejectJoinWhileGameActive() {
			given()
					.when()
					.post("/sessions/{code}/members", sessionCode)
					.then()
					.statusCode(400);
		}

		@Test
		@Order(8)
		@TestSecurity(user = "admin")
		void completeAllTurnsEntersVoting() {
			TurnCompletionSupport.completeAllTurns(gameService, gameId, "admin");
			given()
					.when()
					.get("/games/{id}", gameId)
					.then()
					.statusCode(200)
					.body("status", is("VOTING"));
		}

		@Test
		@Order(9)
		@TestSecurity(user = "admin")
		void adminVotes() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"player2\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200);
		}

		@Test
		@Order(12)
		@TestSecurity(user = "player2")
		void playerTwoVotes() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"player3\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200);
		}

		@Test
		@Order(13)
		@TestSecurity(user = "player3")
		void playerThreeVoteFinishesGame() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"player2\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200)
					.body("status", is("FINISHED"))
					.body("outcome", notNullValue());
		}

		@Test
		@Order(14)
		@TestSecurity(user = "admin")
		void sessionClearsCurrentGameAfterFinishAndAwardsScores() {
			var game = given()
					.when()
					.get("/games/{id}", gameId)
					.then()
					.statusCode(200)
					.extract()
					.jsonPath();

			String outcome = game.getString("outcome");
			String impostor = game.getString("impostorUserId");

			// Final-round votes in this flow: admin→player2, player2→player3, player3→player2
			Map<String, String> votes = Map.of(
					"admin", "player2",
					"player2", "player3",
					"player3", "player2");
			Map<String, Integer> expected = SessionScoringPolicy.deltas(
					GameOutcome.valueOf(outcome),
					impostor,
					votes);

			var session = given()
					.when()
					.get("/sessions/{code}", sessionCode)
					.then()
					.statusCode(200)
					.body("currentGameId", nullValue())
					.body("gamesStartedCount", is(1))
					.extract()
					.jsonPath();

			List<Map<String, Object>> members = session.getList("members");
			assertThat(members, notNullValue());
			for (Map<String, Object> member : members) {
				String userId = (String) member.get("userId");
				int score = ((Number) member.get("score")).intValue();
				assertThat(userId + " score", score, is(expected.getOrDefault(userId, 0)));
			}
		}

		@Test
		@Order(15)
		@TestSecurity(user = "late")
		void latePlayerJoinsBetweenGames() {
			given()
					.when()
					.post("/sessions/{code}/members", sessionCode)
					.then()
					.statusCode(201)
					.body("members.size()", is(4));
		}

		@Test
		@Order(16)
		@TestSecurity(user = "admin")
		void adminStartsSecondGameWithLateJoiner() {
			secondWordId = given()
					.header("Content-Type", "application/json")
					.body("{\"authentic\": \"session-pear\", \"imposed\": \"session-peach\"}")
					.when()
					.post("/secretwords")
					.then()
					.statusCode(201)
					.extract()
					.jsonPath()
					.getLong("id");

			given()
					.header("Content-Type", "application/json")
					.body("{\"secretWordId\": " + secondWordId + "}")
					.when()
					.post("/sessions/{code}/games", sessionCode)
					.then()
					.statusCode(201)
					.body("status", is("IN_PROGRESS"))
					.body("members.size()", is(4))
					.body("members.find { it.userId == 'late' }.userId", is("late"));
		}

		@Test
		@Order(17)
		@TestSecurity(user = "admin")
		void sessionShowsSecondGameCountAndKeepsScores() {
			var session = given()
					.when()
					.get("/sessions/{code}", sessionCode)
					.then()
					.statusCode(200)
					.body("gamesStartedCount", is(2))
					.body("currentGameStatus", is("IN_PROGRESS"))
					.extract()
					.jsonPath();

			List<Map<String, Object>> members = session.getList("members");
			int totalScore = members.stream()
					.mapToInt(m -> ((Number) m.get("score")).intValue())
					.sum();
			// First game always awards at least impostor-win (3) or identify points (2+).
			assertThat(totalScore, org.hamcrest.Matchers.greaterThan(0));
			int lateScore = members.stream()
					.filter(m -> "late".equals(m.get("userId")))
					.mapToInt(m -> ((Number) m.get("score")).intValue())
					.findFirst()
					.orElse(-1);
			assertThat(lateScore, is(0));
		}
	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class LeaveSessionFlow {

		private static String sessionCode;

		@Test
		@Order(1)
		@TestSecurity(user = "admin")
		void setup() {
			sessionCode = given()
					.header("Content-Type", "application/json")
					.body("{\"name\": \"Leave Session\"}")
					.when()
					.post("/sessions")
					.then()
					.statusCode(201)
					.extract()
					.jsonPath()
					.getString("id");
		}

		@Test
		@Order(2)
		@TestSecurity(user = "player2")
		void playerJoins() {
			given().when().post("/sessions/{code}/members", sessionCode).then().statusCode(201);
		}

		@Test
		@Order(3)
		@TestSecurity(user = "admin")
		void adminCannotLeave() {
			given()
					.when()
					.delete("/sessions/{code}/members/{userId}", sessionCode, "admin")
					.then()
					.statusCode(400);
		}

		@Test
		@Order(4)
		@TestSecurity(user = "player2")
		void playerLeaves() {
			given()
					.when()
					.delete("/sessions/{code}/members/{userId}", sessionCode, "player2")
					.then()
					.statusCode(204);
		}
	}
}
