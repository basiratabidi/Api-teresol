package com.teresol.meraapnabank.dto;

import java.math.BigDecimal;

public class NewAccountRequest {
    public String accountNumber;
    public String accountHolderName;
    public String branchCode;
    public String accountType;
    public BigDecimal balance;
    public String regionCode;
}
