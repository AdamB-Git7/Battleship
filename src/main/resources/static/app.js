const SIZE = 10;

const SHIP_LENGTHS = {
    CARRIER: 5,
    BATTLESHIP: 4,
    CRUISER: 3,
    SUBMARINE: 3,
    DESTROYER: 2
};

const LOBBY_TEXT = {
    WAITING_FOR_PLAYER: "Waiting for a second player. Share the room code."
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
    status: document.getElementById("status"),
    placement: document.getElementById("placement"),
    rotate: document.getElementById("rotate"),
    orientation: document.getElementById("orientation"),
    rematch: document.getElementById("rematch"),
    leave: document.getElementById("leave"),
    notice: document.getElementById("notice"),
    error: document.getElementById("error"),
    boards: document.getElementById("boards"),
    yourBoard: document.getElementById("your-board"),
    opponentBoard: document.getElementById("opponent-board")
};

let roomCode = null;
let view = null;
let orientation = "HORIZONTAL";

const yourCells = buildGrid(elements.yourBoard, placeShip, showPreview);
const opponentCells = buildGrid(elements.opponentBoard, fireAt, null);

const protocol = location.protocol === "https:" ? "wss" : "ws";
const client = new StompJs.Client({
    brokerURL: `${protocol}://${location.host}/ws`,
    reconnectDelay: 0
});

client.onConnect = () => {
    client.subscribe("/user/queue/room", message => showRoom(JSON.parse(message.body)));
    client.subscribe("/user/queue/game", message => showGame(JSON.parse(message.body)));
    client.subscribe("/user/queue/errors", message => showError(JSON.parse(message.body).message));
    client.subscribe("/user/queue/left", message => backToLobby(JSON.parse(message.body).message));
    setConnected(true);
};

client.onWebSocketClose = () => setConnected(false);

function setConnected(connected) {
    elements.connection.textContent = connected ? "Connected" : "Disconnected. Refresh the page.";
    elements.create.disabled = !connected;
    elements.join.disabled = !connected;
}

function buildGrid(container, onClick, onHover) {
    const cells = [];
    for (let row = 0; row < SIZE; row++) {
        const cellRow = [];
        for (let col = 0; col < SIZE; col++) {
            const cell = document.createElement("button");
            cell.className = "cell";
            cell.setAttribute("aria-label", `Row ${row + 1}, column ${col + 1}`);
            cell.addEventListener("click", () => onClick(row, col));
            if (onHover) {
                cell.addEventListener("mouseenter", () => onHover(row, col));
            }
            container.appendChild(cell);
            cellRow.push(cell);
        }
        cells.push(cellRow);
    }
    container.addEventListener("mouseleave", clearPreview);
    return cells;
}

function showRoom(room) {
    roomCode = room.code;
    elements.notice.hidden = true;
    elements.lobby.hidden = true;
    elements.room.hidden = false;
    elements.roomCode.textContent = room.code;
    elements.roomPlayers.textContent = room.players;
    if (room.status === "WAITING_FOR_PLAYER") {
        elements.status.textContent = LOBBY_TEXT.WAITING_FOR_PLAYER;
    }
}

function showGame(newView) {
    if (roomCode === null) {
        return;
    }
    view = newView;
    elements.error.hidden = true;
    elements.boards.hidden = false;
    paint(yourCells, view.yourBoard);
    paint(opponentCells, view.opponentBoard);
    elements.status.textContent = statusText();
    elements.placement.hidden = !isPlacing();
    elements.orientation.textContent = orientation === "HORIZONTAL" ? "Horizontal" : "Vertical";
    elements.opponentBoard.classList.toggle("active", canFire());
    elements.yourBoard.classList.toggle("active", isPlacing());
    elements.rematch.hidden = view.status !== "FINISHED";
    elements.rematch.disabled = view.rematchRequestedByYou;
    elements.rematch.textContent = view.rematchRequestedByOpponent ? "Accept rematch" : "Rematch";
}

function backToLobby(message) {
    roomCode = null;
    view = null;
    elements.room.hidden = true;
    elements.boards.hidden = true;
    elements.placement.hidden = true;
    elements.rematch.hidden = true;
    elements.error.hidden = true;
    elements.lobby.hidden = false;
    elements.code.value = "";
    elements.notice.textContent = message;
    elements.notice.hidden = message === null;
    clearPreview();
}

function leave() {
    const midGame = view !== null && (view.status === "PLACING_SHIPS" || view.status === "IN_PROGRESS");
    if (midGame && !confirm("Leave this game? Your opponent will be sent back to the menu.")) {
        return;
    }
    client.publish({ destination: "/app/leave" });
    backToLobby(null);
}

function paint(cells, board) {
    for (let row = 0; row < SIZE; row++) {
        for (let col = 0; col < SIZE; col++) {
            cells[row][col].className = "cell " + board[row][col].toLowerCase();
        }
    }
}

function statusText() {
    if (view.status === "PLACING_SHIPS") {
        if (isPlacing()) {
            const ship = view.shipsToPlace[0];
            return `Place your ${ship.toLowerCase()} (${SHIP_LENGTHS[ship]} squares) on your board.`;
        }
        return "Waiting for your opponent to place their ships.";
    }
    if (view.status === "IN_PROGRESS") {
        return view.yourTurn ? "Your turn: fire at the opponent's board." : "Opponent's turn…";
    }
    if (view.status === "FINISHED") {
        const result = view.youWon ? "You won! All enemy ships are sunk." : "You lost. All your ships are sunk.";
        if (view.rematchRequestedByYou) {
            return `${result} Waiting for your opponent to accept the rematch…`;
        }
        if (view.rematchRequestedByOpponent) {
            return `${result} Your opponent wants a rematch!`;
        }
        return result;
    }
    return "";
}

function isPlacing() {
    return view !== null && view.status === "PLACING_SHIPS" && view.shipsToPlace.length > 0;
}

function canFire() {
    return view !== null && view.status === "IN_PROGRESS" && view.yourTurn;
}

function placeShip(row, col) {
    if (!isPlacing()) {
        return;
    }
    client.publish({
        destination: "/app/place",
        body: JSON.stringify({ code: roomCode, type: view.shipsToPlace[0], row, col, orientation })
    });
}

function fireAt(row, col) {
    if (!canFire() || view.opponentBoard[row][col] !== "EMPTY") {
        return;
    }
    client.publish({
        destination: "/app/fire",
        body: JSON.stringify({ code: roomCode, row, col })
    });
}

function showPreview(row, col) {
    clearPreview();
    if (!isPlacing()) {
        return;
    }
    const length = SHIP_LENGTHS[view.shipsToPlace[0]];
    for (let i = 0; i < length; i++) {
        const r = orientation === "HORIZONTAL" ? row : row + i;
        const c = orientation === "HORIZONTAL" ? col + i : col;
        if (r < SIZE && c < SIZE) {
            yourCells[r][c].classList.add("preview");
        }
    }
}

function clearPreview() {
    for (const cellRow of yourCells) {
        for (const cell of cellRow) {
            cell.classList.remove("preview");
        }
    }
}

function rotate() {
    orientation = orientation === "HORIZONTAL" ? "VERTICAL" : "HORIZONTAL";
    elements.orientation.textContent = orientation === "HORIZONTAL" ? "Horizontal" : "Vertical";
    clearPreview();
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

elements.rotate.addEventListener("click", rotate);

elements.rematch.addEventListener("click", () => {
    client.publish({ destination: "/app/rematch" });
});

elements.leave.addEventListener("click", leave);

document.addEventListener("keydown", event => {
    if ((event.key === "r" || event.key === "R") && event.target !== elements.code) {
        rotate();
    }
});

client.activate();
