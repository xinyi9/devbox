package org.viqueen.devbox.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.viqueen.devbox.services.SampleCommunityService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.lang.management.ManagementFactory;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

@Path("/health")
public class HealthCheckResource {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckResource.class);
    private final SampleCommunityService communityService;

    public HealthCheckResource(final SampleCommunityService communityService) {
        this.communityService = communityService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/ping")
    public Response ping() {
        log.info("** health / ping");
        return Response.ok(
                singletonMap(
                        "components",
                        singletonList(
                                communityService.toString()
                        )
                )
        ).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/pid")
    public Response pid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return Response.ok(singletonMap(
                "pid", name.split("@")[0]
        )).build();
    }
}
