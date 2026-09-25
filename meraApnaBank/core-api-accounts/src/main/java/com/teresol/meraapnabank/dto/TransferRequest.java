package com.teresol.meraapnabank.dto;

import java.math.BigDecimal;

public class TransferRequest {
    public String fromAccountNumber;
    public String toAccountNumber;
    public BigDecimal amount;
}
