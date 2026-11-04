package com.adam.battleship.config;

import java.security.Principal;

public record PlayerPrincipal(String id) implements Principal {
    @Override
    public String getName() {
        return id;
    }
}
