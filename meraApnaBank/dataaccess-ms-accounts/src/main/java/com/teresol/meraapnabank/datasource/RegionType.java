package com.teresol.meraapnabank.datasource;

import jakarta.ws.rs.BadRequestException;

public enum RegionType {
    NORTH,
    SOUTH,
    EAST,
    WEST;

    public static RegionType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("region is required");
        }
        try {
            return RegionType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Unknown region '" + value + "'. Valid regions: NORTH, SOUTH, EAST, WEST");
        }
    }
}
