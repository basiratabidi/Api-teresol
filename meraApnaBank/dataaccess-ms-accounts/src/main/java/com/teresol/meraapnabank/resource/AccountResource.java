package com.teresol.meraapnabank.resource;

import com.teresol.meraapnabank.datasource.Datasources;
import com.teresol.meraapnabank.datasource.RegionType;
import com.teresol.meraapnabank.dto.AccountDto;
import com.teresol.meraapnabank.dto.AmountRequest;
import com.teresol.meraapnabank.dto.NewAccountRequest;
import com.teresol.meraapnabank.dto.TransferRequest;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/accounts")
public class AccountResource {

    // PostgreSQL SQLSTATEs
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String FOREIGN_KEY_VIOLATION = "23503";

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
        List<Selection> values = List.of(
                new Selection("account_number", request.accountNumber),
                new Selection("account_holder_name", request.accountHolderName),
                new Selection("branch_code", request.branchCode),
                new Selection("account_type", request.accountType.toUpperCase()),
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
                if (FOREIGN_KEY_VIOLATION.equals(e.getSQLState())) {
                    throw new BadRequestException("Branch " + request.branchCode + " does not exist in "
                            + regionType.name());
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
        RegionType regionType = RegionType.parse(region);
        List<Selection> values = List.of(
                new Selection("account_holder_name", request.accountHolderName),
                new Selection("account_type", request.accountType.toUpperCase()),
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
        RegionType regionType = RegionType.parse(region);
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

    @POST
    @Path("/{region}/{accountNumber}/deposit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> deposit(@PathParam("region") String region,
            @PathParam("accountNumber") String accountNumber, AmountRequest request) throws Exception {
        validateAmount(request == null ? null : request.amount);
        RegionType regionType = RegionType.parse(region);

        String sql = "UPDATE account SET balance = balance + ? WHERE account_number = ? RETURNING balance";
        try (Connection connection = datasources.getConnection(regionType);
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, request.amount);
            statement.setString(2, accountNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw accountNotFound(regionType, accountNumber);
                }
                return balanceResult(accountNumber, resultSet.getBigDecimal("balance"), "deposited");
            }
        }
    }

    @POST
    @Path("/{region}/{accountNumber}/withdraw")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> withdraw(@PathParam("region") String region,
            @PathParam("accountNumber") String accountNumber, AmountRequest request) throws Exception {
        validateAmount(request == null ? null : request.amount);
        RegionType regionType = RegionType.parse(region);

        // The balance check and the debit happen in one statement, so concurrent
        // withdrawals can never take the balance below zero.
        String sql = "UPDATE account SET balance = balance - ? WHERE account_number = ? AND balance >= ? RETURNING balance";
        try (Connection connection = datasources.getConnection(regionType);
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, request.amount);
            statement.setString(2, accountNumber);
            statement.setBigDecimal(3, request.amount);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return balanceResult(accountNumber, resultSet.getBigDecimal("balance"), "withdrawn");
                }
            }
            if (findBalance(connection, accountNumber, false) == null) {
                throw accountNotFound(regionType, accountNumber);
            }
            throw insufficientFunds(accountNumber);
        }
    }

    @POST
    @Path("/{region}/transfer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> transfer(@PathParam("region") String region, TransferRequest request)
            throws Exception {
        validate(request);
        RegionType regionType = RegionType.parse(region);

        try (Connection connection = datasources.getConnection(regionType)) {
            connection.setAutoCommit(false);
            try {
                // Lock both rows in a fixed (alphabetical) order so two opposite
                // transfers running at once cannot deadlock each other.
                boolean fromFirst = request.fromAccountNumber.compareTo(request.toAccountNumber) < 0;
                String first = fromFirst ? request.fromAccountNumber : request.toAccountNumber;
                String second = fromFirst ? request.toAccountNumber : request.fromAccountNumber;
                BigDecimal firstBalance = findBalance(connection, first, true);
                BigDecimal secondBalance = findBalance(connection, second, true);
                BigDecimal fromBalance = fromFirst ? firstBalance : secondBalance;
                BigDecimal toBalance = fromFirst ? secondBalance : firstBalance;

                if (fromBalance == null) {
                    throw accountNotFound(regionType, request.fromAccountNumber);
                }
                if (toBalance == null) {
                    throw accountNotFound(regionType, request.toAccountNumber);
                }
                if (fromBalance.compareTo(request.amount) < 0) {
                    throw insufficientFunds(request.fromAccountNumber);
                }

                adjustBalance(connection, request.fromAccountNumber, request.amount.negate());
                adjustBalance(connection, request.toAccountNumber, request.amount);
                connection.commit();

                Map<String, Object> result = new HashMap<>();
                result.put("fromAccountNumber", request.fromAccountNumber);
                result.put("fromBalance", fromBalance.subtract(request.amount));
                result.put("toAccountNumber", request.toAccountNumber);
                result.put("toBalance", toBalance.add(request.amount));
                result.put("amount", request.amount);
                result.put("status", "transferred");
                return result;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private BigDecimal findBalance(Connection connection, String accountNumber, boolean lockRow) throws SQLException {
        String sql = "SELECT balance FROM account WHERE account_number = ?" + (lockRow ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getBigDecimal("balance") : null;
            }
        }
    }

    private void adjustBalance(Connection connection, String accountNumber, BigDecimal delta) throws SQLException {
        String sql = "UPDATE account SET balance = balance + ? WHERE account_number = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, delta);
            statement.setString(2, accountNumber);
            statement.executeUpdate();
        }
    }

    private Map<String, Object> balanceResult(String accountNumber, BigDecimal balance, String status) {
        Map<String, Object> result = new HashMap<>();
        result.put("accountNumber", accountNumber);
        result.put("balance", balance);
        result.put("status", status);
        return result;
    }

    private BadRequestException insufficientFunds(String accountNumber) {
        return new BadRequestException("Insufficient funds in account " + accountNumber);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("amount must be positive");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new BadRequestException("amount can have at most 2 decimal places");
        }
    }

    private void validate(TransferRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        if (request.fromAccountNumber == null || request.fromAccountNumber.isBlank()) {
            throw new BadRequestException("fromAccountNumber is required");
        }
        if (request.toAccountNumber == null || request.toAccountNumber.isBlank()) {
            throw new BadRequestException("toAccountNumber is required");
        }
        if (request.fromAccountNumber.equals(request.toAccountNumber)) {
            throw new BadRequestException("fromAccountNumber and toAccountNumber must differ");
        }
        validateAmount(request.amount);
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

    private NotFoundException accountNotFound(RegionType region, String accountNumber) {
        return new NotFoundException("Account " + accountNumber + " not found in " + region.name());
    }

    private boolean isValidAccountType(String accountType) {
        return "SAVINGS".equalsIgnoreCase(accountType) || "CURRENT".equalsIgnoreCase(accountType);
    }

    private void validate(UpdateAccountRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        if (request.accountHolderName == null || request.accountHolderName.isBlank()) {
            throw new BadRequestException("accountHolderName is required");
        }
        if (!isValidAccountType(request.accountType)) {
            throw new BadRequestException("accountType must be SAVINGS or CURRENT");
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
        if (!isValidAccountType(request.accountType)) {
            throw new BadRequestException("accountType must be SAVINGS or CURRENT");
        }
        if (request.balance == null || request.balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("balance must be zero or positive");
        }
        if (request.regionCode == null || request.regionCode.isBlank()) {
            throw new BadRequestException("regionCode is required");
        }
    }
}
