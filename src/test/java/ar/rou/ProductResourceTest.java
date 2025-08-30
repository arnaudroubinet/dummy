package ar.rou;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
class ProductResourceTest {

    @Test
    void testGetProducts() {
        given()
          .when().get("/products")
          .then()
             .statusCode(200)
             .body("size()", is(3))
             .body("[0].id", is(1))
             .body("[0].name", is("Laptop"))
             .body("[0].description", is("High-performance laptop"))
             .body("[0].price", is(999.99f));
    }

    @Test
    void testGetProductById() {
        given()
          .when().get("/products/2")
          .then()
             .statusCode(200)
             .body("id", is(2))
             .body("name", is("Mouse"))
             .body("description", is("Wireless optical mouse"))
             .body("price", is(29.99f));
    }

    @Test
    void testGetProductByIdNotFound() {
        given()
          .when().get("/products/999")
          .then()
             .statusCode(404);
    }
}