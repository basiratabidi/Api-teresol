package com.teresol.meraapnabank.client;

import com.teresol.meraapnabank.dto.AccountDto;
import com.teresol.meraapnabank.dto.NewAccountRequest;
import com.teresol.meraapnabank.dto.UpdateAccountRequest;
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
@Path("/accounts")
public interface AccountClient {

    @GET
    @Path("/{region}")
    @Produces(MediaType.APPLICATION_JSON)
    List<AccountDto> getAccounts(@PathParam("region") String region);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> createAccount(NewAccountRequest request);

    @PUT
    @Path("/{region}/{accountNumber}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> updateAccount(@PathParam("region") String region,
            @PathParam("accountNumber") String accountNumber, UpdateAccountRequest request);

    @DELETE
    @Path("/{region}/{accountNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> deleteAccount(@PathParam("region") String region,
            @PathParam("accountNumber") String accountNumber);
}
