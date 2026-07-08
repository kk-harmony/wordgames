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
public class GameResourceTest {

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class JoinLeaveFlow {

		private static Long gameId;

		@Test
		@Order(1)
		@TestSecurity(user = "creator")
		void creatorCreatesGame() {
			gameId = given()
					.header("Content-Type", "application/json")
					.body("{\"name\": \"Joinable Game\"}")
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
		@TestSecurity(user = "joiner")
		void otherUserCanJoinGame() {
			given()
					.queryParam("displayName", "Alex")
					.when()
					.post("/games/{id}/members", gameId)
					.then()
					.statusCode(201)
					.body("members.size()", is(2))
					.body("members.find { it.userId == 'joiner' }.displayName", is("Alex"));
		}

		@Test
		@Order(3)
		@TestSecurity(user = "joiner")
		void memberCanLeaveGame() {
			given()
					.header("Content-Type", "application/json")
					.when()
					.delete("/games/{id}/members/{userId}", gameId, "joiner")
					.then()
					.statusCode(204);
		}

		@Test
		@Order(4)
		@TestSecurity(user = "outsider")
		void nonMemberCannotViewGame() {
			given()
					.when()
					.get("/games/{id}", gameId)
					.then()
					.statusCode(403);
		}
	}

	@Test
	@TestSecurity(user = "creator")
	public void testCreateGameWithDisplayNameSetsAdminDisplayName() {
		given()
				.header("Content-Type", "application/json")
				.body("{\"name\": \"Named Game\", \"displayName\": \"  Host  \"}")
				.when()
				.post("/games")
				.then()
				.statusCode(201)
				.body("members[0].displayName", is("Host"));
	}

	@Test
	@TestSecurity(user = "creator")
	public void testJoinRejectsDisplayNameLongerThanThirtyCharacters() {
		long gameId = given()
				.header("Content-Type", "application/json")
				.body("{\"name\": \"Name Limit Game\"}")
				.when()
				.post("/games")
				.then()
				.statusCode(201)
				.extract()
				.jsonPath()
				.getLong("id");

		given()
				.queryParam("displayName", "a".repeat(31))
				.when()
				.post("/games/{id}/members", gameId)
				.then()
				.statusCode(400);
	}

	@Test
	@TestSecurity(user = "creator")
	public void testCreateGameSetsCreatorAsAdmin() {
		given()
				.header("Content-Type", "application/json")
				.body("{\"name\": \"Word Game\"}")
				.when()
				.post("/games")
				.then()
				.statusCode(201)
				.body("name", is("Word Game"))
				.body("adminUserId", is("creator"))
				.body("members.size()", is(1))
				.body("members[0].userId", is("creator"))
				.body("members[0].role", is("ADMIN"));
	}

	@Test
	@TestSecurity(user = "creator")
	public void testAdminCannotLeaveGame() {
		long gameId = given()
				.header("Content-Type", "application/json")
				.body("{\"name\": \"Leave Game\"}")
				.when()
				.post("/games")
				.then()
				.statusCode(201)
				.extract()
				.jsonPath()
				.getLong("id");

		given()
				.header("Content-Type", "application/json")
				.when()
				.delete("/games/{id}/members/{userId}", gameId, "creator")
				.then()
				.statusCode(400);
	}

	@Test
	@TestSecurity(user = "creator")
	public void testGetGameRequiresMembership() {
		long gameId = given()
				.header("Content-Type", "application/json")
				.body("{\"name\": \"Private Game\"}")
				.when()
				.post("/games")
				.then()
				.statusCode(201)
				.extract()
				.jsonPath()
				.getLong("id");

		given()
				.when()
				.get("/games/{id}", gameId)
				.then()
				.statusCode(200)
				.body("name", is("Private Game"));
	}

	@Test
	public void testCreateGameRequiresAuthentication() {
		given()
				.header("Content-Type", "application/json")
				.body("{\"name\": \"Unauthorized Game\"}")
				.when()
				.post("/games")
				.then()
				.statusCode(401);
	}

	@Test
	@TestSecurity(user = "creator")
	public void testJoinNonExistentGameReturns404() {
		given()
				.header("Content-Type", "application/json")
				.when()
				.post("/games/{id}/members", 99999L)
				.then()
				.statusCode(404);
	}
}
