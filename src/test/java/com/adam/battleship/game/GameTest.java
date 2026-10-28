package com.adam.battleship.game;

import com.adam.battleship.model.GameStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameTest {
    @Test
    void newGameIsWaitingForSecondPlayer() {
        Game game = new Game("ABCDEF", "alice");

        assertEquals("ABCDEF", game.getCode());
        assertEquals("alice", game.getPlayer1());
        assertNull(game.getPlayer2());
        assertEquals(GameStatus.WAITING_FOR_PLAYER, game.getStatus());
    }

    @Test
    void joiningAddsPlayer2AndStartsShipPlacement() {
        Game game = new Game("ABCDEF", "alice");

        game.join("bob");

        assertEquals("bob", game.getPlayer2());
        assertEquals(GameStatus.PLACING_SHIPS, game.getStatus());
    }

    @Test
    void cannotJoinAFullRoom() {
        Game game = new Game("ABCDEF", "alice");
        game.join("bob");

        assertThrows(IllegalStateException.class, () -> game.join("carol"));

        assertEquals("bob", game.getPlayer2());
    }

    @Test
    void player1CannotJoinTheirOwnRoom() {
        Game game = new Game("ABCDEF", "alice");

        assertThrows(IllegalStateException.class, () -> game.join("alice"));

        assertNull(game.getPlayer2());
        assertEquals(GameStatus.WAITING_FOR_PLAYER, game.getStatus());
    }

    @Test
    void player1CheckComparesTextNotObjects() {
        Game game = new Game("ABCDEF", "alice");
        String sameNameNewObject = new String("alice");

        assertThrows(IllegalStateException.class, () -> game.join(sameNameNewObject));
    }
}
