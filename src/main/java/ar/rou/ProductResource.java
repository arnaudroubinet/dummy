package ar.rou;

import ar.rou.model.Product;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    @GET
    public List<Product> getProducts() {
        return Arrays.asList(
            new Product(1L, "Laptop", "High-performance laptop", new BigDecimal("999.99")),
            new Product(2L, "Mouse", "Wireless optical mouse", new BigDecimal("29.99")),
            new Product(3L, "Keyboard", "Mechanical keyboard", new BigDecimal("79.99"))
        );
    }

    @GET
    @Path("/{id}")
    public Response getProductById(@PathParam("id") Long id) {
        if (id == 1L) {
            return Response.ok(new Product(1L, "Laptop", "High-performance laptop", new BigDecimal("999.99"))).build();
        } else if (id == 2L) {
            return Response.ok(new Product(2L, "Mouse", "Wireless optical mouse", new BigDecimal("29.99"))).build();
        } else if (id == 3L) {
            return Response.ok(new Product(3L, "Keyboard", "Mechanical keyboard", new BigDecimal("79.99"))).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}