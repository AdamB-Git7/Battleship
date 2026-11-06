package com.adam.battleship.game;

import com.adam.battleship.model.CellView;
import com.adam.battleship.model.GameStatus;
import com.adam.battleship.model.ShipLayout;
import com.adam.battleship.model.ShipType;

import java.util.List;

public record PlayerView(
        String code,
        GameStatus status,
        boolean yourTurn,
        boolean youWon,
        List<ShipType> shipsToPlace,
        List<List<CellView>> yourBoard,
        List<List<CellView>> opponentBoard,
        boolean rematchRequestedByYou,
        boolean rematchRequestedByOpponent,
        List<ShipLayout> yourShips,
        List<ShipLayout> opponentShips) {
}
