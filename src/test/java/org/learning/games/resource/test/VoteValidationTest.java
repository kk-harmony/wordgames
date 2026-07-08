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
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class VoteValidationTest {

	private static long secretWordId;

	private static long secretWordId() {
		if (secretWordId == 0) {
			secretWordId = given()
					.header("Content-Type", "application/json")
					.body("{\"authentic\": \"vote-val-a\", \"imposed\": \"vote-val-i\"}")
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
	class VoteValidationFlow {

		private static long gameId;

		@Test
		@Order(1)
		@TestSecurity(user = "vote-admin")
		void setupGameInVoting() {
			gameId = given()
					.header("Content-Type", "application/json")
					.body("{\"name\": \"Vote Validation\"}")
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
		@TestSecurity(user = "vote-p2")
		void playerTwoJoins() {
			given().when().post("/games/{id}/members", gameId).then().statusCode(201);
		}

		@Test
		@Order(3)
		@TestSecurity(user = "vote-p3")
		void playerThreeJoins() {
			given().when().post("/games/{id}/members", gameId).then().statusCode(201);
		}

		@Test
		@Order(4)
		@TestSecurity(user = "vote-admin")
		void startAndCompleteAllTurns() {
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
		@TestSecurity(user = "vote-p2")
		void playerTwoTurn() {
			given().when().post("/games/{id}/turn/complete", gameId).then().statusCode(200);
		}

		@Test
		@Order(6)
		@TestSecurity(user = "vote-p3")
		void playerThreeTurnEntersVoting() {
			given()
					.when()
					.post("/games/{id}/turn/complete", gameId)
					.then()
					.statusCode(200)
					.body("status", is("VOTING"));
		}

		@Test
		@Order(7)
		@TestSecurity(user = "vote-admin")
		void cannotVoteForSelf() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"vote-admin\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(400);
		}

		@Test
		@Order(8)
		@TestSecurity(user = "vote-admin")
		void validVoteSucceeds() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"vote-p2\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200);
		}

		@Test
		@Order(9)
		@TestSecurity(user = "vote-admin")
		void cannotVoteTwice() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"vote-p3\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(400);
		}
	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class FourthTieBreakFlow {

		private static long gameId;

		@Test
		@Order(1)
		@TestSecurity(user = "tie4-admin")
		void adminCreatesGame() {
			gameId = given()
					.header("Content-Type", "application/json")
					.body("{\"name\": \"Fourth Tie\"}")
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
		@TestSecurity(user = "tie4-p2")
		void playerTwoJoins() {
			given().when().post("/games/{id}/members", gameId).then().statusCode(201);
		}

		@Test
		@Order(3)
		@TestSecurity(user = "tie4-p3")
		void playerThreeJoins() {
			given().when().post("/games/{id}/members", gameId).then().statusCode(201);
		}

		@Test
		@Order(4)
		@TestSecurity(user = "tie4-admin")
		void adminStartsGame() {
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
		@TestSecurity(user = "tie4-admin")
		void adminTurn() {
			given().when().post("/games/{id}/turn/complete", gameId).then().statusCode(200);
		}

		@Test
		@Order(6)
		@TestSecurity(user = "tie4-p2")
		void playerTwoTurn() {
			given().when().post("/games/{id}/turn/complete", gameId).then().statusCode(200);
		}

		@Test
		@Order(7)
		@TestSecurity(user = "tie4-p3")
		void playerThreeTurn() {
			given().when().post("/games/{id}/turn/complete", gameId).then().statusCode(200);
		}

		@Test
		@Order(8)
		@TestSecurity(user = "tie4-admin")
		void tieRoundOneVoteOne() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"tie4-p2\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200);
		}

		@Test
		@Order(9)
		@TestSecurity(user = "tie4-p2")
		void tieRoundOneVoteTwo() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"tie4-p3\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200);
		}

		@Test
		@Order(10)
		@TestSecurity(user = "tie4-p3")
		void tieRoundOneVoteThreeResets() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"tie4-admin\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200)
					.body("voteResetCount", is(1))
					.body("status", is("VOTING"));
		}

		@Test
		@Order(11)
		@TestSecurity(user = "tie4-admin")
		void tieRoundTwoVoteOne() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"tie4-p2\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200);
		}

		@Test
		@Order(12)
		@TestSecurity(user = "tie4-p2")
		void tieRoundTwoVoteTwo() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"tie4-p3\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200);
		}

		@Test
		@Order(13)
		@TestSecurity(user = "tie4-p3")
		void tieRoundTwoVoteThreeResets() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"tie4-admin\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200)
					.body("voteResetCount", is(2));
		}

		@Test
		@Order(14)
		@TestSecurity(user = "tie4-admin")
		void tieRoundThreeVoteOne() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"tie4-p2\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200);
		}

		@Test
		@Order(15)
		@TestSecurity(user = "tie4-p2")
		void tieRoundThreeVoteTwo() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"tie4-p3\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200);
		}

		@Test
		@Order(16)
		@TestSecurity(user = "tie4-p3")
		void tieRoundThreeVoteThreeResets() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"tie4-admin\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200)
					.body("voteResetCount", is(3));
		}

		@Test
		@Order(17)
		@TestSecurity(user = "tie4-admin")
		void tieRoundFourVoteOne() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"tie4-p2\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200);
		}

		@Test
		@Order(18)
		@TestSecurity(user = "tie4-p2")
		void tieRoundFourVoteTwo() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"tie4-p3\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200);
		}

		@Test
		@Order(19)
		@TestSecurity(user = "tie4-p3")
		void tieRoundFourVoteThreeFinishesGame() {
			given()
					.header("Content-Type", "application/json")
					.body("{\"votedUserId\": \"tie4-admin\"}")
					.when()
					.post("/games/{id}/vote", gameId)
					.then()
					.statusCode(200)
					.body("status", is("FINISHED"))
					.body("outcome", notNullValue());
		}
	}
}
