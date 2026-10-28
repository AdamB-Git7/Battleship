package com.adam.battleship.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShipTest {
    @Test
    void horizontalShipGrowsToTheRight() {
        Ship ship = new Ship(ShipType.CRUISER, new Coordinate(2, 3), Orientation.HORIZONTAL);

        assertEquals(List.of(
                new Coordinate(2, 3),
                new Coordinate(2, 4),
                new Coordinate(2, 5)), ship.getPositions());
    }

    @Test
    void verticalShipGrowsDownward() {
        Ship ship = new Ship(ShipType.DESTROYER, new Coordinate(5, 0), Orientation.VERTICAL);

        assertEquals(List.of(
                new Coordinate(5, 0),
                new Coordinate(6, 0)), ship.getPositions());
    }

    @Test
    void shipCoversExactlyItsLength() {
        for (ShipType type : ShipType.values()) {
            Ship ship = new Ship(type, new Coordinate(0, 0), Orientation.HORIZONTAL);

            assertEquals(type.getLength(), ship.getPositions().size(), type.name());
        }
    }

    @Test
    void positionsCannotBeChangedFromOutside() {
        Ship ship = new Ship(ShipType.DESTROYER, new Coordinate(0, 0), Orientation.HORIZONTAL);

        assertThrows(UnsupportedOperationException.class,
                () -> ship.getPositions().add(new Coordinate(9, 9)));
    }

    @Test
    void shipTypesHaveClassicLengths() {
        assertEquals(5, ShipType.CARRIER.getLength());
        assertEquals(4, ShipType.BATTLESHIP.getLength());
        assertEquals(3, ShipType.CRUISER.getLength());
        assertEquals(3, ShipType.SUBMARINE.getLength());
        assertEquals(2, ShipType.DESTROYER.getLength());
    }

    @Test
    void coordinatesWithSameRowAndColAreEqual() {
        assertEquals(new Coordinate(4, 7), new Coordinate(4, 7));
        assertEquals(new Coordinate(4, 7).hashCode(), new Coordinate(4, 7).hashCode());
    }
}
