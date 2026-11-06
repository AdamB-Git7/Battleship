package com.adam.battleship.controller;

import com.adam.battleship.dto.Notice;
import com.adam.battleship.game.Game;
import com.adam.battleship.game.GameRoomManager;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
public class PlayerExits {
    private final GameRoomManager rooms;
    private final SimpMessagingTemplate messaging;

    public PlayerExits(GameRoomManager rooms, SimpMessagingTemplate messaging) {
        this.rooms = rooms;
        this.messaging = messaging;
    }

    public void leave(String playerId) {
        rooms.findRoomOf(playerId).ifPresent(game -> close(game, playerId));
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Principal player = event.getUser();
        if (player != null) {
            leave(player.getName());
        }
    }

    private void close(Game game, String leaver) {
        synchronized (game) {
            rooms.removeRoom(game.getCode());
            String other = leaver.equals(game.getPlayer1()) ? game.getPlayer2() : game.getPlayer1();
            if (other != null) {
                messaging.convertAndSendToUser(other, "/queue/left", new Notice("Your opponent left the game."));
            }
        }
    }
}
