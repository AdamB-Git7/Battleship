package com.adam.battleship.model;

public record ShipLayout(ShipType type, int row, int col, Orientation orientation, boolean sunk) {
}
