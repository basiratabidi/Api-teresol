package com.teresol.meraapnabank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;

public class RegionTypeTest {

    @Test
    void parsesKnownRegionsCaseInsensitively() {
        assertEquals(RegionType.NORTH, RegionType.parse("north"));
        assertEquals(RegionType.SOUTH, RegionType.parse("South"));
        assertEquals(RegionType.EAST, RegionType.parse("EAST"));
        assertEquals(RegionType.WEST, RegionType.parse("west"));
    }

    @Test
    void rejectsBlankOrNullRegion() {
        assertThrows(BadRequestException.class, () -> RegionType.parse(" "));
        assertThrows(BadRequestException.class, () -> RegionType.parse(null));
    }

    @Test
    void rejectsUnknownRegion() {
        assertThrows(BadRequestException.class, () -> RegionType.parse("ATLANTIS"));
    }

    @Test
    void exposesCodePrefixPerRegion() {
        assertEquals("NO", RegionType.NORTH.codePrefix());
        assertEquals("SO", RegionType.SOUTH.codePrefix());
        assertEquals("EA", RegionType.EAST.codePrefix());
        assertEquals("WE", RegionType.WEST.codePrefix());
    }
}
