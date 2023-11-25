package de.fanta.casestats;

import de.cubeside.connection.ConnectionAPI;
import de.cubeside.connection.GlobalClientFabric;
import de.fanta.casestats.data.Database;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class CaseStats implements ClientModInitializer {
    private static boolean openCase = false;
    private static ItemStack lastUseItem;
    private static Map<String, String> config = new HashMap<>();

    public static final String MODID = "CaseStats";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    private static Database DATABASE;
    private ConnectionAPI connectionAPI;
    private boolean useConnectionAPI = false;
    @Override
    public void onInitializeClient() {
        connectionAPI = GlobalClientFabric.getInstance();
        if (connectionAPI != null) {
            useConnectionAPI = true;
        }
        config = createAndGetConfig();
        DATABASE = new Database();

        Events events = new Events();
        events.init();

        new CaseStatsChannelHandler();
    }

    public static void setOpenCase(boolean value) {
        openCase = value;
    }

    public static boolean isOpenCase() {
        return openCase;
    }

    public static void setLastUseItem(ItemStack lastUseItem) {
        CaseStats.lastUseItem = lastUseItem;
    }
    public static ItemStack getLastUseItem() {
        return lastUseItem;
    }

    private static HashMap<String, String> createAndGetConfig() {
        HashMap<String, String> data = new HashMap<>();
        data.put("host", "136.243.5.245");
        data.put("user", "casestats");
        data.put("password", "K66XLgJPB5RgUvvH");
        data.put("database", "casestats");
        data.put("tableprefix", "casestats");
        return data;
    }

    public static Map<String, String> getConfig() {
        return config;
    }

    public static Database getDatabase() {
        return DATABASE;
    }

    public ConnectionAPI getConnectionAPI() {
        return connectionAPI;
    }

    public boolean isUseConnectionAPI() {
        return useConnectionAPI;
    }
}
