package com.adam.battleship.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardTest {
    private final Board board = new Board();

    @Test
    void shotAtWaterIsAMiss() {
        board.placeShip(ShipType.DESTROYER, new Coordinate(0, 0), Orientation.HORIZONTAL);

        assertEquals(ShotResult.MISS, board.fireAt(new Coordinate(5, 5)));
    }

    @Test
    void shotAtAShipIsAHit() {
        board.placeShip(ShipType.CRUISER, new Coordinate(0, 0), Orientation.HORIZONTAL);

        assertEquals(ShotResult.HIT, board.fireAt(new Coordinate(0, 1)));
    }

    @Test
    void lastSquareOfAShipSinksIt() {
        board.placeShip(ShipType.DESTROYER, new Coordinate(3, 3), Orientation.VERTICAL);

        assertEquals(ShotResult.HIT, board.fireAt(new Coordinate(3, 3)));
        assertEquals(ShotResult.SUNK, board.fireAt(new Coordinate(4, 3)));
    }

    @Test
    void firingAtTheSameShipSquareTwiceIsRejected() {
        board.placeShip(ShipType.DESTROYER, new Coordinate(0, 0), Orientation.HORIZONTAL);
        board.fireAt(new Coordinate(0, 0));

        assertThrows(IllegalStateException.class, () -> board.fireAt(new Coordinate(0, 0)));
    }

    @Test
    void firingAtTheSameWaterSquareTwiceIsRejected() {
        board.placeShip(ShipType.DESTROYER, new Coordinate(0, 0), Orientation.HORIZONTAL);
        board.fireAt(new Coordinate(7, 7));

        assertThrows(IllegalStateException.class, () -> board.fireAt(new Coordinate(7, 7)));
    }

    @Test
    void shotOffTheBoardIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> board.fireAt(new Coordinate(10, 0)));
        assertThrows(IllegalArgumentException.class, () -> board.fireAt(new Coordinate(0, -1)));
    }

    @Test
    void refusedOffBoardShotIsNotRecorded() {
        assertThrows(IllegalArgumentException.class, () -> board.fireAt(new Coordinate(10, 0)));

        assertThrows(IllegalArgumentException.class, () -> board.fireAt(new Coordinate(10, 0)));
    }

    @Test
    void shotsAtTheEdgeSquaresAreAllowed() {
        assertEquals(ShotResult.MISS, board.fireAt(new Coordinate(0, 0)));
        assertEquals(ShotResult.MISS, board.fireAt(new Coordinate(9, 9)));
    }

    @Test
    void emptyBoardDoesNotCountAsAllSunk() {
        assertFalse(board.allShipsSunk());
    }

    @Test
    void allShipsSunkOnlyWhenEveryShipIsSunk() {
        board.placeShip(ShipType.DESTROYER, new Coordinate(0, 0), Orientation.HORIZONTAL);
        board.placeShip(ShipType.CRUISER, new Coordinate(2, 0), Orientation.HORIZONTAL);

        board.fireAt(new Coordinate(0, 0));
        board.fireAt(new Coordinate(0, 1));
        assertFalse(board.allShipsSunk());

        board.fireAt(new Coordinate(2, 0));
        board.fireAt(new Coordinate(2, 1));
        board.fireAt(new Coordinate(2, 2));
        assertTrue(board.allShipsSunk());
    }

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

    @Test
    void viewIsTenRowsOfTenSquares() {
        List<List<CellView>> view = board.view(true);

        assertEquals(Board.SIZE, view.size());
        for (List<CellView> row : view) {
            assertEquals(Board.SIZE, row.size());
        }
    }

    @Test
    void emptyBoardViewIsAllEmpty() {
        for (List<CellView> row : board.view(true)) {
            for (CellView cell : row) {
                assertEquals(CellView.EMPTY, cell);
            }
        }
    }

    @Test
    void ownerSeesTheirShips() {
        board.placeShip(ShipType.DESTROYER, new Coordinate(2, 3), Orientation.HORIZONTAL);

        List<List<CellView>> view = board.view(true);

        assertEquals(CellView.SHIP, view.get(2).get(3));
        assertEquals(CellView.SHIP, view.get(2).get(4));
        assertEquals(CellView.EMPTY, view.get(2).get(5));
        assertEquals(CellView.EMPTY, view.get(3).get(3));
    }

    @Test
    void opponentNeverSeesUnhitShips() {
        for (ShipType type : ShipType.values()) {
            board.placeShip(type, new Coordinate(type.ordinal(), 0), Orientation.HORIZONTAL);
        }
        board.fireAt(new Coordinate(0, 0));

        for (List<CellView> row : board.view(false)) {
            for (CellView cell : row) {
                assertTrue(cell != CellView.SHIP, "opponent view leaked a ship");
            }
        }
    }

    @Test
    void shotsShowAsMissHitAndSunkForBothViewers() {
        board.placeShip(ShipType.DESTROYER, new Coordinate(0, 0), Orientation.HORIZONTAL);
        board.placeShip(ShipType.CRUISER, new Coordinate(5, 5), Orientation.VERTICAL);
        board.fireAt(new Coordinate(9, 9));
        board.fireAt(new Coordinate(5, 5));
        board.fireAt(new Coordinate(0, 0));
        board.fireAt(new Coordinate(0, 1));

        for (boolean showShips : new boolean[] {true, false}) {
            List<List<CellView>> view = board.view(showShips);
            assertEquals(CellView.MISS, view.get(9).get(9));
            assertEquals(CellView.HIT, view.get(5).get(5));
            assertEquals(CellView.SUNK, view.get(0).get(0));
            assertEquals(CellView.SUNK, view.get(0).get(1));
        }
    }

    @Test
    void ownerSeesUnhitPartOfAHitShipButOpponentDoesNot() {
        board.placeShip(ShipType.CRUISER, new Coordinate(5, 5), Orientation.VERTICAL);
        board.fireAt(new Coordinate(5, 5));

        assertEquals(CellView.SHIP, board.view(true).get(6).get(5));
        assertEquals(CellView.EMPTY, board.view(false).get(6).get(5));
    }

    @Test
    void viewCannotBeChangedFromOutside() {
        List<List<CellView>> view = board.view(true);

        assertThrows(UnsupportedOperationException.class, () -> view.get(0).set(0, CellView.SHIP));
        assertThrows(UnsupportedOperationException.class, () -> view.add(List.of()));
    }

    @Test
    void unplacedShipsListsMissingTypesBiggestFirst() {
        assertEquals(List.of(ShipType.values()), board.unplacedShips());

        board.placeShip(ShipType.CARRIER, new Coordinate(0, 0), Orientation.HORIZONTAL);
        board.placeShip(ShipType.SUBMARINE, new Coordinate(1, 0), Orientation.HORIZONTAL);

        assertEquals(List.of(ShipType.BATTLESHIP, ShipType.CRUISER, ShipType.DESTROYER),
                board.unplacedShips());
    }

    @Test
    void unplacedShipsIsEmptyWhenAllArePlaced() {
        for (ShipType type : ShipType.values()) {
            board.placeShip(type, new Coordinate(type.ordinal(), 0), Orientation.HORIZONTAL);
        }

        assertTrue(board.unplacedShips().isEmpty());
    }
}
