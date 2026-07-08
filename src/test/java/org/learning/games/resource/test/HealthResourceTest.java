package org.learning.games.resource.test;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class HealthResourceTest {

	@Test
	public void healthEndpointIsPublic() {
		given()
				.when()
				.get("/q/health/ready")
				.then()
				.statusCode(200)
				.body("status", is("UP"));
	}
}
