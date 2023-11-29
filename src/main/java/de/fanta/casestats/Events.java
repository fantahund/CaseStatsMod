package de.fanta.casestats;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.apache.logging.log4j.Level;

import java.sql.SQLException;

public class Events {

    public Events() {
    }

    public void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {

        });
    }
}
