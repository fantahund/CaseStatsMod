package de.fanta.casestats.packets;

import de.fanta.casestats.CaseStats;
import de.fanta.casestats.MinecraftDataOutputStream;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.io.ByteArrayOutputStream;

public class CaseStatsDataS2C implements CustomPayload {
    public static final Id<CaseStatsDataS2C> PACKET_ID = new Id<>(Identifier.of("casestats", "data"));
    public static final PacketCodec<PacketByteBuf, CaseStatsDataS2C> PACKET_CODEC = PacketCodec.of(CaseStatsDataS2C::write, CaseStatsDataS2C::new);

    private ServerCaseData serverCaseData;
    private GlobalServerLogin globalServerLogin;

    public CaseStatsDataS2C(PacketByteBuf packet) {
        int caseOpenDataChannelID = 0;
        int loginGlobalClientChannelID = 1;

        try {
            int caseStatsDateChannel = packet.readByte();
            int caseStatsDateChannelVersion = packet.readByte();
            if (caseStatsDateChannel == caseOpenDataChannelID && caseStatsDateChannelVersion == 0) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                MinecraftDataOutputStream outputStream = new MinecraftDataOutputStream(out);

                String caseID = packet.readString();
                String caseItem = packet.readString();
                String caseItemString = packet.readString();

                int itemsSize = packet.readInt();

                outputStream.writeByte(caseStatsDateChannel);
                outputStream.writeByte(caseStatsDateChannelVersion);

                outputStream.writeString(caseID);
                outputStream.writeString(caseItem);
                outputStream.writeString(caseItemString);
                outputStream.writeInt(itemsSize);


                for (int i = 0; i < itemsSize; i++) {
                    String item = packet.readString();
                    int amount = packet.readInt();
                    String itemNBT = packet.readString();

                    outputStream.writeString(item);
                    outputStream.writeInt(amount);
                    outputStream.writeString(itemNBT);
                }
                outputStream.flush();
                serverCaseData = new ServerCaseData(out.toByteArray());
            }

            if (caseStatsDateChannel == loginGlobalClientChannelID) {
                String uuid = packet.readString();
                String passwort = packet.readString();
                String ipString = packet.readString();
                int port = packet.readInt();
                globalServerLogin = new GlobalServerLogin(uuid, passwort, ipString, port);
            }
        } catch (Exception e) {
            CaseStats.LOGGER.warn("Unable to read CaseStats data", e);
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

    public void write(PacketByteBuf buf) {
        // nix write
    }

    public ServerCaseData getServerCaseData() {
        return serverCaseData;
    }

    public GlobalServerLogin getGlobalServerLogin() {
        return globalServerLogin;
    }
}
