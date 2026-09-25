package com.teresol.meraapnabank.util;

public class Selection {

    private final String column;
    private final Object value;

    public Selection(String column, Object value) {
        this.column = column;
        this.value = value;
    }

    public String getColumn() {
        return column;
    }

    public Object getValue() {
        return value;
    }
}
