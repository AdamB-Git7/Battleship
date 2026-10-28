package com.adam.battleship.game;

import com.adam.battleship.model.GameStatus;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameRoomManagerTest {
    private final GameRoomManager manager = new GameRoomManager();

    @Test
    void createRoomGivesSixUppercaseLetterCode() {
        Game game = manager.createRoom("alice");

        assertTrue(game.getCode().matches("[A-Z]{6}"), "bad code: " + game.getCode());
        assertEquals("alice", game.getPlayer1());
    }

    @Test
    void createdRoomCanBeFoundByItsCode() {
        Game game = manager.createRoom("alice");

        assertSame(game, manager.getRoom(game.getCode()));
    }

    @Test
    void eachRoomGetsADifferentCode() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(manager.createRoom("player" + i).getCode());
        }

        assertEquals(1000, codes.size());
    }

    @Test
    void joinRoomAddsSecondPlayer() {
        Game game = manager.createRoom("alice");

        Game joined = manager.joinRoom(game.getCode(), "bob");

        assertSame(game, joined);
        assertEquals("bob", game.getPlayer2());
        assertEquals(GameStatus.PLACING_SHIPS, game.getStatus());
    }

    @Test
    void joiningUnknownCodeFails() {
        assertThrows(IllegalArgumentException.class, () -> manager.joinRoom("ZZZZZZ", "bob"));
    }

    @Test
    void joiningFullRoomFails() {
        Game game = manager.createRoom("alice");
        manager.joinRoom(game.getCode(), "bob");

        assertThrows(IllegalStateException.class, () -> manager.joinRoom(game.getCode(), "carol"));
    }

    @Test
    void onlyOnePlayerWinsARaceToJoin() throws InterruptedException {
        Game game = manager.createRoom("alice");
        int players = 100;
        ExecutorService pool = Executors.newFixedThreadPool(players);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(players);
        AtomicInteger successes = new AtomicInteger();

        for (int i = 0; i < players; i++) {
            String playerId = "player" + i;
            pool.submit(() -> {
                try {
                    startSignal.await();
                    manager.joinRoom(game.getCode(), playerId);
                    successes.incrementAndGet();
                } catch (IllegalStateException expected) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finished.countDown();
                }
            });
        }

        startSignal.countDown();
        assertTrue(finished.await(5, TimeUnit.SECONDS), "threads did not finish");
        pool.shutdown();

        assertEquals(1, successes.get());
        assertTrue(game.getPlayer2().startsWith("player"));
    }
}
