package com.teresol.meraapnabank.service;

import com.teresol.meraapnabank.client.BranchClient;
import com.teresol.meraapnabank.dto.BranchDto;
import com.teresol.meraapnabank.dto.NewBranchRequest;
import com.teresol.meraapnabank.dto.UpdateBranchRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class BranchService {

    @Inject
    @RestClient
    BranchClient branchClient;

    public List<BranchDto> getBranches(String region) {
        return branchClient.getBranches(region);
    }

    public Map<String, Object> createBranch(NewBranchRequest request) {
        return branchClient.createBranch(request);
    }

    public Map<String, Object> updateBranch(String region, String branchCode, UpdateBranchRequest request) {
        return branchClient.updateBranch(region, branchCode, request);
    }

    public Map<String, Object> deleteBranch(String region, String branchCode) {
        return branchClient.deleteBranch(region, branchCode);
    }
}
