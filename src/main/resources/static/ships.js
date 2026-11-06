const SHIP_LENGTHS = {
    CARRIER: 5,
    BATTLESHIP: 4,
    CRUISER: 3,
    SUBMARINE: 3,
    DESTROYER: 2
};

const SHIP_COLORS = {
    outline: "#1b2533",
    hull: "#55606e",
    deck: "#7d8896",
    detail: "#c9d1db",
    dark: "#3a4350"
};

function hullPath(w) {
    return `M 16 14 L ${w - 90} 14 C ${w - 40} 14 ${w - 12} 34 ${w - 4} 50 `
        + `C ${w - 12} 66 ${w - 40} 86 ${w - 90} 86 L 16 86 Q 6 86 6 76 L 6 24 Q 6 14 16 14 Z`;
}

function deckPath(w) {
    return `M 22 24 L ${w - 94} 24 C ${w - 56} 24 ${w - 30} 38 ${w - 22} 50 `
        + `C ${w - 30} 62 ${w - 56} 76 ${w - 94} 76 L 22 76 Z`;
}

function turret(x, towardBow) {
    const end = towardBow ? x + 36 : x - 36;
    return `<line x1="${x}" y1="50" x2="${end}" y2="50" stroke="${SHIP_COLORS.outline}" stroke-width="8" stroke-linecap="round"/>`
        + `<circle cx="${x}" cy="50" r="16" fill="${SHIP_COLORS.detail}" stroke="${SHIP_COLORS.outline}" stroke-width="4"/>`;
}

function bridge(x, width) {
    return `<rect x="${x}" y="32" width="${width}" height="36" rx="6" fill="${SHIP_COLORS.detail}" stroke="${SHIP_COLORS.outline}" stroke-width="4"/>`
        + `<rect x="${x + 8}" y="40" width="${width - 16}" height="7" rx="3" fill="${SHIP_COLORS.dark}"/>`;
}

function standardHull(w) {
    return `<path d="${hullPath(w)}" fill="${SHIP_COLORS.hull}" stroke="${SHIP_COLORS.outline}" stroke-width="4"/>`
        + `<path d="${deckPath(w)}" fill="${SHIP_COLORS.deck}"/>`;
}

const SHIP_DRAWINGS = {
    CARRIER(w) {
        return `<path d="${hullPath(w)}" fill="${SHIP_COLORS.hull}" stroke="${SHIP_COLORS.outline}" stroke-width="4"/>`
            + `<path d="${deckPath(w)}" fill="${SHIP_COLORS.dark}"/>`
            + `<line x1="36" y1="54" x2="${w - 110}" y2="54" stroke="#e5e7eb" stroke-width="4" stroke-dasharray="18 14"/>`
            + `<line x1="36" y1="36" x2="${w - 150}" y2="36" stroke="#e5e7eb" stroke-opacity="0.35" stroke-width="3"/>`
            + `<line x1="36" y1="70" x2="${w - 150}" y2="70" stroke="#e5e7eb" stroke-opacity="0.35" stroke-width="3"/>`
            + `<rect x="${w * 0.55}" y="8" width="64" height="24" rx="4" fill="${SHIP_COLORS.detail}" stroke="${SHIP_COLORS.outline}" stroke-width="4"/>`
            + `<rect x="${w * 0.55 + 22}" y="2" width="18" height="10" fill="${SHIP_COLORS.detail}" stroke="${SHIP_COLORS.outline}" stroke-width="3"/>`;
    },
    BATTLESHIP(w) {
        return standardHull(w) + turret(74, false) + turret(154, false) + bridge(200, 62) + turret(310, true);
    },
    CRUISER(w) {
        return standardHull(w) + turret(70, false) + bridge(116, 60) + turret(226, true);
    },
    SUBMARINE(w) {
        return `<path d="M 24 50 L 4 26 L 4 74 Z" fill="${SHIP_COLORS.dark}" stroke="${SHIP_COLORS.outline}" stroke-width="4"/>`
            + `<rect x="16" y="20" width="${w - 22}" height="60" rx="30" fill="${SHIP_COLORS.dark}" stroke="${SHIP_COLORS.outline}" stroke-width="4"/>`
            + `<rect x="30" y="44" width="${w - 70}" height="7" rx="3" fill="${SHIP_COLORS.hull}"/>`
            + `<rect x="${w * 0.42}" y="30" width="58" height="40" rx="14" fill="${SHIP_COLORS.hull}" stroke="${SHIP_COLORS.outline}" stroke-width="4"/>`
            + `<line x1="${w * 0.42 + 38}" y1="50" x2="${w * 0.42 + 38}" y2="18" stroke="${SHIP_COLORS.outline}" stroke-width="6" stroke-linecap="round"/>`;
    },
    DESTROYER(w) {
        return standardHull(w) + bridge(36, 52) + turret(136, true);
    }
};

function shipSvg(type, orientation, extraClass) {
    const length = SHIP_LENGTHS[type];
    const drawing = SHIP_DRAWINGS[type];
    if (!length || !drawing) {
        return "";
    }
    const w = length * 100;
    const horizontal = orientation === "HORIZONTAL";
    const viewBox = horizontal ? `0 0 ${w} 100` : `0 0 100 ${w}`;
    const rotate = horizontal ? "" : ` transform="translate(100 0) rotate(90)"`;
    return `<svg class="ship ${extraClass}" viewBox="${viewBox}" preserveAspectRatio="none" aria-hidden="true">`
        + `<g${rotate}>${drawing(w)}</g></svg>`;
}

function placeShipSvg(layer, type, row, col, orientation, extraClass) {
    const length = SHIP_LENGTHS[type];
    if (!length) {
        return;
    }
    const holder = document.createElement("div");
    holder.className = "ship-holder";
    holder.style.top = `${Number(row) * 10}%`;
    holder.style.left = `${Number(col) * 10}%`;
    holder.style.width = orientation === "HORIZONTAL" ? `${length * 10}%` : "10%";
    holder.style.height = orientation === "HORIZONTAL" ? "10%" : `${length * 10}%`;
    holder.innerHTML = shipSvg(type, orientation, extraClass);
    layer.appendChild(holder);
}
