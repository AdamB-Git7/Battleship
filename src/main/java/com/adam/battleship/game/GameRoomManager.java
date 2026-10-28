package com.adam.battleship.game;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameRoomManager {
    private static final int CODE_LENGTH = 6;

    private final Map<String, Game> rooms = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public Game createRoom(String playerId) {
        while (true) {
            String code = generateCode();
            Game game = new Game(code, playerId);
            if (rooms.putIfAbsent(code, game) == null) {
                return game;
            }
        }
    }

    public Game joinRoom(String code, String playerId) {
        Game game = getRoom(code);
        game.join(playerId);
        return game;
    }

    public Game getRoom(String code) {
        Game game = rooms.get(code);
        if (game == null) {
            throw new IllegalArgumentException("Room not found: " + code);
        }
        return game;
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append((char) ('A' + random.nextInt(26)));
        }
        return code.toString();
    }
}