package com.flansmod.warforge.compat;

import java.util.UUID;

/** A known player (from the server's name/UUID cache). */
public record PlayerRef(UUID id, String name) {
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
