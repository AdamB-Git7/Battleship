const STATUS_TEXT = {
    WAITING_FOR_PLAYER: "Waiting for a second player. Share the room code.",
    PLACING_SHIPS: "Both players are here. Ship placement is next.",
    IN_PROGRESS: "The game is on.",
    FINISHED: "The game is over."
};

const elements = {
    connection: document.getElementById("connection"),
    create: document.getElementById("create"),
    join: document.getElementById("join"),
    code: document.getElementById("code"),
    lobby: document.getElementById("lobby"),
    room: document.getElementById("room"),
    roomCode: document.getElementById("room-code"),
    roomPlayers: document.getElementById("room-players"),
    roomStatus: document.getElementById("room-status"),
    error: document.getElementById("error")
};

const protocol = location.protocol === "https:" ? "wss" : "ws";
const client = new StompJs.Client({
    brokerURL: `${protocol}://${location.host}/ws`,
    reconnectDelay: 0
});

client.onConnect = () => {
    client.subscribe("/user/queue/room", message => showRoom(JSON.parse(message.body)));
    client.subscribe("/user/queue/errors", message => showError(JSON.parse(message.body).message));
    setConnected(true);
};

client.onWebSocketClose = () => setConnected(false);

function setConnected(connected) {
    elements.connection.textContent = connected ? "Connected" : "Disconnected. Refresh the page.";
    elements.create.disabled = !connected;
    elements.join.disabled = !connected;
}

function showRoom(room) {
    elements.lobby.hidden = true;
    elements.room.hidden = false;
    elements.error.hidden = true;
    elements.roomCode.textContent = room.code;
    elements.roomPlayers.textContent = room.players;
    elements.roomStatus.textContent = STATUS_TEXT[room.status];
}

function showError(message) {
    elements.error.textContent = message;
    elements.error.hidden = false;
}

elements.create.addEventListener("click", () => {
    client.publish({ destination: "/app/create" });
});

elements.join.addEventListener("click", () => {
    client.publish({
        destination: "/app/join",
        body: JSON.stringify({ code: elements.code.value })
    });
});

elements.code.addEventListener("keydown", event => {
    if (event.key === "Enter") {
        elements.join.click();
    }
});

client.activate();
