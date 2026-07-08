package org.learning.games.resource.test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class IdempotencyTest {

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

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class TurnIdempotencyFlow {

		private static long gameId;
		private static String firstResponse;

		@Test
		@Order(1)
		@TestSecurity(user = "admin")
		void setupGame() {
			gameId = given()
					.header("Content-Type", "application/json")
					.body("{\"name\": \"Turn Idempotency\"}")
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
		void firstTurnCompleteStoresResponse() {
			firstResponse = given()
					.header("Idempotency-Key", "turn-key-1")
					.when()
					.post("/games/{id}/turn/complete", gameId)
					.then()
					.statusCode(200)
					.extract()
					.asString();
		}

		@Test
		@Order(6)
		@TestSecurity(user = "admin")
		void replayTurnCompleteReturnsSameResponse() {
			given()
					.header("Idempotency-Key", "turn-key-1")
					.when()
					.post("/games/{id}/turn/complete", gameId)
					.then()
					.statusCode(200)
					.body(is(firstResponse));
		}
	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class VoteIdempotencyFlow {

		private static long gameId;

		@Test
		@Order(1)
		@TestSecurity(user = "admin")
		void setupAndEnterVoting() {
			gameId = given()
					.header("Content-Type", "application/json")
					.body("{\"name\": \"Vote Idempotency\"}")
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
		void startAndCompleteTurns() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"secretWordId\": " + secretWordId() + "}")
					.when()
					.post("/games/{id}/start", gameId)
					.then()
					.statusCode(200);

			given().when().post("/games/{id}/turn/complete", gameId).then().statusCode(200);
		}

		@Test
		@Order(5)
		@TestSecurity(user = "player2")
		void playerTwoTurn() {
			given().when().post("/games/{id}/turn/complete", gameId).then().statusCode(200);
		}

		@Test
		@Order(6)
		@TestSecurity(user = "player3")
		void playerThreeTurn() {
			given().when().post("/games/{id}/turn/complete", gameId).then().statusCode(200);
		}

		@Test
		@Order(7)
		@TestSecurity(user = "admin")
		void firstVoteWithKey() {
			given()
					.header("Content-Type", "application/json")
					.header("Idempotency-Key", "vote-key-1")
					.body("{\"votedUserId\": \"player2\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200);
		}

		@Test
		@Order(8)
		@TestSecurity(user = "admin")
		void replayVoteWithSameKey() {
			given()
					.header("Content-Type", "application/json")
					.header("Idempotency-Key", "vote-key-1")
					.body("{\"votedUserId\": \"player2\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200)
					.body("status", is("VOTING"));
		}
	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class VoteKeyConflictFlow {

		private static long gameId;

		@Test
		@Order(1)
		@TestSecurity(user = "admin")
		void setupAndEnterVoting() {
			gameId = given()
					.header("Content-Type", "application/json")
					.body("{\"name\": \"Vote Conflict\"}")
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
		void startAndCompleteTurns() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"secretWordId\": " + secretWordId() + "}")
					.when()
					.post("/games/{id}/start", gameId)
					.then()
					.statusCode(200);

			given().when().post("/games/{id}/turn/complete", gameId).then().statusCode(200);
		}

		@Test
		@Order(5)
		@TestSecurity(user = "player2")
		void playerTwoTurn() {
			given().when().post("/games/{id}/turn/complete", gameId).then().statusCode(200);
		}

		@Test
		@Order(6)
		@TestSecurity(user = "player3")
		void playerThreeTurn() {
			given().when().post("/games/{id}/turn/complete", gameId).then().statusCode(200);
		}

		@Test
		@Order(7)
		@TestSecurity(user = "admin")
		void sameKeyDifferentVoteBodyReturns409() {
			given()
					.header("Content-Type", "application/json")
					.header("Idempotency-Key", "vote-key-conflict")
					.body("{\"votedUserId\": \"player2\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200);

			given()
					.header("Content-Type", "application/json")
					.header("Idempotency-Key", "vote-key-conflict")
					.body("{\"votedUserId\": \"player3\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(409)
					.body("type", is("IDEMPOTENCY_KEY_REUSED"));
		}
	}
}
