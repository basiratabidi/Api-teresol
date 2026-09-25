package com.teresol.meraapnabank.resource;

import com.teresol.meraapnabank.datasource.Datasources;
import com.teresol.meraapnabank.datasource.RegionType;
import com.teresol.meraapnabank.dto.BranchDto;
import com.teresol.meraapnabank.dto.NewBranchRequest;
import com.teresol.meraapnabank.dto.UpdateBranchRequest;
import com.teresol.meraapnabank.util.QueryBuilder;
import com.teresol.meraapnabank.util.Selection;
import com.teresol.meraapnabank.util.Tables;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/branches")
public class BranchResource {

    // PostgreSQL SQLSTATE for unique_violation
    private static final String UNIQUE_VIOLATION = "23505";

    @Inject
    Datasources datasources;

    @GET
    @Path("/{region}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<BranchDto> getBranches(@PathParam("region") String region) throws Exception {
        RegionType regionType = parseRegion(region);
        List<BranchDto> results = new ArrayList<>();

        try (Connection connection = datasources.getConnection(regionType);
                PreparedStatement statement = QueryBuilder.buildSelect(connection, Tables.BRANCH, List.of());
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                BranchDto dto = new BranchDto();
                dto.branchCode = resultSet.getString("branch_code");
                dto.branchName = resultSet.getString("branch_name");
                dto.city = resultSet.getString("city");
                dto.regionCode = resultSet.getString("region_code");
                results.add(dto);
            }
        }
        return results;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> createBranch(NewBranchRequest request) throws Exception {
        validate(request);

        RegionType regionType = parseRegion(request.regionCode);
        List<Selection> values = List.of(
                new Selection("branch_code", request.branchCode),
                new Selection("branch_name", request.branchName),
                new Selection("city", request.city),
                new Selection("region_code", regionType.name()));

        try (Connection connection = datasources.getConnection(regionType);
                PreparedStatement statement = QueryBuilder.buildInsert(connection, Tables.BRANCH, values)) {

            int rowsInserted;
            try {
                rowsInserted = statement.executeUpdate();
            } catch (SQLException e) {
                if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
                    throw new ClientErrorException("Branch " + request.branchCode + " already exists in "
                            + regionType.name(), Response.Status.CONFLICT);
                }
                throw e;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("rowsInserted", rowsInserted);
            result.put("branchCode", request.branchCode);
            return result;
        }
    }

    @PUT
    @Path("/{region}/{branchCode}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> updateBranch(@PathParam("region") String region,
            @PathParam("branchCode") String branchCode, UpdateBranchRequest request) throws Exception {
        validate(request);
        RegionType regionType = parseRegion(region);
        List<Selection> values = List.of(
                new Selection("branch_name", request.branchName),
                new Selection("city", request.city));
        List<Selection> where = List.of(new Selection("branch_code", branchCode));

        try (Connection connection = datasources.getConnection(regionType);
                PreparedStatement statement = QueryBuilder.buildUpdate(connection, Tables.BRANCH, values, where)) {

            int rowsUpdated = statement.executeUpdate();
            if (rowsUpdated == 0) {
                throw branchNotFound(regionType, branchCode);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("rowsUpdated", rowsUpdated);
            result.put("branchCode", branchCode);
            return result;
        }
    }

    @DELETE
    @Path("/{region}/{branchCode}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> deleteBranch(@PathParam("region") String region,
            @PathParam("branchCode") String branchCode) throws Exception {
        RegionType regionType = parseRegion(region);
        List<Selection> where = List.of(new Selection("branch_code", branchCode));

        try (Connection connection = datasources.getConnection(regionType);
                PreparedStatement statement = QueryBuilder.buildDelete(connection, Tables.BRANCH, where)) {

            int rowsDeleted = statement.executeUpdate();
            if (rowsDeleted == 0) {
                throw branchNotFound(regionType, branchCode);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("rowsDeleted", rowsDeleted);
            result.put("branchCode", branchCode);
            return result;
        }
    }

    private RegionType parseRegion(String region) {
        try {
            return RegionType.valueOf(region.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown region: " + region + ". Valid regions: "
                    + Arrays.toString(RegionType.values()));
        }
    }

    private NotFoundException branchNotFound(RegionType region, String branchCode) {
        return new NotFoundException("Branch " + branchCode + " not found in " + region.name());
    }

    private void validate(UpdateBranchRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        if (request.branchName == null || request.branchName.isBlank()) {
            throw new BadRequestException("branchName is required");
        }
        if (request.city == null || request.city.isBlank()) {
            throw new BadRequestException("city is required");
        }
    }

    private void validate(NewBranchRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        if (request.branchCode == null || request.branchCode.isBlank()) {
            throw new BadRequestException("branchCode is required");
        }
        if (request.branchName == null || request.branchName.isBlank()) {
            throw new BadRequestException("branchName is required");
        }
        if (request.city == null || request.city.isBlank()) {
            throw new BadRequestException("city is required");
        }
        if (request.regionCode == null || request.regionCode.isBlank()) {
            throw new BadRequestException("regionCode is required");
        }
    }
}
