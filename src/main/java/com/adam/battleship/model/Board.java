package com.adam.battleship.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Board {
    public static final int SIZE = 10;

    private final Map<ShipType, Ship> ships = new HashMap<>();
    private final Map<Coordinate, Ship> shipAt = new HashMap<>();
    private final Set<Coordinate> shotsReceived = new HashSet<>();

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

    public ShotResult fireAt(Coordinate target) {
        if (!isInBounds(target)) {
            throw new IllegalArgumentException(target + " is off the board");
        }
        if (!shotsReceived.add(target)) {
            throw new IllegalStateException(target + " was already fired at");
        }

        Ship ship = shipAt.get(target);
        if (ship == null) {
            return ShotResult.MISS;
        }
        ship.hit(target);
        return ship.isSunk() ? ShotResult.SUNK : ShotResult.HIT;
    }

    public boolean hasShipAt(Coordinate c) {
        return shipAt.containsKey(c);
    }

    public boolean allShipsPlaced() {
        return ships.size() == ShipType.values().length;
    }

    public boolean allShipsSunk() {
        if (ships.isEmpty()) {
            return false;
        }
        for (Ship ship : ships.values()) {
            if (!ship.isSunk()) {
                return false;
            }
        }
        return true;
    }

    private boolean isInBounds(Coordinate c) {
        return c.row() >= 0 && c.row() < SIZE
                && c.col() >= 0 && c.col() < SIZE;
    }
}
