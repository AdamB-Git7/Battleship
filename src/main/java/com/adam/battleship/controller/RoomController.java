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

    public RoomController(GameRoomManager rooms, GameUpdates updates) {
        this.rooms = rooms;
        this.updates = updates;
    }

    @MessageMapping("/create")
    @SendToUser("/queue/room")
    public RoomUpdate create(Principal player) {
        Game game = rooms.createRoom(player.getName());
        return RoomUpdate.from(game);
    }

    @MessageMapping("/join")
    public void join(JoinRequest request, Principal player) {
        Game game = rooms.getRoom(RoomCodes.normalize(request.code()));
        synchronized (game) {
            game.join(player.getName());
            updates.sendRoom(game);
            updates.sendViews(game);
        }
    }
}
