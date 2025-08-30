package ar.rou;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
class OrderResourceTest {

    @Test
    void testGetOrders() {
        given()
          .when().get("/orders")
          .then()
             .statusCode(200)
             .body("size()", is(3))
             .body("[0].id", is(1))
             .body("[0].userId", is(1))
             .body("[0].status", is("COMPLETED"))
             .body("[0].productIds.size()", is(2));
    }
}