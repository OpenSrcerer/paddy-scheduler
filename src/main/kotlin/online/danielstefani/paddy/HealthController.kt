package online.danielstefani.paddy

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/health")
class HealthController {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun health() = ":)"
}