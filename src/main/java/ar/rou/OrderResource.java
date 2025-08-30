package ar.rou;

import ar.rou.model.Order;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    @GET
    public List<Order> getOrders() {
        return Arrays.asList(
            new Order(1L, Arrays.asList(1L, 2L), "COMPLETED", LocalDateTime.now().minusDays(1)),
            new Order(2L, Arrays.asList(3L), "PENDING", LocalDateTime.now().minusHours(2)),
            new Order(3L, Arrays.asList(1L, 3L), "SHIPPED", LocalDateTime.now().minusHours(6))
        );
    }
}
