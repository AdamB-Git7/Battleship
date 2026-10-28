package com.adam.battleship.game;

import com.adam.battleship.model.GameStatus;

public class Game {
    private final String code;
    private final String player1;
    private String player2;
    private GameStatus status;

    public Game(String code, String player1) {
        this.code = code;
        this.player1 = player1;
        this.status = GameStatus.WAITING_FOR_PLAYER;
    }

    public String getCode() { return code; }
    public String getPlayer1() { return player1; }
    public String getPlayer2() { return player2; }
    public GameStatus getStatus() { return status; }

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
}