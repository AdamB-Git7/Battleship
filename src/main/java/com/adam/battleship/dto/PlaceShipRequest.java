package com.adam.battleship.dto;

import com.adam.battleship.model.Orientation;
import com.adam.battleship.model.ShipType;

public record PlaceShipRequest(String code, ShipType type, int row, int col, Orientation orientation) {
}
