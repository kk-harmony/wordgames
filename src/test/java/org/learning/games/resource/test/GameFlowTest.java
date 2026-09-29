package org.learning.games.resource.test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.learning.games.domain.GameService;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class GameFlowTest {

	@Inject
	GameService gameService;

	private static long secretWordId;

	private static long secretWordId() {
		if (secretWordId == 0) {
			secretWordId = given()
					.header("Content-Type", "application/json")
					.body("{\"authentic\": \"apple\", \"imposed\": \"orange\"}")
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
	void startGameRequiresThreePlayers() {
		long gameId = given()
				.header("Content-Type", "application/json")
				.body("{\"name\": \"Small Game\"}")
				.when()
				.post("/games")
				.then()
				.statusCode(201)
				.extract()
				.jsonPath()
				.getLong("id");

		given()
				.header("Content-Type", "application/json")
				.body("{\"secretWordId\": " + secretWordId() + "}")
				.when()
				.post("/games/{id}/start", gameId)
				.then()
				.statusCode(400);
	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class StartGameFlow {

		private static long gameId;

		@Test
		@Order(1)
		@TestSecurity(user = "admin")
		void adminCreatesGame() {
			gameId = given()
					.header("Content-Type", "application/json")
					.body("{\"name\": \"Start Game\"}")
					.when()
					.post("/games")
					.then()
					.statusCode(201)
					.extract()
					.jsonPath()
					.getLong("id");
		}

		@Test
		@Order(2)
		@TestSecurity(user = "player2")
		void playerTwoJoins() {
			given()
					.header("Content-Type", "application/json")
					.when()
					.post("/games/{id}/members", gameId)
					.then()
					.statusCode(201);
		}

		@Test
		@Order(3)
		@TestSecurity(user = "player3")
		void playerThreeJoins() {
			given()
					.header("Content-Type", "application/json")
					.when()
					.post("/games/{id}/members", gameId)
					.then()
					.statusCode(201);
		}

		@Test
		@Order(4)
		@TestSecurity(user = "player2")
		void nonAdminCannotStartGame() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"secretWordId\": " + secretWordId() + "}")
					.when()
					.post("/games/{id}/start", gameId)
					.then()
					.statusCode(403);
		}

		@Test
		@Order(5)
		@TestSecurity(user = "admin")
		void adminStartsGame() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"secretWordId\": " + secretWordId() + "}")
					.when()
					.post("/games/{id}/start", gameId)
					.then()
					.statusCode(200)
					.body("status", is("IN_PROGRESS"))
					.body("currentRound", is(1))
					.body("members.size()", is(3));
		}

		@Test
		@Order(6)
		@TestSecurity(user = "player2")
		void memberCannotGetSecretWordWhileInProgress() {
			given()
					.when()
					.get("/secretwords/{id}", secretWordId())
					.then()
					.statusCode(403);
		}

		@Test
		@Order(7)
		@TestSecurity(user = "admin")
		void adminReceivesAssignedWord() {
			given()
					.when()
					.get("/games/{id}/my-word", gameId)
					.then()
					.statusCode(200)
					.body("word", is(notNullValue()))
					.body("type", is(notNullValue()));
		}
	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class TurnAndVoteFlow {

		private static long gameId;

		@Test
		@Order(1)
		@TestSecurity(user = "admin")
		void createAndStartGame() {
			gameId = given()
					.header("Content-Type", "application/json")
					.body("{\"name\": \"Turn Flow\"}")
					.when()
					.post("/games")
					.then()
					.statusCode(201)
					.extract()
					.jsonPath()
					.getLong("id");

			given()
					.header("Content-Type", "application/json")
					.body("{\"secretWordId\": " + secretWordId() + "}")
					.when()
					.post("/games/{id}/start", gameId)
					.then()
					.statusCode(400);
		}

		@Test
		@Order(2)
		@TestSecurity(user = "player2")
		void playerTwoJoins() {
			given()
					.header("Content-Type", "application/json")
					.when()
					.post("/games/{id}/members", gameId)
					.then()
					.statusCode(201);
		}

		@Test
		@Order(3)
		@TestSecurity(user = "player3")
		void playerThreeJoinsAndStarts() {
			given()
					.header("Content-Type", "application/json")
					.when()
					.post("/games/{id}/members", gameId)
					.then()
					.statusCode(201);

			given()
					.header("Content-Type", "application/json")
					.body("{\"secretWordId\": " + secretWordId() + "}")
					.when()
					.post("/games/{id}/start", gameId)
					.then()
					.statusCode(403);
		}

		@Test
		@Order(4)
		@TestSecurity(user = "admin")
		void adminStartsAfterJoins() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"secretWordId\": " + secretWordId() + "}")
					.when()
					.post("/games/{id}/start", gameId)
					.then()
					.statusCode(200);
		}

		@Test
		@Order(5)
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
		@Order(6)
		@TestSecurity(user = "admin")
		void adminVotesForPlayerTwo() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"player2\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200)
					.body("status", is("VOTING"));
		}

		@Test
		@Order(9)
		@TestSecurity(user = "player2")
		void playerTwoVotesForPlayerThree() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"player3\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200)
					.body("status", is("VOTING"));
		}

		@Test
		@Order(10)
		@TestSecurity(user = "player3")
		void unanimousVoteEliminatesPlayerTwo() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"player2\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200)
					.body("status", is("FINISHED"))
					.body("members.find { it.userId == 'player2' }.eliminated", is(true))
					.body("outcome", notNullValue());
		}

		@Test
		@Order(11)
		@TestSecurity(user = "player2")
		void finishedGameMemberCanGetSecretWord() {
			given()
					.when()
					.get("/secretwords/{id}", secretWordId())
					.then()
					.statusCode(200)
					.body("id", is((int) secretWordId()))
					.body("authentic", is("apple"))
					.body("imposed", is("orange"));
		}
	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class TieVoteResetFlow {

		private static long gameId;

		@Test
		@Order(1)
		@TestSecurity(user = "admin")
		void setupGame() {
			gameId = given()
					.header("Content-Type", "application/json")
					.body("{\"name\": \"Tie Game\"}")
					.when()
					.post("/games")
					.then()
					.statusCode(201)
					.extract()
					.jsonPath()
					.getLong("id");
		}

		@Test
		@Order(2)
		@TestSecurity(user = "player2")
		void playerTwoJoins() {
			given().when().post("/games/{id}/members", gameId).then().statusCode(201);
		}

		@Test
		@Order(3)
		@TestSecurity(user = "player3")
		void playerThreeJoins() {
			given().when().post("/games/{id}/members", gameId).then().statusCode(201);
		}

		@Test
		@Order(4)
		@TestSecurity(user = "admin")
		void startGame() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"secretWordId\": " + secretWordId() + "}")
					.when()
					.post("/games/{id}/start", gameId)
					.then()
					.statusCode(200);
		}

		@Test
		@Order(5)
		@TestSecurity(user = "admin")
		void completeAllTurns() {
			TurnCompletionSupport.completeAllTurns(gameService, gameId, "admin");
		}

		@Test
		@Order(6)
		@TestSecurity(user = "admin")
		void firstCircularVoteResets() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"player2\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200);
		}

		@Test
		@Order(9)
		@TestSecurity(user = "player2")
		void secondCircularVoteResets() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"player3\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200);
		}

		@Test
		@Order(10)
		@TestSecurity(user = "player3")
		void thirdCircularVoteIncrementsResetCount() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"admin\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200)
					.body("status", is("VOTING"))
					.body("voteResetCount", is(1));
		}

		@Test
		@Order(11)
		@TestSecurity(user = "admin")
		void secondTieRound() {
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
		void secondTieRoundVoteTwo() {
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
		void secondTieRoundVoteThree() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"admin\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200)
					.body("voteResetCount", is(2));
		}
	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class ImpostorIdentifiedFlow {

		private static long gameId;
		private static String impostor;

		@Test
		@Order(1)
		@TestSecurity(user = "admin")
		void setup() {
			gameId = given()
					.header("Content-Type", "application/json")
					.body("{\"name\": \"Identify Impostor\"}")
					.when()
					.post("/games")
					.then()
					.statusCode(201)
					.extract()
					.jsonPath()
					.getLong("id");

			given()
					.header("Content-Type", "application/json")
					.body("{\"secretWordId\": " + secretWordId() + "}")
					.when()
					.post("/games/{id}/start", gameId)
					.then()
					.statusCode(400);
		}

		@Test
		@Order(2)
		@TestSecurity(user = "player2")
		void join() {
			given().when().post("/games/{id}/members", gameId).then().statusCode(201);
		}

		@Test
		@Order(3)
		@TestSecurity(user = "player3")
		void joinAndStartForbidden() {
			given().when().post("/games/{id}/members", gameId).then().statusCode(201);
		}

		@Test
		@Order(4)
		@TestSecurity(user = "admin")
		void start() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"secretWordId\": " + secretWordId() + "}")
					.when()
					.post("/games/{id}/start", gameId)
					.then()
					.statusCode(200);
		}

		@Test
		@Order(5)
		@TestSecurity(user = "admin")
		void detectImpostorFromWord() {
			String adminType = given().when().get("/games/{id}/my-word", gameId).then().statusCode(200)
					.extract().jsonPath().getString("type");
			impostor = "IMPOSED".equals(adminType) ? "admin" : null;
		}

		@Test
		@Order(6)
		@TestSecurity(user = "player2")
		void detectImpostorPlayerTwo() {
			if (impostor == null) {
				String type = given().when().get("/games/{id}/my-word", gameId).then().statusCode(200)
						.extract().jsonPath().getString("type");
				if ("IMPOSED".equals(type)) {
					impostor = "player2";
				}
			}
		}

		@Test
		@Order(7)
		@TestSecurity(user = "player3")
		void detectImpostorPlayerThree() {
			if (impostor == null) {
				String type = given().when().get("/games/{id}/my-word", gameId).then().statusCode(200)
						.extract().jsonPath().getString("type");
				if ("IMPOSED".equals(type)) {
					impostor = "player3";
				}
			}
		}

		@Test
		@Order(8)
		@TestSecurity(user = "admin")
		void completeAllTurns() {
			TurnCompletionSupport.completeAllTurns(gameService, gameId, "admin");
		}

		@Test
		@Order(9)
		@TestSecurity(user = "admin")
		void voteOutImpostorAdmin() {
			String target = "admin".equals(impostor) ? "player2" : impostor;
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"" + target + "\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200);
		}

		@Test
		@Order(12)
		@TestSecurity(user = "player2")
		void voteOutImpostorPlayerTwo() {
			String target = "player2".equals(impostor) ? "player3" : impostor;
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"" + target + "\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200);
		}

		@Test
		@Order(13)
		@TestSecurity(user = "player3")
		void voteOutImpostorFinishesGame() {
			String target = "player3".equals(impostor) ? "player2" : impostor;
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"" + target + "\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200)
					.body("status", is("FINISHED"))
					.body("outcome", is("IMPOSTOR_IDENTIFIED"));
		}
	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class ImpostorSurvivedFlow {

		private static long gameId;
		private static String impostor;
		private static String innocentTarget;

		@Test
		@Order(1)
		@TestSecurity(user = "admin")
		void setup() {
			gameId = given()
					.header("Content-Type", "application/json")
					.body("{\"name\": \"Survival Game\"}")
					.when()
					.post("/games")
					.then()
					.statusCode(201)
					.extract()
					.jsonPath()
					.getLong("id");
		}

		@Test
		@Order(2)
		@TestSecurity(user = "player2")
		void joinTwo() {
			given().when().post("/games/{id}/members", gameId).then().statusCode(201);
		}

		@Test
		@Order(3)
		@TestSecurity(user = "player3")
		void joinThree() {
			given().when().post("/games/{id}/members", gameId).then().statusCode(201);
		}

		@Test
		@Order(4)
		@TestSecurity(user = "admin")
		void startAndDetectImpostor() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"secretWordId\": " + secretWordId() + "}")
					.when()
					.post("/games/{id}/start", gameId)
					.then()
					.statusCode(200);

			String adminType = given().when().get("/games/{id}/my-word", gameId).then().statusCode(200)
					.extract().jsonPath().getString("type");
			if ("IMPOSED".equals(adminType)) {
				impostor = "admin";
			}
		}

		@Test
		@Order(5)
		@TestSecurity(user = "player2")
		void detectImpostorPlayerTwo() {
			if (impostor == null) {
				String type = given().when().get("/games/{id}/my-word", gameId).then().statusCode(200)
						.extract().jsonPath().getString("type");
				if ("IMPOSED".equals(type)) {
					impostor = "player2";
				}
			}
		}

		@Test
		@Order(6)
		@TestSecurity(user = "player3")
		void detectImpostorAndTarget() {
			if (impostor == null) {
				impostor = "player3";
			}
			innocentTarget = pickInnocent(impostor);
		}

		@Test
		@Order(7)
		@TestSecurity(user = "admin")
		void completeRoundOneTurns() {
			TurnCompletionSupport.completeAllTurns(gameService, gameId, "admin");
		}

		@Test
		@Order(8)
		@TestSecurity(user = "admin")
		void roundOneVoteAdmin() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"" + voteTarget("admin") + "\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200);
		}

		@Test
		@Order(11)
		@TestSecurity(user = "player2")
		void roundOneVoteTwo() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"" + voteTarget("player2") + "\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200);
		}

		@Test
		@Order(12)
		@TestSecurity(user = "player3")
		void roundOneVoteThreeFinishesGame() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"" + voteTarget("player3") + "\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200)
					.body("status", is("FINISHED"))
					.body("outcome", is("IMPOSTOR_SURVIVED"));
		}

		private String pickInnocent(String impostorUser) {
			for (String user : new String[] { "admin", "player2", "player3" }) {
				if (!user.equals(impostorUser)) {
					return user;
				}
			}
			throw new IllegalStateException("No innocent player found");
		}

		private String otherInnocent() {
			for (String user : new String[] { "admin", "player2", "player3" }) {
				if (!user.equals(impostor) && !user.equals(innocentTarget)) {
					return user;
				}
			}
			throw new IllegalStateException("No other innocent player found");
		}

		private String voteTarget(String voter) {
			if (voter.equals(innocentTarget)) {
				return otherInnocent();
			}
			return innocentTarget;
		}
	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class TurnOrderFlow {

		private static long gameId;
		private static String firstTurnUserId;

		@Test
		@Order(1)
		@TestSecurity(user = "admin")
		void createGame() {
			gameId = given()
					.header("Content-Type", "application/json")
					.body("{\"name\": \"Turn Order Game\"}")
					.when()
					.post("/games")
					.then()
					.statusCode(201)
					.extract()
					.jsonPath()
					.getLong("id");
		}

		@Test
		@Order(2)
		@TestSecurity(user = "player2")
		void playerTwoJoins() {
			given().when().post("/games/{id}/members", gameId).then().statusCode(201);
		}

		@Test
		@Order(3)
		@TestSecurity(user = "player3")
		void playerThreeJoins() {
			given().when().post("/games/{id}/members", gameId).then().statusCode(201);
		}

		@Test
		@Order(4)
		@TestSecurity(user = "admin")
		void startSetsRandomFirstTurn() {
			firstTurnUserId = given()
					.header("Content-Type", "application/json")
					.body("{\"secretWordId\": " + secretWordId() + "}")
					.when()
					.post("/games/{id}/start", gameId)
					.then()
					.statusCode(200)
					.body("currentTurnUserId", notNullValue())
					.extract()
					.jsonPath()
					.getString("currentTurnUserId");
			org.junit.jupiter.api.Assertions.assertTrue(
					java.util.Set.of("admin", "player2", "player3").contains(firstTurnUserId));
		}

		@Test
		@Order(5)
		@TestSecurity(user = "player2")
		void playerTwoCannotGoOutOfOrderUnlessTheirTurn() {
			int expected = "player2".equals(firstTurnUserId) ? 200 : 400;
			var response = given()
					.when()
					.post("/games/{id}/turn/complete", gameId)
					.then()
					.statusCode(expected);
			if (expected == 200) {
				firstTurnUserId = response.extract().jsonPath().getString("currentTurnUserId");
			}
		}

		@Test
		@Order(6)
		@TestSecurity(user = "admin")
		void adminActsOnlyOnOwnTurnAndPassesWhenAllowed() {
			if (!"admin".equals(firstTurnUserId)) {
				given()
						.when()
						.post("/games/{id}/turn/complete", gameId)
						.then()
						.statusCode(400);
				return;
			}
			given()
					.when()
					.post("/games/{id}/turn/complete", gameId)
					.then()
					.statusCode(200)
					.body("currentTurnUserId", notNullValue())
					.body("currentTurnUserId", not(is("admin")));
		}
	}
}
