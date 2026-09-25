package com.teresol.meraapnabank.resource;

import com.teresol.meraapnabank.dto.BranchDto;
import com.teresol.meraapnabank.dto.NewBranchRequest;
import com.teresol.meraapnabank.dto.UpdateBranchRequest;
import com.teresol.meraapnabank.service.BranchService;
import jakarta.inject.Inject;
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

@Path("/branches")
public class BranchResource {

    @Inject
    BranchService branchService;

    @GET
    @Path("/{region}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<BranchDto> getBranches(@PathParam("region") String region) {
        return branchService.getBranches(region);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> createBranch(NewBranchRequest request) {
        return branchService.createBranch(request);
    }

    @PUT
    @Path("/{region}/{branchCode}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> updateBranch(@PathParam("region") String region,
            @PathParam("branchCode") String branchCode, UpdateBranchRequest request) {
        return branchService.updateBranch(region, branchCode, request);
    }

    @DELETE
    @Path("/{region}/{branchCode}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> deleteBranch(@PathParam("region") String region,
            @PathParam("branchCode") String branchCode) {
        return branchService.deleteBranch(region, branchCode);
    }
}
