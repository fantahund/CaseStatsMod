package de.fanta.casestats;

import de.cubeside.connection.GlobalConnectionFabricClient;
import de.fanta.casestats.packets.CaseStatsDataS2C;
import de.fanta.casestats.packets.GlobalServerLogin;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class CaseStatsChannelHandler implements ClientPlayNetworking.PlayPayloadHandler<CaseStatsDataS2C>, ClientConfigurationNetworking.ConfigurationPayloadHandler<CaseStatsDataS2C> {
    ;

    public CaseStatsChannelHandler() {
        PayloadTypeRegistry.playS2C().register(CaseStatsDataS2C.PACKET_ID, CaseStatsDataS2C.PACKET_CODEC);
        PayloadTypeRegistry.configurationS2C().register(CaseStatsDataS2C.PACKET_ID, CaseStatsDataS2C.PACKET_CODEC);

        ClientPlayNetworking.registerGlobalReceiver(CaseStatsDataS2C.PACKET_ID, this);
        ClientConfigurationNetworking.registerGlobalReceiver(CaseStatsDataS2C.PACKET_ID, this);
    }

    @Override
    public void receive(CaseStatsDataS2C payload, ClientConfigurationNetworking.Context context) {
        receive(payload);
    }

    @Override
    public void receive(CaseStatsDataS2C payload, ClientPlayNetworking.Context context) {
        receive(payload);
    }

    public void receive(CaseStatsDataS2C data) {
        if (data.getServerCaseData() != null) {
            CaseStats.getInstance().getGlobalDataHelper().sendCaseData(data.getServerCaseData().data());
        }
        if (data.getGlobalServerLogin() != null) {
            GlobalServerLogin login = data.getGlobalServerLogin();
            GlobalConnectionFabricClient.getInstance().loginClientAndWriteConfig(login.ipString(), login.port(), login.uuid(), login.passwort());
        }
    }
}

