package de.fanta.casestats.globaldata;

import de.cubeside.connection.event.GlobalDataCallback;
import de.fanta.casestats.CaseStats;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public abstract class GlobalDataHelperFabric<T extends Enum<T>> extends GlobalDataHelperImpl<T> {

    public GlobalDataHelperFabric(Class<T> messageTypeClass, String channel) {
        super(messageTypeClass, channel);
        loadEvents();
    }

    private void loadEvents() {
        CaseStats.LOGGER.info("Register Callback for Channel: " + getChannel());
        GlobalDataCallback.EVENT.register((source, targetPlayer, channel, data) -> {
            CaseStats.LOGGER.info("Input from message: " + channel);
            if (!channel.equals(getChannel())) {
                return;
            }


            try {
                DataInputStream newData = new DataInputStream(new ByteArrayInputStream(data));
                T messageType = fromOrdinal(newData.readInt());
                CaseStats.LOGGER.info("  MessageType: " + messageType);
                handleMessage(messageType, source, newData);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

}
