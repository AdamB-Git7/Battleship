package com.adam.battleship.dto;

import com.adam.battleship.game.Game;
import com.adam.battleship.model.GameStatus;

public record RoomUpdate(String code, GameStatus status, int players) {
    public static RoomUpdate from(Game game) {
        int players = game.getPlayer2() == null ? 1 : 2;
        return new RoomUpdate(game.getCode(), game.getStatus(), players);
    }
}
