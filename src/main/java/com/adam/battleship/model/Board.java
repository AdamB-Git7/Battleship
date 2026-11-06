package com.adam.battleship.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

    public List<List<CellView>> view(boolean showShips) {
        List<List<CellView>> rows = new ArrayList<>();
        for (int r = 0; r < SIZE; r++) {
            List<CellView> row = new ArrayList<>();
            for (int c = 0; c < SIZE; c++) {
                row.add(cellAt(new Coordinate(r, c), showShips));
            }
            rows.add(List.copyOf(row));
        }
        return List.copyOf(rows);
    }

    public List<ShipType> unplacedShips() {
        List<ShipType> missing = new ArrayList<>();
        for (ShipType type : ShipType.values()) {
            if (!ships.containsKey(type)) {
                missing.add(type);
            }
        }
        return List.copyOf(missing);
    }

    private CellView cellAt(Coordinate c, boolean showShips) {
        Ship ship = shipAt.get(c);
        boolean shot = shotsReceived.contains(c);
        if (shot && ship == null) {
            return CellView.MISS;
        }
        if (shot) {
            return ship.isSunk() ? CellView.SUNK : CellView.HIT;
        }
        if (ship != null && showShips) {
            return CellView.SHIP;
        }
        return CellView.EMPTY;
    }

    private boolean isInBounds(Coordinate c) {
        return c.row() >= 0 && c.row() < SIZE
                && c.col() >= 0 && c.col() < SIZE;
    }
}
