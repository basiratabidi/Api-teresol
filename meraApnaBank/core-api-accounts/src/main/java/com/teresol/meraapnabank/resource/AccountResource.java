package com.teresol.meraapnabank.resource;

import com.teresol.meraapnabank.dto.AccountDto;
import com.teresol.meraapnabank.dto.AmountRequest;
import com.teresol.meraapnabank.dto.NewAccountRequest;
import com.teresol.meraapnabank.dto.TransferRequest;
import com.teresol.meraapnabank.dto.UpdateAccountRequest;
import com.teresol.meraapnabank.service.AccountService;
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

@Path("/accounts")
public class AccountResource {

    @Inject
    AccountService accountService;

    @GET
    @Path("/{region}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AccountDto> getAccounts(@PathParam("region") String region) {
        return accountService.getAccounts(region);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> createAccount(NewAccountRequest request) {
        return accountService.createAccount(request);
    }

    @PUT
    @Path("/{region}/{accountNumber}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> updateAccount(@PathParam("region") String region,
            @PathParam("accountNumber") String accountNumber, UpdateAccountRequest request) {
        return accountService.updateAccount(region, accountNumber, request);
    }

    @DELETE
    @Path("/{region}/{accountNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> deleteAccount(@PathParam("region") String region,
            @PathParam("accountNumber") String accountNumber) {
        return accountService.deleteAccount(region, accountNumber);
    }

    @POST
    @Path("/{region}/{accountNumber}/deposit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> deposit(@PathParam("region") String region,
            @PathParam("accountNumber") String accountNumber, AmountRequest request) {
        return accountService.deposit(region, accountNumber, request);
    }

    @POST
    @Path("/{region}/{accountNumber}/withdraw")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> withdraw(@PathParam("region") String region,
            @PathParam("accountNumber") String accountNumber, AmountRequest request) {
        return accountService.withdraw(region, accountNumber, request);
    }

    @POST
    @Path("/{region}/transfer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> transfer(@PathParam("region") String region, TransferRequest request) {
        return accountService.transfer(region, request);
    }
}
