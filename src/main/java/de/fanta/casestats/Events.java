package de.fanta.casestats;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.apache.logging.log4j.Level;

import java.sql.SQLException;

public class Events {

    public Events() {
    }

    public void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            try {
                CaseStats.getDatabase().insertUUIDPlayer(new CachedPlayer(client.player.getUuid(), client.player.getName().getString()));
            } catch (SQLException e) {
                CaseStats.LOGGER.log(Level.ERROR, "Cached Player konnte nicht gesichert werden!", e);
            }
        });
    }
}
