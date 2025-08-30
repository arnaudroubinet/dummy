package ar.rou;

import ar.rou.model.User;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Arrays;
import java.util.List;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @GET
    public List<User> getUsers() {
        return Arrays.asList(
            new User(1L, "John Doe", "john.doe@example.com"),
            new User(2L, "Jane Smith", "jane.smith@example.com"),
            new User(3L, "Bob Johnson", "bob.johnson@example.com")
        );
    }

    @GET
    @Path("/{id}")
    public Response getUserById(@PathParam("id") Long id) {
        if (id == 1L) {
            return Response.ok(new User(1L, "John Doe", "john.doe@example.com")).build();
        } else if (id == 2L) {
            return Response.ok(new User(2L, "Jane Smith", "jane.smith@example.com")).build();
        } else if (id == 3L) {
            return Response.ok(new User(3L, "Bob Johnson", "bob.johnson@example.com")).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @POST
    public Response createUser(User user) {
        // Dummy implementation - just return the user with a generated ID
        user.setId(99L);
        return Response.status(Response.Status.CREATED).entity(user).build();
    }
}