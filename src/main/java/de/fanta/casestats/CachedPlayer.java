package de.fanta.casestats;

import java.util.UUID;

public class CachedPlayer {
    private final UUID uuid;
    private final String name;

    public CachedPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }
}
