package com.teresol.meraapnabank.util;

public final class Tables {

    private Tables() {
    }

    public static final Table BRANCH = new Table("branch",
            "branch_code", "branch_name", "city", "region_code");

    public static final Table ACCOUNT = new Table("account",
            "account_number", "account_holder_name", "branch_code", "account_type", "balance", "region_code");
}
