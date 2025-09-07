package ar.rou;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
class PreferencesResourceTest {

    @Test
    void testGetUserPreferences() {
        given()
          .when().get("/users/1/preferences")
          .then()
             .statusCode(200)
             .body("theme", is("dark"))
             .body("notifications", is(true));
    }

    @Test
    void testGetUserPreferencesNotFound() {
        given()
          .when().get("/users/999/preferences")
          .then()
             .statusCode(404);
    }
}