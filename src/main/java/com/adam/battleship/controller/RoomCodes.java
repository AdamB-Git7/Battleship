package com.adam.battleship.controller;

final class RoomCodes {
    private RoomCodes() {
    }

    static String normalize(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Enter a room code");
        }
        return code.trim().toUpperCase();
    }
}
