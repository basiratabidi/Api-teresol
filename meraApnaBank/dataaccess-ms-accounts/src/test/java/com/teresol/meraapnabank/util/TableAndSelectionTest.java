package com.teresol.meraapnabank.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TableAndSelectionTest {

    @Test
    void tableExposesNameAndColumns() {
        Table table = new Table("branch", "branch_code", "city");

        assertEquals("branch", table.getName());
        assertArrayEquals(new String[] { "branch_code", "city" }, table.getColumns());
    }

    @Test
    void selectionExposesColumnAndValue() {
        Selection selection = new Selection("branch_code", "NO-001");

        assertEquals("branch_code", selection.getColumn());
        assertEquals("NO-001", selection.getValue());
    }

    @Test
    void tablesDefinesBranchAndAccountTables() {
        assertEquals("branch", Tables.BRANCH.getName());
        assertEquals("account", Tables.ACCOUNT.getName());
    }
}
