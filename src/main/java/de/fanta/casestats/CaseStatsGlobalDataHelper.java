package de.fanta.casestats;

import de.cubeside.connection.GlobalServer;
import de.fanta.casestats.globaldata.GlobalDataHelperFabric;
import de.fanta.casestats.globaldata.StringSerializable;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CaseStatsGlobalDataHelper extends GlobalDataHelperFabric<CaseStatsMessageType> {

    public static final String CHANNEL = "CaseStats";

    public static interface DataMessageHandler {

        public abstract void handleMessage(CaseStatsMessageType messageType, DataInputStream data) throws IOException;
    }

    private Map<CaseStatsMessageType, List<DataMessageHandler>> messageHandlers;

    public CaseStatsGlobalDataHelper() {
        super(CaseStatsMessageType.class, CHANNEL);

        this.messageHandlers = new EnumMap<>(CaseStatsMessageType.class);
    }

    public void registerHandler(CaseStatsMessageType messageType, DataMessageHandler handler) {
        this.messageHandlers.computeIfAbsent(messageType, type -> new ArrayList<>()).add(handler);
    }

    @Override
    public UUID readUUID(DataInputStream msgin) throws IOException {
        return super.readUUID(msgin);
    }

    @Override
    public <S extends StringSerializable> S readStringSerializable(DataInputStream msgin) throws IOException {
        return super.readStringSerializable(msgin);
    }

    @Override
    protected void handleMessage(CaseStatsMessageType messageType, GlobalServer source, DataInputStream data) throws IOException {
        List<DataMessageHandler> handlers = this.messageHandlers.getOrDefault(messageType, Collections.emptyList());
        for (DataMessageHandler handler : handlers) {
            handler.handleMessage(messageType, data);
        }
    }
}
