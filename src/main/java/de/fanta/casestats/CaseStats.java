package de.fanta.casestats;

import de.cubeside.connection.ConnectionAPI;
import de.cubeside.connection.GlobalClientFabric;
import de.cubeside.connection.event.GlobalServerConnectedCallback;
import de.fanta.casestats.config.Configs;
import fi.dy.masa.malilib.util.FileUtils;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class CaseStats implements ClientModInitializer {
    private static CaseStats INSTANCE;
    public static final String MODID = "CaseStats";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    private ConnectionAPI connectionAPI;
    private CaseStatsGlobalDataRequestManager globalDataRequestManager;
    private CaseStatsGlobalDataHelper globalDataHelper;
    private static File configDirectory;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        configDirectory = new File(FileUtils.getConfigDirectory().getPath() + "/CaseStats");
        if (!configDirectory.isDirectory()) {
            configDirectory.mkdirs();
        }
        Configs.loadFromFile();

        new CaseStatsChannelHandler();
        GlobalServerConnectedCallback.EVENT.register(server -> {
            if (server.getName().equals(MinecraftClient.getInstance().getGameProfile().getId().toString())) {
                onConnectGlobalClient();
            }
        });
    }

    public void onConnectGlobalClient() {
        connectionAPI = GlobalClientFabric.getInstance();
        globalDataRequestManager = new CaseStatsGlobalDataRequestManager();
        globalDataHelper = new CaseStatsGlobalDataHelper();
    }

    public static CaseStats getInstance() {
        return INSTANCE;
    }

    public ConnectionAPI getConnectionAPI() {
        return connectionAPI;
    }

    public CaseStatsGlobalDataRequestManager getGlobalDataRequestManager() {
        return globalDataRequestManager;
    }

    public CaseStatsGlobalDataHelper getGlobalDataHelper() {
        return globalDataHelper;
    }

    public static File getConfigDirectory() {
        return configDirectory;
    }
}
