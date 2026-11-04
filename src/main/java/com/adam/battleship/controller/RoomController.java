package com.adam.battleship.controller;

import com.adam.battleship.dto.ErrorMessage;
import com.adam.battleship.dto.JoinRequest;
import com.adam.battleship.dto.RoomUpdate;
import com.adam.battleship.game.Game;
import com.adam.battleship.game.GameRoomManager;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class RoomController {
    private final GameRoomManager rooms;
    private final SimpMessagingTemplate messaging;

    public RoomController(GameRoomManager rooms, SimpMessagingTemplate messaging) {
        this.rooms = rooms;
        this.messaging = messaging;
    }

    @MessageMapping("/create")
    @SendToUser("/queue/room")
    public RoomUpdate create(Principal player) {
        Game game = rooms.createRoom(player.getName());
        return RoomUpdate.from(game);
    }

    @MessageMapping("/join")
    public void join(JoinRequest request, Principal player) {
        Game game = rooms.joinRoom(normalize(request.code()), player.getName());
        RoomUpdate update = RoomUpdate.from(game);
        messaging.convertAndSendToUser(game.getPlayer1(), "/queue/room", update);
        messaging.convertAndSendToUser(game.getPlayer2(), "/queue/room", update);
    }

    @MessageExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @SendToUser("/queue/errors")
    public ErrorMessage handleError(RuntimeException e) {
        return new ErrorMessage(e.getMessage());
    }

    private String normalize(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Enter a room code");
        }
        return code.trim().toUpperCase();
    }
}
