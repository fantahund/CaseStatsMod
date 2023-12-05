package de.fanta.casestats;

import de.cubeside.connection.GlobalConnectionFabricClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class CaseStatsChannelHandler implements ClientPlayNetworking.PlayChannelHandler {

    public static final Identifier CHANNEL_IDENTIFIER = new Identifier("casestats", "data");

    private final CaseStats caseStats;

    public CaseStatsChannelHandler(CaseStats caseStats) {
        this.caseStats = caseStats;
        ClientPlayNetworking.registerGlobalReceiver(CHANNEL_IDENTIFIER, this);
    }

    @Override
    public void receive(MinecraftClient client, ClientPlayNetworkHandler networkHandler, PacketByteBuf packet, PacketSender sender) {
        int caseOpenDataChannelID = 0;
        int loginGlobalClientChannelID = 1;
        try {
            int caseStatsDateChannel = packet.readByte();
            int caseStatsDateChannelVersion = packet.readByte();
            if (caseStatsDateChannel == caseOpenDataChannelID && caseStatsDateChannelVersion == 0) {
                if (packet.hasArray()) {
                    byte[] data = packet.array();
                    caseStats.getGlobalDataHelper().sendCaseData(data);
                }
            }

            if (caseStatsDateChannel == loginGlobalClientChannelID) {
                String uuid = packet.readString();
                String passwort = packet.readString();
                String ipString = packet.readString();
                int port = packet.readInt();
                GlobalConnectionFabricClient.getInstance().loginClientAndWriteConfig(ipString, port, uuid, passwort);
            }
        } catch (Exception e) {
            CaseStats.LOGGER.warn("Unable to read CaseStats data", e);
        }
    }
}

