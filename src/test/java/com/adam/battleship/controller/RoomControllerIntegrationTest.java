package com.adam.battleship.controller;

import com.adam.battleship.dto.ErrorMessage;
import com.adam.battleship.dto.JoinRequest;
import com.adam.battleship.dto.RoomUpdate;
import com.adam.battleship.model.GameStatus;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RoomControllerIntegrationTest {
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
        sessions.forEach(StompSession::disconnect);
        stompClient.stop();
    }

    @Test
    void creatingARoomRepliesWithItsCode() throws Exception {
        Player alice = connect();

        alice.session.send("/app/create", null);

        RoomUpdate room = next(alice.room);
        assertTrue(room.code().matches("[A-Z]{6}"), "bad code: " + room.code());
        assertEquals(GameStatus.WAITING_FOR_PLAYER, room.status());
        assertEquals(1, room.players());
    }

    @Test
    void secondPlayerJoinsAndBothPlayersAreTold() throws Exception {
        Player alice = connect();
        String code = createRoom(alice);
        Player bob = connect();

        bob.session.send("/app/join", new JoinRequest(code));

        RoomUpdate bobSees = next(bob.room);
        assertEquals(code, bobSees.code());
        assertEquals(GameStatus.PLACING_SHIPS, bobSees.status());
        assertEquals(2, bobSees.players());

        RoomUpdate aliceSees = next(alice.room);
        assertEquals(GameStatus.PLACING_SHIPS, aliceSees.status());
        assertEquals(2, aliceSees.players());
    }

    @Test
    void roomCodeIsCaseInsensitiveAndTrimmed() throws Exception {
        Player alice = connect();
        String code = createRoom(alice);
        Player bob = connect();

        bob.session.send("/app/join", new JoinRequest("  " + code.toLowerCase() + " "));

        assertEquals(code, next(bob.room).code());
    }

    @Test
    void joiningAnUnknownRoomSendsAnErrorOnlyToThatPlayer() throws Exception {
        Player alice = connect();
        createRoom(alice);
        Player bob = connect();

        bob.session.send("/app/join", new JoinRequest("ZZZZZZ"));

        assertEquals("Room not found: ZZZZZZ", next(bob.errors).message());
        assertNull(alice.errors.poll(500, TimeUnit.MILLISECONDS));
    }

    @Test
    void emptyCodeSendsAnError() throws Exception {
        Player bob = connect();

        bob.session.send("/app/join", new JoinRequest(""));

        assertEquals("Enter a room code", next(bob.errors).message());
    }

    @Test
    void thirdPlayerIsToldTheRoomIsFull() throws Exception {
        Player alice = connect();
        String code = createRoom(alice);
        Player bob = connect();
        bob.session.send("/app/join", new JoinRequest(code));
        next(bob.room);
        Player carol = connect();

        carol.session.send("/app/join", new JoinRequest(code));

        assertEquals("Room is full", next(carol.errors).message());
    }

    @Test
    void creatorCannotJoinTheirOwnRoom() throws Exception {
        Player alice = connect();
        String code = createRoom(alice);

        alice.session.send("/app/join", new JoinRequest(code));

        assertEquals("You are already in a room", next(alice.errors).message());
    }

    @Test
    void playerInARoomCannotCreateAnother() throws Exception {
        Player alice = connect();
        createRoom(alice);

        alice.session.send("/app/create", null);

        assertEquals("You are already in a room", next(alice.errors).message());
        assertNull(alice.room.poll(500, TimeUnit.MILLISECONDS));
    }

    @Test
    void playerInARoomCannotJoinAnother() throws Exception {
        Player alice = connect();
        createRoom(alice);
        Player dave = connect();
        String daveCode = createRoom(dave);

        alice.session.send("/app/join", new JoinRequest(daveCode));

        assertEquals("You are already in a room", next(alice.errors).message());
    }

    @Test
    void playersInOtherRoomsHearNothing() throws Exception {
        Player alice = connect();
        String code = createRoom(alice);
        Player dave = connect();
        createRoom(dave);
        Player bob = connect();

        bob.session.send("/app/join", new JoinRequest(code));
        next(bob.room);
        next(alice.room);

        assertNull(dave.room.poll(500, TimeUnit.MILLISECONDS));
    }

    private record Player(StompSession session, BlockingQueue<RoomUpdate> room, BlockingQueue<ErrorMessage> errors) {
    }

    private Player connect() throws Exception {
        StompSession session = stompClient
                .connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {
                })
                .get(5, TimeUnit.SECONDS);
        sessions.add(session);
        BlockingQueue<RoomUpdate> room = subscribe(session, "/user/queue/room", RoomUpdate.class);
        BlockingQueue<ErrorMessage> errors = subscribe(session, "/user/queue/errors", ErrorMessage.class);
        return new Player(session, room, errors);
    }

    private String createRoom(Player player) throws InterruptedException {
        player.session.send("/app/create", null);
        return next(player.room).code();
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
