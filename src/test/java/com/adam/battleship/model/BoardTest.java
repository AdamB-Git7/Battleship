package com.adam.battleship.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardTest {
    private final Board board = new Board();

    @Test
    void placedShipOccupiesItsSquares() {
        board.placeShip(ShipType.CRUISER, new Coordinate(2, 3), Orientation.HORIZONTAL);

        assertTrue(board.hasShipAt(new Coordinate(2, 3)));
        assertTrue(board.hasShipAt(new Coordinate(2, 4)));
        assertTrue(board.hasShipAt(new Coordinate(2, 5)));
        assertFalse(board.hasShipAt(new Coordinate(2, 6)));
        assertFalse(board.hasShipAt(new Coordinate(3, 3)));
    }

    @Test
    void shipCanTouchTheLastRowAndColumn() {
        assertDoesNotThrow(() ->
                board.placeShip(ShipType.CARRIER, new Coordinate(9, 5), Orientation.HORIZONTAL));
        assertDoesNotThrow(() ->
                board.placeShip(ShipType.DESTROYER, new Coordinate(0, 9), Orientation.VERTICAL));

        assertTrue(board.hasShipAt(new Coordinate(9, 9)));
        assertTrue(board.hasShipAt(new Coordinate(1, 9)));
    }

    @Test
    void shipStickingOffTheRightEdgeIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                board.placeShip(ShipType.CARRIER, new Coordinate(0, 6), Orientation.HORIZONTAL));
    }

    @Test
    void shipStickingOffTheBottomEdgeIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                board.placeShip(ShipType.BATTLESHIP, new Coordinate(7, 0), Orientation.VERTICAL));
    }

    @Test
    void negativeStartIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                board.placeShip(ShipType.DESTROYER, new Coordinate(-1, 0), Orientation.HORIZONTAL));
        assertThrows(IllegalArgumentException.class, () ->
                board.placeShip(ShipType.DESTROYER, new Coordinate(0, -1), Orientation.HORIZONTAL));
    }

    @Test
    void overlappingShipIsRejected() {
        board.placeShip(ShipType.CRUISER, new Coordinate(2, 3), Orientation.HORIZONTAL);

        assertThrows(IllegalArgumentException.class, () ->
                board.placeShip(ShipType.SUBMARINE, new Coordinate(0, 4), Orientation.VERTICAL));
    }

    @Test
    void sameShipTypeCannotBePlacedTwice() {
        board.placeShip(ShipType.DESTROYER, new Coordinate(0, 0), Orientation.HORIZONTAL);

        assertThrows(IllegalStateException.class, () ->
                board.placeShip(ShipType.DESTROYER, new Coordinate(5, 5), Orientation.HORIZONTAL));
        assertFalse(board.hasShipAt(new Coordinate(5, 5)));
    }

    @Test
    void rejectedShipLeavesNoSquaresBehind() {
        board.placeShip(ShipType.DESTROYER, new Coordinate(4, 2), Orientation.HORIZONTAL);

        assertThrows(IllegalArgumentException.class, () ->
                board.placeShip(ShipType.CRUISER, new Coordinate(2, 2), Orientation.VERTICAL));

        assertFalse(board.hasShipAt(new Coordinate(2, 2)));
        assertFalse(board.hasShipAt(new Coordinate(3, 2)));
    }

    @Test
    void rejectedShipCanBePlacedAgainSomewhereValid() {
        assertThrows(IllegalArgumentException.class, () ->
                board.placeShip(ShipType.CARRIER, new Coordinate(0, 8), Orientation.HORIZONTAL));

        assertDoesNotThrow(() ->
                board.placeShip(ShipType.CARRIER, new Coordinate(0, 0), Orientation.HORIZONTAL));
    }

    @Test
    void allShipsPlacedOnlyAfterAllFive() {
        board.placeShip(ShipType.CARRIER, new Coordinate(0, 0), Orientation.HORIZONTAL);
        board.placeShip(ShipType.BATTLESHIP, new Coordinate(1, 0), Orientation.HORIZONTAL);
        board.placeShip(ShipType.CRUISER, new Coordinate(2, 0), Orientation.HORIZONTAL);
        board.placeShip(ShipType.SUBMARINE, new Coordinate(3, 0), Orientation.HORIZONTAL);
        assertFalse(board.allShipsPlaced());

        board.placeShip(ShipType.DESTROYER, new Coordinate(4, 0), Orientation.HORIZONTAL);

        assertTrue(board.allShipsPlaced());
    }

    @Test
    void emptyBoardHasNoShips() {
        assertFalse(board.allShipsPlaced());
        assertFalse(board.hasShipAt(new Coordinate(0, 0)));
    }
}
