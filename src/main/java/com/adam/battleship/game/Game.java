package com.adam.battleship.game;

import com.adam.battleship.model.Board;
import com.adam.battleship.model.Coordinate;
import com.adam.battleship.model.GameStatus;
import com.adam.battleship.model.Orientation;
import com.adam.battleship.model.ShipType;
import com.adam.battleship.model.ShotResult;

public class Game {
    private final String code;
    private final String player1;
    private final Board board1 = new Board();
    private final Board board2 = new Board();
    private String player2;
    private GameStatus status;
    private String currentTurn;
    private String winner;

    public Game(String code, String player1) {
        this.code = code;
        this.player1 = player1;
        this.status = GameStatus.WAITING_FOR_PLAYER;
    }

    public String getCode() { return code; }
    public String getPlayer1() { return player1; }
    public synchronized String getPlayer2() { return player2; }
    public synchronized GameStatus getStatus() { return status; }
    public synchronized String getCurrentTurn() { return currentTurn; }
    public synchronized String getWinner() { return winner; }

    public synchronized void join(String playerId) {
        if (player2 != null) {
            throw new IllegalStateException("Room is full");
        }
        if (player1.equals(playerId)) {
            throw new IllegalStateException("You are already in this room");
        }
        player2 = playerId;
        status = GameStatus.PLACING_SHIPS;
    }

    public synchronized void placeShip(String playerId, ShipType type, Coordinate start, Orientation orientation) {
        if (status != GameStatus.PLACING_SHIPS) {
            throw new IllegalStateException("Ships can't be placed right now");
        }
        boardOf(playerId).placeShip(type, start, orientation);
        if (board1.allShipsPlaced() && board2.allShipsPlaced()) {
            status = GameStatus.IN_PROGRESS;
            currentTurn = player1;
        }
    }

    public synchronized ShotResult fire(String playerId, Coordinate target) {
        if (status != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("The game is not in progress");
        }
        if (!currentTurn.equals(playerId)) {
            throw new IllegalStateException("It's not your turn");
        }

        String opponent = opponentOf(playerId);
        Board targetBoard = boardOf(opponent);
        ShotResult result = targetBoard.fireAt(target);

        if (targetBoard.allShipsSunk()) {
            status = GameStatus.FINISHED;
            winner = playerId;
        } else {
            currentTurn = opponent;
        }
        return result;
    }

    private Board boardOf(String playerId) {
        if (player1.equals(playerId)) {
            return board1;
        }
        if (player2.equals(playerId)) {
            return board2;
        }
        throw new IllegalArgumentException(playerId + " is not in this game");
    }

    private String opponentOf(String playerId) {
        return player1.equals(playerId) ? player2 : player1;
    }
}