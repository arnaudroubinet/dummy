package ar.rou;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class SystemResource {

    @GET
    @Path("/health")
    public Map<String, String> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        return health;
    }

    @GET
    @Path("/version")
    public Map<String, String> version() {
        Map<String, String> version = new HashMap<>();
        version.put("version", "1.0.1-SNAPSHOT");
        version.put("build", "development");
        return version;
    }

    @GET
    @Path("/status")
    public Map<String, Object> status() {
        Map<String, Object> status = new HashMap<>();
        status.put("application", "dummy");
        status.put("uptime", "running");
        status.put("environment", "development");
        status.put("features", new String[]{"rest", "jackson", "openapi"});
        return status;
    }

    @GET
    @Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "pong";
    }
}