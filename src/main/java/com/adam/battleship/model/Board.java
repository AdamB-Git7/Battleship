package com.adam.battleship.model;

import java.util.HashMap;
import java.util.Map;

public class Board {
    public static final int SIZE = 10;

    private final Map<ShipType, Ship> ships = new HashMap<>();
    private final Map<Coordinate, Ship> shipAt = new HashMap<>();

    public void placeShip(ShipType type, Coordinate start, Orientation orientation) {
        if (ships.containsKey(type)) {
            throw new IllegalStateException(type + " is already placed");
        }

        Ship ship = new Ship(type, start, orientation);

        for (Coordinate c : ship.getPositions()) {
            if (!isInBounds(c)) {
                throw new IllegalArgumentException(type + " goes off the board");
            }
            if (shipAt.containsKey(c)) {
                throw new IllegalArgumentException(type + " overlaps another ship");
            }
        }

        ships.put(type, ship);
        for (Coordinate c : ship.getPositions()) {
            shipAt.put(c, ship);
        }
    }

    public boolean hasShipAt(Coordinate c) {
        return shipAt.containsKey(c);
    }

    public boolean allShipsPlaced() {
        return ships.size() == ShipType.values().length;
    }

    private boolean isInBounds(Coordinate c) {
        return c.row() >= 0 && c.row() < SIZE
                && c.col() >= 0 && c.col() < SIZE;
    }
}