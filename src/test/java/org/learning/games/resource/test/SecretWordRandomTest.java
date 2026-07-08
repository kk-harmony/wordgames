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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
public class SecretWordRandomTest {

	private static long createSecretWord(String authentic, String imposed) {
		return given()
				.header("Content-Type", "application/json")
				.body("{\"authentic\": \"" + authentic + "\", \"imposed\": \"" + imposed + "\"}")
				.when()
				.post("/secretwords")
				.then()
				.statusCode(201)
				.extract()
				.jsonPath()
				.getLong("id");
	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class AdminExclusionFlow {

		private static long gameId;
		private static long usedWordId;

		@Test
		@Order(1)
		@TestSecurity(user = "excl-admin")
		void adminCreatesDedicatedWordAndGame() {
			usedWordId = createSecretWord("excl-authentic", "excl-imposed");

			gameId = given()
					.header("Content-Type", "application/json")
					.body("{\"name\": \"Exclusion Game\"}")
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
		@TestSecurity(user = "excl-p2")
		void playerTwoJoins() {
			given().when().post("/games/{id}/members", gameId).then().statusCode(201);
		}

		@Test
		@Order(3)
		@TestSecurity(user = "excl-p3")
		void playerThreeJoins() {
			given().when().post("/games/{id}/members", gameId).then().statusCode(201);
		}

		@Test
		@Order(4)
		@TestSecurity(user = "excl-admin")
		void adminStartsWithDedicatedWord() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"secretWordId\": " + usedWordId + "}")
					.when()
					.post("/games/{id}/start", gameId)
					.then()
					.statusCode(200)
					.body("status", is("IN_PROGRESS"));
		}

		@Test
		@Order(5)
		@TestSecurity(user = "excl-admin")
		void randomDoesNotReturnWordAlreadyUsedByAdmin() {
			long randomId = given()
					.when()
					.get("/secretwords/random")
					.then()
					.statusCode(200)
					.extract()
					.jsonPath()
					.getLong("id");

			assertThat(randomId, not(usedWordId));
		}
	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class RandomStartFlow {

		private static long gameId;

		@Test
		@Order(1)
		@TestSecurity(user = "rand-admin")
		void adminCreatesGame() {
			gameId = given()
					.header("Content-Type", "application/json")
					.body("{\"name\": \"Random Start Game\"}")
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
		@TestSecurity(user = "rand-p2")
		void playerTwoJoins() {
			given().when().post("/games/{id}/members", gameId).then().statusCode(201);
		}

		@Test
		@Order(3)
		@TestSecurity(user = "rand-p3")
		void playerThreeJoins() {
			given().when().post("/games/{id}/members", gameId).then().statusCode(201);
		}

		@Test
		@Order(4)
		@TestSecurity(user = "rand-admin")
		void adminStartsWithRandomSecretWord() {
			long randomId = given()
					.when()
					.get("/secretwords/random")
					.then()
					.statusCode(200)
					.extract()
					.jsonPath()
					.getLong("id");

			given()
					.header("Content-Type", "application/json")
					.body("{\"secretWordId\": " + randomId + "}")
					.when()
					.post("/games/{id}/start", gameId)
					.then()
					.statusCode(200)
					.body("status", is("IN_PROGRESS"))
					.body("currentRound", is(1));
		}
	}
}
