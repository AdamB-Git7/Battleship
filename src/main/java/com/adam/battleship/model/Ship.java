package com.adam.battleship.model;

import java.util.ArrayList;
import java.util.List;

public class Ship {
    private final ShipType type;
    private final List<Coordinate> positions;

    public Ship(ShipType type, Coordinate start, Orientation orientation) {
        this.type = type;
        List<Coordinate> cells = new ArrayList<>();
        for (int i = 0; i < type.getLength(); i++) {
            if (orientation == Orientation.HORIZONTAL) {
                cells.add(new Coordinate(start.row(), start.col() + i));
            } else {
                cells.add(new Coordinate(start.row() + i, start.col()));
            }
        }
        this.positions = List.copyOf(cells);
    }

    public ShipType getType() {
        return type;
    }

    public List<Coordinate> getPositions() {
        return positions;
    }
}