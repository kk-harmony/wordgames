package org.learning.games.resource.test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class SecretWordResourceTest {

	@Test
	@TestSecurity(user = "creator")
	public void testGetByIdEndpointWhenWordInSelectionPool() {
		given()
				.header("Content-Type", "application/json")
				.body("{\"name\": \"Word Lookup Game\"}")
				.when()
				.post("/games")
				.then()
				.statusCode(201);

		long id = given()
				.header("Content-Type", "application/json")
				.body("{\"authentic\": \"word\", \"imposed\": \"similar word\"}")
				.when()
				.post("/secretwords")
				.then()
				.statusCode(201)
				.extract()
				.jsonPath()
				.getLong("id");

		given()
				.when().get("/secretwords/{id}", id)
				.then()
				.statusCode(200)
				.body("id", is((int) id))
				.body("authentic", is("word"))
				.body("imposed", is("similar word"));
	}

	@Test
	@TestSecurity(user = "outsider")
	public void testGetByIdForbiddenWithoutAdminAccess() {
		long id = given()
				.header("Content-Type", "application/json")
				.body("{\"authentic\": \"hidden\", \"imposed\": \"secret\"}")
				.when()
				.post("/secretwords")
				.then()
				.statusCode(201)
				.extract()
				.jsonPath()
				.getLong("id");

		given()
				.when()
				.get("/secretwords/{id}", id)
				.then()
				.statusCode(403);
	}

	@Test
	@TestSecurity(user = "creator")
	public void testGetByIdForbiddenForArbitraryWordWithoutWaitingGame() {
		long id = given()
				.header("Content-Type", "application/json")
				.body("{\"authentic\": \"alpha\", \"imposed\": \"beta\"}")
				.when()
				.post("/secretwords")
				.then()
				.statusCode(201)
				.extract()
				.jsonPath()
				.getLong("id");

		given()
				.when()
				.get("/secretwords/{id}", 99999L)
				.then()
				.statusCode(404);
	}

	@Test
	@TestSecurity(user = "creator")
	public void testCreateEndpoint() {
		given()
				.header("Content-Type", "application/json")
				.body("{\"authentic\": \"word\", \"imposed\": \"other word\"}")
				.when()
				.post("/secretwords")
				.then()
				.statusCode(201)
				.body("authentic", is("word"))
				.body("imposed", is("other word"));
	}

	@Test
	@TestSecurity(user = "creator")
	public void testRandomEndpoint() {
		given()
				.when()
				.get("/secretwords/random")
				.then()
				.statusCode(200)
				.body("id", notNullValue())
				.body("authentic", notNullValue())
				.body("imposed", notNullValue());
	}

	@Test
	public void testCreateEndpointRequiresAuthentication() {
		given()
				.header("Content-Type", "application/json")
				.body("{\"authentic\": \"word\", \"imposed\": \"other word\"}")
				.when()
				.post("/secretwords")
				.then()
				.statusCode(401);
	}
}
