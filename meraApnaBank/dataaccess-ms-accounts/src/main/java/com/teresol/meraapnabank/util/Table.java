package com.teresol.meraapnabank.util;

public class Table {

    private final String name;
    private final String[] columns;

    public Table(String name, String... columns) {
        this.name = name;
        this.columns = columns;
    }

    public String getName() {
        return name;
    }

    public String[] getColumns() {
        return columns;
    }
}
