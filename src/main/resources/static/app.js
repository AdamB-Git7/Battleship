const SIZE = 10;
const ROW_LABELS = "ABCDEFGHIJ";

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
let lastHover = null;

const yourBoard = buildBoard(elements.yourBoard, placeShip, showPreview);
const opponentBoard = buildBoard(elements.opponentBoard, fireAt, null);

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

function buildBoard(container, onClick, onHover) {
    const colLabels = document.createElement("div");
    colLabels.className = "col-labels";
    for (let col = 0; col < SIZE; col++) {
        const label = document.createElement("span");
        label.textContent = col + 1;
        colLabels.appendChild(label);
    }

    const rowLabels = document.createElement("div");
    rowLabels.className = "row-labels";
    for (let row = 0; row < SIZE; row++) {
        const label = document.createElement("span");
        label.textContent = ROW_LABELS[row];
        rowLabels.appendChild(label);
    }

    const sea = document.createElement("div");
    sea.className = "sea";
    const ships = document.createElement("div");
    ships.className = "ships";
    const preview = document.createElement("div");
    preview.className = "ships";
    const grid = document.createElement("div");
    grid.className = "grid";
    sea.append(ships, preview, grid);

    const cells = [];
    for (let row = 0; row < SIZE; row++) {
        const cellRow = [];
        for (let col = 0; col < SIZE; col++) {
            const cell = document.createElement("button");
            cell.className = "cell empty";
            cell.setAttribute("aria-label", `${ROW_LABELS[row]}${col + 1}`);
            cell.addEventListener("click", () => onClick(row, col));
            if (onHover) {
                cell.addEventListener("mouseenter", () => onHover(row, col));
            }
            grid.appendChild(cell);
            cellRow.push(cell);
        }
        cells.push(cellRow);
    }
    grid.addEventListener("mouseleave", () => {
        lastHover = null;
        clearPreview();
    });

    const corner = document.createElement("span");
    container.append(corner, colLabels, rowLabels, sea);
    return { cells, ships, preview };
}

function showRoom(room) {
    roomCode = room.code;
    elements.notice.hidden = true;
    elements.lobby.hidden = true;
    elements.room.hidden = false;
    elements.roomCode.textContent = room.code;
    elements.roomPlayers.textContent = room.players;
    if (room.status === "WAITING_FOR_PLAYER") {
        elements.status.textContent = "Waiting for a second player. Share the room code.";
    }
}

function showGame(newView) {
    if (roomCode === null) {
        return;
    }
    view = newView;
    elements.error.hidden = true;
    elements.boards.hidden = false;
    paint(yourBoard, view.yourBoard, view.yourShips);
    paint(opponentBoard, view.opponentBoard, view.opponentShips);
    elements.status.textContent = statusText();
    elements.placement.hidden = !isPlacing();
    elements.orientation.textContent = orientation === "HORIZONTAL" ? "Horizontal" : "Vertical";
    elements.opponentBoard.classList.toggle("active", canFire());
    elements.yourBoard.classList.toggle("active", isPlacing());
    elements.rematch.hidden = view.status !== "FINISHED";
    elements.rematch.disabled = view.rematchRequestedByYou;
    elements.rematch.textContent = view.rematchRequestedByOpponent ? "Accept rematch" : "Rematch";
    if (lastHover !== null) {
        showPreview(lastHover.row, lastHover.col);
    }
}

function paint(board, cellsData, ships) {
    for (let row = 0; row < SIZE; row++) {
        for (let col = 0; col < SIZE; col++) {
            board.cells[row][col].className = "cell " + cellsData[row][col].toLowerCase();
        }
    }
    board.ships.replaceChildren();
    for (const ship of ships) {
        placeShipSvg(board.ships, ship.type, ship.row, ship.col, ship.orientation, ship.sunk ? "sunk" : "");
    }
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

function statusText() {
    if (view.status === "PLACING_SHIPS") {
        if (isPlacing()) {
            const ship = view.shipsToPlace[0];
            return `Place your ${ship.toLowerCase()} (${SHIP_LENGTHS[ship]} squares) in your waters.`;
        }
        return "Waiting for your opponent to place their ships.";
    }
    if (view.status === "IN_PROGRESS") {
        return view.yourTurn ? "Your turn: fire at the enemy waters." : "Opponent's turn…";
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
    lastHover = { row, col };
    clearPreview();
    if (!isPlacing()) {
        return;
    }
    const type = view.shipsToPlace[0];
    const valid = fits(row, col, SHIP_LENGTHS[type]);
    placeShipSvg(yourBoard.preview, type, row, col, orientation, valid ? "preview" : "preview invalid");
}

function fits(row, col, length) {
    for (let i = 0; i < length; i++) {
        const r = orientation === "HORIZONTAL" ? row : row + i;
        const c = orientation === "HORIZONTAL" ? col + i : col;
        if (r >= SIZE || c >= SIZE || view.yourBoard[r][c] !== "EMPTY") {
            return false;
        }
    }
    return true;
}

function clearPreview() {
    yourBoard.preview.replaceChildren();
}

function rotate() {
    orientation = orientation === "HORIZONTAL" ? "VERTICAL" : "HORIZONTAL";
    elements.orientation.textContent = orientation === "HORIZONTAL" ? "Horizontal" : "Vertical";
    if (lastHover !== null) {
        showPreview(lastHover.row, lastHover.col);
    }
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
