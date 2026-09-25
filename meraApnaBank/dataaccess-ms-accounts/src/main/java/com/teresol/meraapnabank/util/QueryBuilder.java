package com.teresol.meraapnabank.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class QueryBuilder {

    private QueryBuilder() {
    }

    public static PreparedStatement buildSelect(Connection connection, Table table, List<Selection> whereClauses)
            throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT ")
                .append(String.join(", ", table.getColumns()))
                .append(" FROM ").append(table.getName());
        appendWhere(sql, whereClauses);

        PreparedStatement statement = connection.prepareStatement(sql.toString());
        bindParameters(statement, whereClauses);
        return statement;
    }

    public static PreparedStatement buildInsert(Connection connection, Table table, List<Selection> values)
            throws SQLException {
        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        for (Selection value : values) {
            columns.add(value.getColumn());
            placeholders.add("?");
        }

        String sql = "INSERT INTO " + table.getName()
                + " (" + String.join(", ", columns) + ") VALUES (" + String.join(", ", placeholders) + ")";

        PreparedStatement statement = connection.prepareStatement(sql);
        bindParameters(statement, values);
        return statement;
    }

    public static PreparedStatement buildUpdate(Connection connection, Table table, List<Selection> values,
            List<Selection> whereClauses) throws SQLException {
        List<String> assignments = new ArrayList<>();
        for (Selection value : values) {
            assignments.add(value.getColumn() + " = ?");
        }

        StringBuilder sql = new StringBuilder("UPDATE ").append(table.getName())
                .append(" SET ").append(String.join(", ", assignments));
        appendWhere(sql, whereClauses);

        PreparedStatement statement = connection.prepareStatement(sql.toString());
        List<Selection> allParams = new ArrayList<>(values);
        allParams.addAll(whereClauses);
        bindParameters(statement, allParams);
        return statement;
    }

    public static PreparedStatement buildDelete(Connection connection, Table table, List<Selection> whereClauses)
            throws SQLException {
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(table.getName());
        appendWhere(sql, whereClauses);

        PreparedStatement statement = connection.prepareStatement(sql.toString());
        bindParameters(statement, whereClauses);
        return statement;
    }

    private static void appendWhere(StringBuilder sql, List<Selection> whereClauses) {
        if (whereClauses != null && !whereClauses.isEmpty()) {
            List<String> conditions = new ArrayList<>();
            for (Selection clause : whereClauses) {
                conditions.add(clause.getColumn() + " = ?");
            }
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
    }

    private static void bindParameters(PreparedStatement statement, List<Selection> params) throws SQLException {
        int index = 1;
        for (Selection param : params) {
            statement.setObject(index++, param.getValue());
        }
    }
}
