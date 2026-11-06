package com.adam.battleship.controller;

import com.adam.battleship.dto.JoinRequest;
import com.adam.battleship.dto.RoomUpdate;
import com.adam.battleship.game.Game;
import com.adam.battleship.game.GameRoomManager;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class RoomController {
    private final GameRoomManager rooms;
    private final GameUpdates updates;
    private final PlayerExits exits;

    public RoomController(GameRoomManager rooms, GameUpdates updates, PlayerExits exits) {
        this.rooms = rooms;
        this.updates = updates;
        this.exits = exits;
    }

    @MessageMapping("/create")
    @SendToUser("/queue/room")
    public RoomUpdate create(Principal player) {
        requireNotInARoom(player.getName());
        Game game = rooms.createRoom(player.getName());
        return RoomUpdate.from(game);
    }

    @MessageMapping("/join")
    public void join(JoinRequest request, Principal player) {
        requireNotInARoom(player.getName());
        Game game = rooms.getRoom(RoomCodes.normalize(request.code()));
        synchronized (game) {
            game.join(player.getName());
            updates.sendRoom(game);
            updates.sendViews(game);
        }
    }

    @MessageMapping("/leave")
    public void leave(Principal player) {
        exits.leave(player.getName());
    }

    private void requireNotInARoom(String playerId) {
        if (rooms.findRoomOf(playerId).isPresent()) {
            throw new IllegalStateException("You are already in a room");
        }
    }
}
