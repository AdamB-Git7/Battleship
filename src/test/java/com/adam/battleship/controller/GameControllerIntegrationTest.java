package com.adam.battleship.controller;

import com.adam.battleship.dto.ErrorMessage;
import com.adam.battleship.dto.FireRequest;
import com.adam.battleship.dto.JoinRequest;
import com.adam.battleship.dto.Notice;
import com.adam.battleship.dto.PlaceShipRequest;
import com.adam.battleship.dto.RoomUpdate;
import com.adam.battleship.game.PlayerView;
import com.adam.battleship.model.CellView;
import com.adam.battleship.model.GameStatus;
import com.adam.battleship.model.Orientation;
import com.adam.battleship.model.ShipType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameControllerIntegrationTest {
    private static final int ALICE_FIRST_ROW = 0;
    private static final int BOB_FIRST_ROW = 5;

    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;
    private final List<StompSession> sessions = new ArrayList<>();

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());
    }

    @AfterEach
    void tearDown() {
        sessions.stream()
                .filter(StompSession::isConnected)
                .forEach(StompSession::disconnect);
        stompClient.stop();
    }

    @Test
    void joiningSendsBothPlayersAnEmptyGameView() throws Exception {
        Player alice = connect();
        Player bob = connect();
        String code = createAndJoin(alice, bob);

        PlayerView aliceView = next(alice.game);
        PlayerView bobView = next(bob.game);

        assertEquals(code, aliceView.code());
        assertEquals(GameStatus.PLACING_SHIPS, aliceView.status());
        assertEquals(List.of(ShipType.values()), aliceView.shipsToPlace());
        assertEquals(List.of(ShipType.values()), bobView.shipsToPlace());
    }

    @Test
    void placedShipIsVisibleToItsOwnerOnly() throws Exception {
        Player alice = connect();
        Player bob = connect();
        String code = createAndJoin(alice, bob);
        next(alice.game);
        next(bob.game);

        alice.session.send("/app/place",
                new PlaceShipRequest(code, ShipType.CARRIER, 0, 0, Orientation.HORIZONTAL));

        PlayerView aliceView = next(alice.game);
        PlayerView bobView = next(bob.game);
        assertEquals(CellView.SHIP, aliceView.yourBoard().get(0).get(4));
        assertEquals(ShipType.BATTLESHIP, aliceView.shipsToPlace().get(0));
        assertFalse(containsShip(bobView.opponentBoard()), "Bob received Alice's ship position");
    }

    @Test
    void invalidPlacementSendsAnErrorToThatPlayer() throws Exception {
        Player alice = connect();
        Player bob = connect();
        String code = createAndJoin(alice, bob);

        alice.session.send("/app/place",
                new PlaceShipRequest(code, ShipType.CARRIER, 0, 8, Orientation.HORIZONTAL));

        assertEquals("CARRIER goes off the board", next(alice.errors).message());
        assertNull(bob.errors.poll(500, TimeUnit.MILLISECONDS));
    }

    @Test
    void missingShipTypeIsRejected() throws Exception {
        Player alice = connect();
        Player bob = connect();
        String code = createAndJoin(alice, bob);

        alice.session.send("/app/place", new PlaceShipRequest(code, null, 0, 0, Orientation.HORIZONTAL));

        assertEquals("Choose a ship and a direction", next(alice.errors).message());
    }

    @Test
    void firingOutOfTurnSendsAnError() throws Exception {
        Player alice = connect();
        Player bob = connect();
        String code = startGame(alice, bob);

        bob.session.send("/app/fire", new FireRequest(code, 0, 0));

        assertEquals("It's not your turn", next(bob.errors).message());
    }

    @Test
    void strangerCannotFireIntoSomeoneElsesGame() throws Exception {
        Player alice = connect();
        Player bob = connect();
        String code = startGame(alice, bob);
        Player mallory = connect();

        mallory.session.send("/app/fire", new FireRequest(code, BOB_FIRST_ROW, 0));

        assertEquals("It's not your turn", next(mallory.errors).message());
        assertNull(alice.game.poll(500, TimeUnit.MILLISECONDS));
        assertNull(bob.game.poll(500, TimeUnit.MILLISECONDS));
    }

    @Test
    void fullGameOverTheNetworkNeverLeaksShips() throws Exception {
        Player alice = connect();
        Player bob = connect();
        String code = startGame(alice, bob);

        List<int[]> bobShips = shipSquares(BOB_FIRST_ROW);
        PlayerView aliceView = null;
        PlayerView bobView = null;
        for (int i = 0; i < bobShips.size(); i++) {
            int[] target = bobShips.get(i);
            alice.session.send("/app/fire", new FireRequest(code, target[0], target[1]));
            aliceView = checkedNext(alice.game);
            bobView = checkedNext(bob.game);
            assertTrue(aliceView.opponentBoard().get(target[0]).get(target[1]) != CellView.EMPTY);

            if (aliceView.status() == GameStatus.IN_PROGRESS) {
                assertTrue(bobView.yourTurn());
                bob.session.send("/app/fire", new FireRequest(code, BOB_FIRST_ROW + i / 10, i % 10));
                aliceView = checkedNext(alice.game);
                bobView = checkedNext(bob.game);
                assertTrue(aliceView.yourTurn());
            }
        }

        assertEquals(GameStatus.FINISHED, aliceView.status());
        assertTrue(aliceView.youWon());
        assertFalse(bobView.youWon());
        assertEquals(CellView.SUNK, aliceView.opponentBoard().get(BOB_FIRST_ROW).get(0));
    }

    @Test
    void bothPlayersAskingForARematchStartsANewGame() throws Exception {
        Player alice = connect();
        Player bob = connect();
        String code = startGame(alice, bob);
        playToTheEnd(alice, bob, code);

        bob.session.send("/app/rematch", null);
        PlayerView aliceSees = checkedNext(alice.game);
        PlayerView bobSees = checkedNext(bob.game);
        assertTrue(aliceSees.rematchRequestedByOpponent());
        assertTrue(bobSees.rematchRequestedByYou());
        assertEquals(GameStatus.FINISHED, aliceSees.status());

        alice.session.send("/app/rematch", null);
        aliceSees = checkedNext(alice.game);
        bobSees = checkedNext(bob.game);
        assertEquals(GameStatus.PLACING_SHIPS, aliceSees.status());
        assertEquals(code, aliceSees.code());
        assertEquals(List.of(ShipType.values()), bobSees.shipsToPlace());
        assertFalse(containsShip(bobSees.yourBoard()), "old ships survived the rematch");
    }

    @Test
    void rematchBeforeTheGameIsOverSendsAnError() throws Exception {
        Player alice = connect();
        Player bob = connect();
        startGame(alice, bob);

        alice.session.send("/app/rematch", null);

        assertEquals("The game is not over yet", next(alice.errors).message());
    }

    @Test
    void leavingTellsTheOpponentAndDeletesTheRoom() throws Exception {
        Player alice = connect();
        Player bob = connect();
        String code = startGame(alice, bob);

        bob.session.send("/app/leave", null);

        assertEquals("Your opponent left the game.", next(alice.left).message());
        assertNull(bob.left.poll(500, TimeUnit.MILLISECONDS));
        Player carol = connect();
        carol.session.send("/app/join", new JoinRequest(code));
        assertEquals("Room not found: " + code, next(carol.errors).message());
    }

    @Test
    void closingTheTabCountsAsLeaving() throws Exception {
        Player alice = connect();
        Player bob = connect();
        startGame(alice, bob);

        bob.session.disconnect();

        assertEquals("Your opponent left the game.", next(alice.left).message());
    }

    @Test
    void creatorLeavingAnEmptyRoomDeletesIt() throws Exception {
        Player alice = connect();
        alice.session.send("/app/create", null);
        String code = next(alice.room).code();

        alice.session.send("/app/leave", null);

        Player bob = connect();
        bob.session.send("/app/join", new JoinRequest(code));
        assertEquals("Room not found: " + code, next(bob.errors).message());
    }

    @Test
    void bothPlayersCanStartNewGamesAfterLeaving() throws Exception {
        Player alice = connect();
        Player bob = connect();
        startGame(alice, bob);
        bob.session.send("/app/leave", null);
        next(alice.left);

        alice.session.send("/app/create", null);
        bob.session.send("/app/create", null);

        assertNotNull(next(alice.room).code());
        assertNotNull(next(bob.room).code());
    }

    @Test
    void leavingWhenInNoRoomDoesNothing() throws Exception {
        Player alice = connect();

        alice.session.send("/app/leave", null);
        alice.session.send("/app/create", null);

        assertNotNull(next(alice.room).code());
        assertNull(alice.errors.poll(500, TimeUnit.MILLISECONDS));
    }

    private void playToTheEnd(Player alice, Player bob, String code) throws InterruptedException {
        List<int[]> bobShips = shipSquares(BOB_FIRST_ROW);
        for (int i = 0; i < bobShips.size(); i++) {
            int[] target = bobShips.get(i);
            alice.session.send("/app/fire", new FireRequest(code, target[0], target[1]));
            PlayerView aliceView = checkedNext(alice.game);
            checkedNext(bob.game);
            if (aliceView.status() == GameStatus.IN_PROGRESS) {
                bob.session.send("/app/fire", new FireRequest(code, BOB_FIRST_ROW + i / 10, i % 10));
                checkedNext(alice.game);
                checkedNext(bob.game);
            }
        }
    }

    private record Player(StompSession session, BlockingQueue<RoomUpdate> room,
                          BlockingQueue<PlayerView> game, BlockingQueue<ErrorMessage> errors,
                          BlockingQueue<Notice> left) {
    }

    private Player connect() throws Exception {
        StompSession session = stompClient
                .connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {
                })
                .get(5, TimeUnit.SECONDS);
        sessions.add(session);
        return new Player(session,
                subscribe(session, "/user/queue/room", RoomUpdate.class),
                subscribe(session, "/user/queue/game", PlayerView.class),
                subscribe(session, "/user/queue/errors", ErrorMessage.class),
                subscribe(session, "/user/queue/left", Notice.class));
    }

    private String createAndJoin(Player alice, Player bob) throws InterruptedException {
        alice.session.send("/app/create", null);
        String code = next(alice.room).code();
        bob.session.send("/app/join", new JoinRequest(code));
        next(bob.room);
        next(alice.room);
        return code;
    }

    private String startGame(Player alice, Player bob) throws InterruptedException {
        String code = createAndJoin(alice, bob);
        next(alice.game);
        next(bob.game);
        placeAllShips(alice, code, ALICE_FIRST_ROW, alice, bob);
        placeAllShips(bob, code, BOB_FIRST_ROW, alice, bob);
        return code;
    }

    private void placeAllShips(Player player, String code, int firstRow, Player alice, Player bob)
            throws InterruptedException {
        int row = firstRow;
        for (ShipType type : ShipType.values()) {
            player.session.send("/app/place", new PlaceShipRequest(code, type, row, 0, Orientation.HORIZONTAL));
            checkedNext(alice.game);
            checkedNext(bob.game);
            row++;
        }
    }

    private List<int[]> shipSquares(int firstRow) {
        List<int[]> squares = new ArrayList<>();
        int row = firstRow;
        for (ShipType type : ShipType.values()) {
            for (int col = 0; col < type.getLength(); col++) {
                squares.add(new int[] {row, col});
            }
            row++;
        }
        return squares;
    }

    private PlayerView checkedNext(BlockingQueue<PlayerView> queue) throws InterruptedException {
        PlayerView view = next(queue);
        assertFalse(containsShip(view.opponentBoard()), "a view leaked the opponent's ships");
        return view;
    }

    private boolean containsShip(List<List<CellView>> board) {
        for (List<CellView> row : board) {
            if (row.contains(CellView.SHIP)) {
                return true;
            }
        }
        return false;
    }

    private <T> BlockingQueue<T> subscribe(StompSession session, String destination, Class<T> type) {
        BlockingQueue<T> received = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return type;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add(type.cast(payload));
            }
        });
        return received;
    }

    private <T> T next(BlockingQueue<T> queue) throws InterruptedException {
        T message = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull(message, "no message received within 5 seconds");
        return message;
    }
}
