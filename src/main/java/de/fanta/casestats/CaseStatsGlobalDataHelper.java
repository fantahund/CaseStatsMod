package de.fanta.casestats;

import de.cubeside.connection.GlobalServer;
import de.fanta.casestats.globaldata.GlobalDataHelperFabric;

import java.io.DataInputStream;
import java.io.IOException;

public class CaseStatsGlobalDataHelper extends GlobalDataHelperFabric<CaseStatsMessageType> {

    public static final String CHANNEL = "CaseStats";

    public CaseStatsGlobalDataHelper() {
        super(CaseStatsMessageType.class, CHANNEL);
    }

    public void sendCaseData(byte[] data) {
        sendData(CHANNEL, data);
    }

    @Override
    protected void handleMessage(CaseStatsMessageType messageType, GlobalServer source, DataInputStream data) throws IOException {

    }

}
