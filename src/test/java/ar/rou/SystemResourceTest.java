package ar.rou;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
class SystemResourceTest {

    @Test
    void testHealth() {
        given()
          .when().get("/health")
          .then()
             .statusCode(200)
             .body("status", is("UP"))
             .body("timestamp", notNullValue());
    }

    @Test
    void testVersion() {
        given()
          .when().get("/version")
          .then()
             .statusCode(200)
             .body("version", is("1.0.1-SNAPSHOT"))
             .body("build", is("development"));
    }

    @Test
    void testStatus() {
        given()
          .when().get("/status")
          .then()
             .statusCode(200)
             .body("application", is("dummy"))
             .body("uptime", is("running"))
             .body("environment", is("development"))
             .body("features.size()", is(3));
    }

    @Test
    void testPing() {
        given()
          .when().get("/ping")
          .then()
             .statusCode(200)
             .body(is("pong"));
    }
}