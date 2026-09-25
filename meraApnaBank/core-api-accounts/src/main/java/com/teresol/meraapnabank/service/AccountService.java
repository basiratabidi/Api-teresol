package com.teresol.meraapnabank.service;

import com.teresol.meraapnabank.client.AccountClient;
import com.teresol.meraapnabank.dto.AccountDto;
import com.teresol.meraapnabank.dto.NewAccountRequest;
import com.teresol.meraapnabank.dto.UpdateAccountRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class AccountService {

    @Inject
    @RestClient
    AccountClient accountClient;

    public List<AccountDto> getAccounts(String region) {
        return accountClient.getAccounts(region);
    }

    public Map<String, Object> createAccount(NewAccountRequest request) {
        return accountClient.createAccount(request);
    }

    public Map<String, Object> updateAccount(String region, String accountNumber, UpdateAccountRequest request) {
        return accountClient.updateAccount(region, accountNumber, request);
    }

    public Map<String, Object> deleteAccount(String region, String accountNumber) {
        return accountClient.deleteAccount(region, accountNumber);
    }
}
