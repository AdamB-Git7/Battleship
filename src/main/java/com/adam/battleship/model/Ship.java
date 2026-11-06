package com.adam.battleship.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Ship {
    private final ShipType type;
    private final Orientation orientation;
    private final List<Coordinate> positions;
    private final Set<Coordinate> hits = new HashSet<>();

    public Ship(ShipType type, Coordinate start, Orientation orientation) {
        this.type = type;
        this.orientation = orientation;
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

    public Coordinate getStart() {
        return positions.get(0);
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void hit(Coordinate c) {
        if (!positions.contains(c)) {
            throw new IllegalArgumentException(c + " is not part of this " + type);
        }
        hits.add(c);
    }

    public boolean isSunk() {
        return hits.size() == positions.size();
    }
}