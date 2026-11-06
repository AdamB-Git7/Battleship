package com.adam.battleship.controller;

import com.adam.battleship.dto.RoomUpdate;
import com.adam.battleship.game.Game;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class GameUpdates {
    private final SimpMessagingTemplate messaging;

    public GameUpdates(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    public void sendRoom(Game game) {
        RoomUpdate update = RoomUpdate.from(game);
        for (String player : playersOf(game)) {
            messaging.convertAndSendToUser(player, "/queue/room", update);
        }
    }

    public void sendViews(Game game) {
        for (String player : playersOf(game)) {
            messaging.convertAndSendToUser(player, "/queue/game", game.viewFor(player));
        }
    }

    private String[] playersOf(Game game) {
        String player2 = game.getPlayer2();
        return player2 == null
                ? new String[] {game.getPlayer1()}
                : new String[] {game.getPlayer1(), player2};
    }
}
