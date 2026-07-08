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
public class KickFlowTest {

	private static long secretWordId;

	private static long secretWordId() {
		if (secretWordId == 0) {
			secretWordId = given()
					.header("Content-Type", "application/json")
					.body("{\"authentic\": \"grape\", \"imposed\": \"berry\"}")
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
	class KickDisconnectedPlayer {

		private static long gameId;

		@Test
		@Order(1)
		@TestSecurity(user = "admin")
		void adminCreatesGame() {
			gameId = given()
					.header("Content-Type", "application/json")
					.body("{\"name\": \"Kick Game\"}")
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
			joinAs("player2");
		}

		@Test
		@Order(3)
		@TestSecurity(user = "player3")
		void playerThreeJoins() {
			joinAs("player3");
		}

		@Test
		@Order(4)
		@TestSecurity(user = "player4")
		void playerFourJoins() {
			joinAs("player4");
		}

		@Test
		@Order(5)
		@TestSecurity(user = "player5")
		void playerFiveJoins() {
			joinAs("player5");
		}

		@Test
		@Order(6)
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
					.body("members.size()", is(5));
		}

		@Test
		@Order(7)
		@TestSecurity(user = "admin")
		void adminKicksDisconnectedPlayer() {
			given()
					.when()
					.delete("/games/{id}/members/{userId}", gameId, "player5")
					.then()
					.statusCode(200)
					.body("members.size()", is(4));
		}

		@Test
		@Order(8)
		@TestSecurity(user = "player2")
		void nonAdminCannotKick() {
			given()
					.when()
					.delete("/games/{id}/members/{userId}", gameId, "player3")
					.then()
					.statusCode(403);
		}

		@Test
		@Order(9)
		@TestSecurity(user = "player4")
		void memberCannotLeaveAfterGameStarts() {
			given()
					.when()
					.delete("/games/{id}/members/{userId}", gameId, "player4")
					.then()
					.statusCode(400);
		}

		private void joinAs(String user) {
			given()
					.when()
					.post("/games/{id}/members", gameId)
					.then()
					.statusCode(201);
		}
	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class KickInThreePlayerGame {

		private static long gameId;

		@Test
		@Order(1)
		@TestSecurity(user = "admin")
		void createGame() {
			gameId = given()
					.header("Content-Type", "application/json")
					.body("{\"name\": \"Three Player Kick Game\"}")
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
					.statusCode(200)
					.body("members.size()", is(3));
		}

		@Test
		@Order(5)
		@TestSecurity(user = "admin")
		void adminKicksOfflinePlayer() {
			given()
					.when()
					.delete("/games/{id}/members/{userId}", gameId, "player3")
					.then()
					.statusCode(200)
					.body("members.size()", is(2))
					.body("status", is("FINISHED"));
		}
	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class KickRejectedWhenTooFewPlayers {

		private static long gameId;

		@Test
		@Order(1)
		@TestSecurity(user = "admin")
		void createGame() {
			gameId = given()
					.header("Content-Type", "application/json")
					.body("{\"name\": \"Two Player Kick Game\"}")
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
		@TestSecurity(user = "admin")
		void cannotStartWithTwoPlayers() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"secretWordId\": " + secretWordId() + "}")
					.when()
					.post("/games/{id}/start", gameId)
					.then()
					.statusCode(400);
		}
	}
}
