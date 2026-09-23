package com.teresol.meraapnabank;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;

@ApplicationScoped
public class Datasources {

    @Inject
    @DataSource("north")
    AgroalDataSource northDataSource;

    @Inject
    @DataSource("south")
    AgroalDataSource southDataSource;

    @Inject
    @DataSource("east")
    AgroalDataSource eastDataSource;

    public Connection getConnection(RegionType region) throws SQLException {
        return switch (region) {
            case NORTH -> northDataSource.getConnection();
            case SOUTH -> southDataSource.getConnection();
            case EAST -> eastDataSource.getConnection();
        };
    }
}
