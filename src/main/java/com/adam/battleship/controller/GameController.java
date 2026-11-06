package com.adam.battleship.controller;

import com.adam.battleship.dto.FireRequest;
import com.adam.battleship.dto.PlaceShipRequest;
import com.adam.battleship.game.Game;
import com.adam.battleship.game.GameRoomManager;
import com.adam.battleship.model.Coordinate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class GameController {
    private final GameRoomManager rooms;
    private final GameUpdates updates;

    public GameController(GameRoomManager rooms, GameUpdates updates) {
        this.rooms = rooms;
        this.updates = updates;
    }

    @MessageMapping("/place")
    public void place(PlaceShipRequest request, Principal player) {
        if (request.type() == null || request.orientation() == null) {
            throw new IllegalArgumentException("Choose a ship and a direction");
        }
        Game game = rooms.getRoom(RoomCodes.normalize(request.code()));
        synchronized (game) {
            game.placeShip(player.getName(), request.type(),
                    new Coordinate(request.row(), request.col()), request.orientation());
            updates.sendViews(game);
        }
    }

    @MessageMapping("/rematch")
    public void rematch(Principal player) {
        Game game = rooms.findRoomOf(player.getName())
                .orElseThrow(() -> new IllegalStateException("You are not in a room"));
        synchronized (game) {
            game.requestRematch(player.getName());
            updates.sendViews(game);
        }
    }

    @MessageMapping("/fire")
    public void fire(FireRequest request, Principal player) {
        Game game = rooms.getRoom(RoomCodes.normalize(request.code()));
        synchronized (game) {
            game.fire(player.getName(), new Coordinate(request.row(), request.col()));
            updates.sendViews(game);
        }
    }
}
