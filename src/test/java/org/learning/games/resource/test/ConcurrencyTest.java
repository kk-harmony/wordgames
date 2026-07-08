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
public class ConcurrencyTest {

	/**
	 * Domain-rule concurrency: duplicate turn without idempotency key is rejected as BAD_REQUEST.
	 * Optimistic-lock conflicts (409 CONFLICT) are covered by {@link org.learning.games.domain.GameServiceConcurrencyTest}.
	 */

	private static long secretWordId;

	private static long secretWordId() {
		if (secretWordId == 0) {
			secretWordId = given()
					.header("Content-Type", "application/json")
					.body("{\"authentic\": \"lime\", \"imposed\": \"lemon\"}")
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
	class TurnCompleteWithoutIdempotencyKey {

		private static long gameId;

		@Test
		@Order(1)
		@TestSecurity(user = "admin")
		void setup() {
			gameId = given()
					.header("Content-Type", "application/json")
					.body("{\"name\": \"Concurrency Game\"}")
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
		void secondTurnCompleteWithoutKeyReturnsBadRequest() {
			given()
					.when()
					.post("/games/{id}/turn/complete", gameId)
					.then()
					.statusCode(200);

			given()
					.when()
					.post("/games/{id}/turn/complete", gameId)
					.then()
					.statusCode(400)
					.body("type", is("BAD_REQUEST"));
		}
	}
}
