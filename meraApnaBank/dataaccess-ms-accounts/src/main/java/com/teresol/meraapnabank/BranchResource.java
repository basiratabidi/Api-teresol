package com.teresol.meraapnabank;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/branches")
public class BranchResource {

    @Inject
    Datasources datasources;

    @GET
    @Path("/{region}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<BranchDto> getBranches(@PathParam("region") String region) throws Exception {
        RegionType regionType = RegionType.valueOf(region.toUpperCase());
        List<BranchDto> results = new ArrayList<>();

        try (Connection connection = datasources.getConnection(regionType);
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT branch_code, branch_name, city, region_code FROM branch")) {

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

        RegionType regionType = RegionType.valueOf(request.regionCode.toUpperCase());
        String insertSql = "INSERT INTO branch (branch_code, branch_name, city, region_code) VALUES (?, ?, ?, ?)";

        try (Connection connection = datasources.getConnection(regionType);
                PreparedStatement statement = connection.prepareStatement(insertSql)) {

            statement.setString(1, request.branchCode);
            statement.setString(2, request.branchName);
            statement.setString(3, request.city);
            statement.setString(4, regionType.name());

            int rowsInserted = statement.executeUpdate();

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
        RegionType regionType = RegionType.valueOf(region.toUpperCase());
        String updateSql = "UPDATE branch SET branch_name = ?, city = ? WHERE branch_code = ?";

        try (Connection connection = datasources.getConnection(regionType);
                PreparedStatement statement = connection.prepareStatement(updateSql)) {

            statement.setString(1, request.branchName);
            statement.setString(2, request.city);
            statement.setString(3, branchCode);

            int rowsUpdated = statement.executeUpdate();

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
        RegionType regionType = RegionType.valueOf(region.toUpperCase());
        String deleteSql = "DELETE FROM branch WHERE branch_code = ?";

        try (Connection connection = datasources.getConnection(regionType);
                PreparedStatement statement = connection.prepareStatement(deleteSql)) {

            statement.setString(1, branchCode);
            int rowsDeleted = statement.executeUpdate();

            Map<String, Object> result = new HashMap<>();
            result.put("rowsDeleted", rowsDeleted);
            result.put("branchCode", branchCode);
            return result;
        }
    }

    private void validate(NewBranchRequest request) {
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
