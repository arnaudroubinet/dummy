package ar.rou;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
class UserResourceTest {

    @Test
    void testGetUsers() {
        given()
          .when().get("/users")
          .then()
             .statusCode(200)
             .body("size()", is(3))
             .body("[0].id", is(1))
             .body("[0].name", is("John Doe"))
             .body("[0].email", is("john.doe@example.com"));
    }

    @Test
    void testGetUserById() {
        given()
          .when().get("/users/1")
          .then()
             .statusCode(200)
             .body("id", is(1))
             .body("name", is("John Doe"))
             .body("email", is("john.doe@example.com"));
    }

    @Test
    void testGetUserByIdNotFound() {
        given()
          .when().get("/users/999")
          .then()
             .statusCode(404);
    }

    @Test
    void testCreateUser() {
        given()
          .contentType("application/json")
          .body("{\"name\":\"Test User\",\"email\":\"test@example.com\"}")
          .when().post("/users")
          .then()
             .statusCode(201)
             .body("id", is(99))
             .body("name", is("Test User"))
             .body("email", is("test@example.com"));
    }
}