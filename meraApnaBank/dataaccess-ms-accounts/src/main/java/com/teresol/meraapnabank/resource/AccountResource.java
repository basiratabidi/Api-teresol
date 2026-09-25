package com.teresol.meraapnabank.resource;

import com.teresol.meraapnabank.datasource.Datasources;
import com.teresol.meraapnabank.datasource.RegionType;
import com.teresol.meraapnabank.dto.AccountDto;
import com.teresol.meraapnabank.dto.NewAccountRequest;
import com.teresol.meraapnabank.dto.UpdateAccountRequest;
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
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/accounts")
public class AccountResource {

    // PostgreSQL SQLSTATE for unique_violation
    private static final String UNIQUE_VIOLATION = "23505";

    @Inject
    Datasources datasources;

    @GET
    @Path("/{region}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AccountDto> getAccounts(@PathParam("region") String region) throws Exception {
        RegionType regionType = parseRegion(region);
        List<AccountDto> results = new ArrayList<>();

        try (Connection connection = datasources.getConnection(regionType);
                PreparedStatement statement = QueryBuilder.buildSelect(connection, Tables.ACCOUNT, List.of());
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                results.add(mapRow(resultSet));
            }
        }
        return results;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> createAccount(NewAccountRequest request) throws Exception {
        validate(request);

        RegionType regionType = parseRegion(request.regionCode);
        List<Selection> values = List.of(
                new Selection("account_number", request.accountNumber),
                new Selection("account_holder_name", request.accountHolderName),
                new Selection("branch_code", request.branchCode),
                new Selection("account_type", request.accountType),
                new Selection("balance", request.balance),
                new Selection("region_code", regionType.name()));

        try (Connection connection = datasources.getConnection(regionType);
                PreparedStatement statement = QueryBuilder.buildInsert(connection, Tables.ACCOUNT, values)) {

            int rowsInserted;
            try {
                rowsInserted = statement.executeUpdate();
            } catch (SQLException e) {
                if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
                    throw new ClientErrorException("Account " + request.accountNumber + " already exists in "
                            + regionType.name(), Response.Status.CONFLICT);
                }
                throw e;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("rowsInserted", rowsInserted);
            result.put("accountNumber", request.accountNumber);
            return result;
        }
    }

    @PUT
    @Path("/{region}/{accountNumber}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> updateAccount(@PathParam("region") String region,
            @PathParam("accountNumber") String accountNumber, UpdateAccountRequest request) throws Exception {
        validate(request);
        RegionType regionType = parseRegion(region);
        List<Selection> values = List.of(
                new Selection("account_holder_name", request.accountHolderName),
                new Selection("account_type", request.accountType),
                new Selection("balance", request.balance));
        List<Selection> where = List.of(new Selection("account_number", accountNumber));

        try (Connection connection = datasources.getConnection(regionType);
                PreparedStatement statement = QueryBuilder.buildUpdate(connection, Tables.ACCOUNT, values, where)) {

            int rowsUpdated = statement.executeUpdate();
            if (rowsUpdated == 0) {
                throw accountNotFound(regionType, accountNumber);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("rowsUpdated", rowsUpdated);
            result.put("accountNumber", accountNumber);
            return result;
        }
    }

    @DELETE
    @Path("/{region}/{accountNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> deleteAccount(@PathParam("region") String region,
            @PathParam("accountNumber") String accountNumber) throws Exception {
        RegionType regionType = parseRegion(region);
        List<Selection> where = List.of(new Selection("account_number", accountNumber));

        try (Connection connection = datasources.getConnection(regionType);
                PreparedStatement statement = QueryBuilder.buildDelete(connection, Tables.ACCOUNT, where)) {

            int rowsDeleted = statement.executeUpdate();
            if (rowsDeleted == 0) {
                throw accountNotFound(regionType, accountNumber);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("rowsDeleted", rowsDeleted);
            result.put("accountNumber", accountNumber);
            return result;
        }
    }

    private AccountDto mapRow(ResultSet resultSet) throws Exception {
        AccountDto dto = new AccountDto();
        dto.accountNumber = resultSet.getString("account_number");
        dto.accountHolderName = resultSet.getString("account_holder_name");
        dto.branchCode = resultSet.getString("branch_code");
        dto.accountType = resultSet.getString("account_type");
        dto.balance = resultSet.getBigDecimal("balance");
        dto.regionCode = resultSet.getString("region_code");
        return dto;
    }

    private RegionType parseRegion(String region) {
        try {
            return RegionType.valueOf(region.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown region: " + region + ". Valid regions: "
                    + Arrays.toString(RegionType.values()));
        }
    }

    private NotFoundException accountNotFound(RegionType region, String accountNumber) {
        return new NotFoundException("Account " + accountNumber + " not found in " + region.name());
    }

    private void validate(UpdateAccountRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        if (request.accountHolderName == null || request.accountHolderName.isBlank()) {
            throw new BadRequestException("accountHolderName is required");
        }
        if (request.accountType == null || request.accountType.isBlank()) {
            throw new BadRequestException("accountType is required");
        }
        if (request.balance == null || request.balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("balance must be zero or positive");
        }
    }

    private void validate(NewAccountRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        if (request.accountNumber == null || request.accountNumber.isBlank()) {
            throw new BadRequestException("accountNumber is required");
        }
        if (request.accountHolderName == null || request.accountHolderName.isBlank()) {
            throw new BadRequestException("accountHolderName is required");
        }
        if (request.branchCode == null || request.branchCode.isBlank()) {
            throw new BadRequestException("branchCode is required");
        }
        if (request.accountType == null || request.accountType.isBlank()) {
            throw new BadRequestException("accountType is required");
        }
        if (request.balance == null || request.balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("balance must be zero or positive");
        }
        if (request.regionCode == null || request.regionCode.isBlank()) {
            throw new BadRequestException("regionCode is required");
        }
    }
}
