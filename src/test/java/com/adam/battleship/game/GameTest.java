package com.adam.battleship.game;

import com.adam.battleship.model.Coordinate;
import com.adam.battleship.model.GameStatus;
import com.adam.battleship.model.Orientation;
import com.adam.battleship.model.ShipType;
import com.adam.battleship.model.ShotResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTest {
    private static final int ALICE_FIRST_ROW = 0;
    private static final int BOB_FIRST_ROW = 5;

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

    @Test
    void cannotPlaceShipsBeforeSecondPlayerJoins() {
        Game game = new Game("ABCDEF", "alice");

        assertThrows(IllegalStateException.class, () ->
                game.placeShip("alice", ShipType.DESTROYER, new Coordinate(0, 0), Orientation.HORIZONTAL));
    }

    @Test
    void strangerCannotPlaceShips() {
        Game game = gameWithBothPlayers();

        assertThrows(IllegalArgumentException.class, () ->
                game.placeShip("mallory", ShipType.DESTROYER, new Coordinate(0, 0), Orientation.HORIZONTAL));
    }

    @Test
    void eachPlayerHasTheirOwnBoard() {
        Game game = gameWithBothPlayers();

        game.placeShip("alice", ShipType.DESTROYER, new Coordinate(0, 0), Orientation.HORIZONTAL);

        assertDoesNotThrow(() ->
                game.placeShip("bob", ShipType.DESTROYER, new Coordinate(0, 0), Orientation.HORIZONTAL));
    }

    @Test
    void gameStartsOnlyWhenBothPlayersPlacedAllShips() {
        Game game = gameWithBothPlayers();

        placeAllShips(game, "alice", ALICE_FIRST_ROW);
        assertEquals(GameStatus.PLACING_SHIPS, game.getStatus());

        placeAllShips(game, "bob", BOB_FIRST_ROW);
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertEquals("alice", game.getCurrentTurn());
    }

    @Test
    void cannotPlaceShipsAfterGameStarted() {
        Game game = startedGame();

        assertThrows(IllegalStateException.class, () ->
                game.placeShip("alice", ShipType.DESTROYER, new Coordinate(9, 0), Orientation.HORIZONTAL));
    }

    @Test
    void cannotFireBeforeGameStarts() {
        Game game = gameWithBothPlayers();
        placeAllShips(game, "alice", ALICE_FIRST_ROW);

        assertThrows(IllegalStateException.class, () -> game.fire("alice", new Coordinate(0, 0)));
    }

    @Test
    void turnsAlternateAfterEachShot() {
        Game game = startedGame();

        game.fire("alice", new Coordinate(9, 9));
        assertEquals("bob", game.getCurrentTurn());

        game.fire("bob", new Coordinate(9, 9));
        assertEquals("alice", game.getCurrentTurn());
    }

    @Test
    void turnSwitchesEvenAfterAHit() {
        Game game = startedGame();

        assertEquals(ShotResult.HIT, game.fire("alice", new Coordinate(5, 0)));

        assertEquals("bob", game.getCurrentTurn());
    }

    @Test
    void cannotFireOutOfTurn() {
        Game game = startedGame();

        assertThrows(IllegalStateException.class, () -> game.fire("bob", new Coordinate(0, 0)));
        assertEquals("alice", game.getCurrentTurn());
    }

    @Test
    void strangerCannotFire() {
        Game game = startedGame();

        assertThrows(IllegalStateException.class, () -> game.fire("mallory", new Coordinate(0, 0)));
    }

    @Test
    void shotsLandOnTheOpponentsBoard() {
        Game game = startedGame();

        assertEquals(ShotResult.HIT, game.fire("alice", new Coordinate(5, 0)));
        assertEquals(ShotResult.MISS, game.fire("bob", new Coordinate(5, 0)));
        assertEquals(ShotResult.MISS, game.fire("alice", new Coordinate(0, 0)));
        assertEquals(ShotResult.HIT, game.fire("bob", new Coordinate(0, 0)));
    }

    @Test
    void offBoardShotKeepsTheTurn() {
        Game game = startedGame();

        assertThrows(IllegalArgumentException.class, () -> game.fire("alice", new Coordinate(10, 0)));

        assertEquals("alice", game.getCurrentTurn());
    }

    @Test
    void repeatShotKeepsTheTurn() {
        Game game = startedGame();
        game.fire("alice", new Coordinate(9, 9));
        game.fire("bob", new Coordinate(9, 9));

        assertThrows(IllegalStateException.class, () -> game.fire("alice", new Coordinate(9, 9)));

        assertEquals("alice", game.getCurrentTurn());
    }

    @Test
    void sinkingEveryShipWinsTheGame() {
        Game game = startedGame();
        List<Coordinate> bobShips = shipSquares(BOB_FIRST_ROW);
        List<Coordinate> water = halfOfBoard(BOB_FIRST_ROW);

        ShotResult last = null;
        for (int i = 0; i < bobShips.size(); i++) {
            last = game.fire("alice", bobShips.get(i));
            if (i < bobShips.size() - 1) {
                assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
                game.fire("bob", water.get(i));
            }
        }

        assertEquals(ShotResult.SUNK, last);
        assertEquals(GameStatus.FINISHED, game.getStatus());
        assertEquals("alice", game.getWinner());
    }

    @Test
    void nobodyCanFireAfterTheGameIsOver() {
        Game game = startedGame();
        List<Coordinate> bobShips = shipSquares(BOB_FIRST_ROW);
        List<Coordinate> water = halfOfBoard(BOB_FIRST_ROW);
        for (int i = 0; i < bobShips.size(); i++) {
            game.fire("alice", bobShips.get(i));
            if (i < bobShips.size() - 1) {
                game.fire("bob", water.get(i));
            }
        }

        assertThrows(IllegalStateException.class, () -> game.fire("bob", new Coordinate(9, 9)));
        assertThrows(IllegalStateException.class, () -> game.fire("alice", new Coordinate(9, 9)));
    }

    @Test
    void noWinnerWhileTheGameIsRunning() {
        Game game = startedGame();

        game.fire("alice", new Coordinate(0, 0));

        assertNull(game.getWinner());
    }

    @Test
    void onlyOneShotCountsWhenAPlayerFiresManyAtOnce() throws InterruptedException {
        Game game = startedGame();
        List<Coordinate> targets = halfOfBoard(ALICE_FIRST_ROW);
        int shots = targets.size();
        ExecutorService pool = Executors.newFixedThreadPool(shots);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(shots);
        AtomicInteger accepted = new AtomicInteger();

        for (Coordinate target : targets) {
            pool.submit(() -> {
                try {
                    startSignal.await();
                    game.fire("alice", target);
                    accepted.incrementAndGet();
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

        assertEquals(1, accepted.get());
        assertEquals("bob", game.getCurrentTurn());
    }

    private Game gameWithBothPlayers() {
        Game game = new Game("ABCDEF", "alice");
        game.join("bob");
        return game;
    }

    private Game startedGame() {
        Game game = gameWithBothPlayers();
        placeAllShips(game, "alice", ALICE_FIRST_ROW);
        placeAllShips(game, "bob", BOB_FIRST_ROW);
        return game;
    }

    private void placeAllShips(Game game, String player, int firstRow) {
        int row = firstRow;
        for (ShipType type : ShipType.values()) {
            game.placeShip(player, type, new Coordinate(row, 0), Orientation.HORIZONTAL);
            row++;
        }
    }

    private List<Coordinate> shipSquares(int firstRow) {
        List<Coordinate> squares = new ArrayList<>();
        int row = firstRow;
        for (ShipType type : ShipType.values()) {
            for (int col = 0; col < type.getLength(); col++) {
                squares.add(new Coordinate(row, col));
            }
            row++;
        }
        return squares;
    }

    private List<Coordinate> halfOfBoard(int firstRow) {
        List<Coordinate> squares = new ArrayList<>();
        for (int row = firstRow; row < firstRow + 5; row++) {
            for (int col = 0; col < 10; col++) {
                squares.add(new Coordinate(row, col));
            }
        }
        return squares;
    }
}
