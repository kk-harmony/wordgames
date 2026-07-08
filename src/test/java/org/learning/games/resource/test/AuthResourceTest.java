package org.learning.games.resource.test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class AuthResourceTest {

	@Test
	@TestSecurity(user = "test-user")
	public void testMeEndpointReturnsAuthenticatedUser() {
		given()
				.when()
				.get("/auth/me")
				.then()
				.statusCode(200)
				.body("userId", is("test-user"));
	}

	@Test
	public void testMeEndpointRequiresAuthentication() {
		given()
				.when()
				.get("/auth/me")
				.then()
				.statusCode(401);
	}
}
