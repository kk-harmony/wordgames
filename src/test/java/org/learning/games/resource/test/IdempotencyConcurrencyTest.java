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
public class IdempotencyConcurrencyTest {

	/**
	 * REST replay after duplicate action with the same idempotency key.
	 * Parallel first-time key races are covered by {@link org.learning.games.domain.IdempotencyServiceConcurrencyTest}.
	 */

	private static long secretWordId;

	private static long secretWordId() {
		if (secretWordId == 0) {
			secretWordId = given()
					.header("Content-Type", "application/json")
					.body("{\"authentic\": \"pear\", \"imposed\": \"apple\"}")
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
	class ReplayAfterDuplicateAction {

		private static long gameId;

		@Test
		@Order(1)
		@TestSecurity(user = "admin")
		void setup() {
			gameId = given()
					.header("Content-Type", "application/json")
					.body("{\"name\": \"Idem Concurrency\"}")
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
		void duplicateActionWithSameKeyReplaysInsteadOfFailing() {
			String key = "rapid-turn-key";

			given()
					.header("Idempotency-Key", key)
					.when()
					.post("/games/{id}/turn/complete", gameId)
					.then()
					.statusCode(200);

			given()
					.header("Idempotency-Key", key)
					.when()
					.post("/games/{id}/turn/complete", gameId)
					.then()
					.statusCode(200)
					.body("status", is("IN_PROGRESS"));
		}
	}
}
