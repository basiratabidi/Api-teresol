package com.teresol.meraapnabank;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "branch-data-access")
@Path("/branches")
public interface BranchClient {

    @GET
    @Path("/{region}")
    @Produces(MediaType.APPLICATION_JSON)
    List<Map<String, Object>> getBranches(@PathParam("region") String region);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> createBranch(NewBranchRequest request);

    @PUT
    @Path("/{region}/{branchCode}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> updateBranch(@PathParam("region") String region, @PathParam("branchCode") String branchCode,
            UpdateBranchRequest request);

    @DELETE
    @Path("/{region}/{branchCode}")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> deleteBranch(@PathParam("region") String region, @PathParam("branchCode") String branchCode);
}
