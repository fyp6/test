package com.mycompany.project.resources;

import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 *
 * @author 
 */
@Path("jakartaee10")
public class JakartaEE10Resource {
    
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response ping(){
        return Response
                .ok("ping Jakarta EE 10 on GlassFish 7.0.24")
                .build();
    }

    @GET
    @Path("status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response status() {
        return Response.ok(Map.of(
                "app", "Project",
                "runtime", "GlassFish 7.0.24",
                "jakartaEE", "10"
        )).build();
    }
}
