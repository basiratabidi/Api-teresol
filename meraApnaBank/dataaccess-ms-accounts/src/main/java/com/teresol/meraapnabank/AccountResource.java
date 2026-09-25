package com.teresol.meraapnabank;

import com.teresol.meraapnabank.util.QueryBuilder;
import com.teresol.meraapnabank.util.Selection;
import com.teresol.meraapnabank.util.Tables;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
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
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Path("/accounts")
public class AccountResource {

    @Inject
    Datasources datasources;

    @GET
    @Path("/{region}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AccountDto> getAccounts(@PathParam("region") String region) throws Exception {
        RegionType regionType = RegionType.parse(region);
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

        RegionType regionType = RegionType.parse(request.regionCode);
        validateCodePrefix(request.accountNumber, regionType);

        List<Selection> values = List.of(
                new Selection("account_number", request.accountNumber),
                new Selection("account_holder_name", request.accountHolderName),
                new Selection("branch_code", request.branchCode),
                new Selection("account_type", request.accountType),
                new Selection("balance", request.balance),
                new Selection("region_code", regionType.name()));

        try (Connection connection = datasources.getConnection(regionType);
                PreparedStatement statement = QueryBuilder.buildInsert(connection, Tables.ACCOUNT, values)) {

            int rowsInserted = statement.executeUpdate();

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
        validateUpdate(request);

        RegionType regionType = RegionType.parse(region);
        List<Selection> values = List.of(
                new Selection("account_holder_name", request.accountHolderName),
                new Selection("account_type", request.accountType),
                new Selection("balance", request.balance));
        List<Selection> where = List.of(new Selection("account_number", accountNumber));

        try (Connection connection = datasources.getConnection(regionType);
                PreparedStatement statement = QueryBuilder.buildUpdate(connection, Tables.ACCOUNT, values, where)) {

            int rowsUpdated = statement.executeUpdate();

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
        RegionType regionType = RegionType.parse(region);
        List<Selection> where = List.of(new Selection("account_number", accountNumber));

        try (Connection connection = datasources.getConnection(regionType);
                PreparedStatement statement = QueryBuilder.buildDelete(connection, Tables.ACCOUNT, where)) {

            int rowsDeleted = statement.executeUpdate();

            Map<String, Object> result = new HashMap<>();
            result.put("rowsDeleted", rowsDeleted);
            result.put("accountNumber", accountNumber);
            return result;
        }
    }

    @POST
    @Path("/{region}/{accountNumber}/deposit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> deposit(@PathParam("region") String region,
            @PathParam("accountNumber") String accountNumber, AmountRequest request) throws Exception {
        validateAmount(request);
        RegionType regionType = RegionType.parse(region);

        String sql = "UPDATE account SET balance = balance + ? WHERE account_number = ?";
        try (Connection connection = datasources.getConnection(regionType);
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, request.amount);
            statement.setString(2, accountNumber);

            int rowsUpdated = statement.executeUpdate();
            if (rowsUpdated == 0) {
                throw new NotFoundException("Account '" + accountNumber + "' not found");
            }
            return balanceResult(connection, accountNumber, "deposited");
        }
    }

    @POST
    @Path("/{region}/{accountNumber}/withdraw")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> withdraw(@PathParam("region") String region,
            @PathParam("accountNumber") String accountNumber, AmountRequest request) throws Exception {
        validateAmount(request);
        RegionType regionType = RegionType.parse(region);

        String sql = "UPDATE account SET balance = balance - ? WHERE account_number = ? AND balance >= ?";
        try (Connection connection = datasources.getConnection(regionType);
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, request.amount);
            statement.setString(2, accountNumber);
            statement.setBigDecimal(3, request.amount);

            int rowsUpdated = statement.executeUpdate();
            if (rowsUpdated == 0) {
                ensureAccountCanCoverAmount(connection, accountNumber);
            }
            return balanceResult(connection, accountNumber, "withdrawn");
        }
    }

    @POST
    @Path("/{region}/transfer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> transfer(@PathParam("region") String region, TransferRequest request)
            throws Exception {
        validateTransfer(request);
        RegionType regionType = RegionType.parse(region);

        try (Connection connection = datasources.getConnection(regionType)) {
            connection.setAutoCommit(false);
            try {
                String withdrawSql = "UPDATE account SET balance = balance - ? WHERE account_number = ? AND balance >= ?";
                try (PreparedStatement statement = connection.prepareStatement(withdrawSql)) {
                    statement.setBigDecimal(1, request.amount);
                    statement.setString(2, request.fromAccountNumber);
                    statement.setBigDecimal(3, request.amount);

                    if (statement.executeUpdate() == 0) {
                        ensureAccountCanCoverAmount(connection, request.fromAccountNumber);
                    }
                }

                String depositSql = "UPDATE account SET balance = balance + ? WHERE account_number = ?";
                try (PreparedStatement statement = connection.prepareStatement(depositSql)) {
                    statement.setBigDecimal(1, request.amount);
                    statement.setString(2, request.toAccountNumber);

                    if (statement.executeUpdate() == 0) {
                        throw new NotFoundException("Account '" + request.toAccountNumber + "' not found");
                    }
                }

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("fromAccountNumber", request.fromAccountNumber);
            result.put("toAccountNumber", request.toAccountNumber);
            result.put("amount", request.amount);
            result.put("status", "transferred");
            return result;
        }
    }

    private Map<String, Object> balanceResult(Connection connection, String accountNumber, String action)
            throws Exception {
        String sql = "SELECT balance FROM account WHERE account_number = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                Map<String, Object> result = new HashMap<>();
                result.put("accountNumber", accountNumber);
                result.put("balance", resultSet.getBigDecimal("balance"));
                result.put("status", action);
                return result;
            }
        }
    }

    private void ensureAccountCanCoverAmount(Connection connection, String accountNumber) throws Exception {
        String sql = "SELECT 1 FROM account WHERE account_number = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new NotFoundException("Account '" + accountNumber + "' not found");
                }
            }
        }
        throw new BadRequestException("Insufficient funds in account '" + accountNumber + "'");
    }

    private void validateAmount(AmountRequest request) {
        if (request.amount == null || request.amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("amount must be positive");
        }
    }

    private void validateTransfer(TransferRequest request) {
        if (request.fromAccountNumber == null || request.fromAccountNumber.isBlank()) {
            throw new BadRequestException("fromAccountNumber is required");
        }
        if (request.toAccountNumber == null || request.toAccountNumber.isBlank()) {
            throw new BadRequestException("toAccountNumber is required");
        }
        if (request.fromAccountNumber.equalsIgnoreCase(request.toAccountNumber)) {
            throw new BadRequestException("fromAccountNumber and toAccountNumber must differ");
        }
        if (request.amount == null || request.amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("amount must be positive");
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

    private void validate(NewAccountRequest request) {
        if (request.accountNumber == null || request.accountNumber.isBlank()) {
            throw new BadRequestException("accountNumber is required");
        }
        if (request.accountHolderName == null || request.accountHolderName.isBlank()) {
            throw new BadRequestException("accountHolderName is required");
        }
        if (request.branchCode == null || request.branchCode.isBlank()) {
            throw new BadRequestException("branchCode is required");
        }
        if (request.accountType == null
                || !(request.accountType.equalsIgnoreCase("SAVINGS") || request.accountType.equalsIgnoreCase("CURRENT"))) {
            throw new BadRequestException("accountType must be SAVINGS or CURRENT");
        }
        if (request.balance == null || request.balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("balance must be zero or positive");
        }
        if (request.regionCode == null || request.regionCode.isBlank()) {
            throw new BadRequestException("regionCode is required");
        }
    }

    private void validateCodePrefix(String accountNumber, RegionType regionType) {
        String requiredPrefix = regionType.codePrefix() + "-";
        if (!accountNumber.toUpperCase().startsWith(requiredPrefix)) {
            throw new BadRequestException(
                    "accountNumber for region " + regionType.name() + " must start with '" + requiredPrefix + "'");
        }
    }

    private void validateUpdate(UpdateAccountRequest request) {
        if (request.accountHolderName == null || request.accountHolderName.isBlank()) {
            throw new BadRequestException("accountHolderName is required");
        }
        if (request.accountType == null
                || !(request.accountType.equalsIgnoreCase("SAVINGS") || request.accountType.equalsIgnoreCase("CURRENT"))) {
            throw new BadRequestException("accountType must be SAVINGS or CURRENT");
        }
        if (request.balance == null || request.balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("balance must be zero or positive");
        }
    }
}