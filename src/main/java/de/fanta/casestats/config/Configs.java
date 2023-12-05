package de.fanta.casestats.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.fanta.casestats.CaseStats;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import fi.dy.masa.malilib.util.JsonUtils;
import net.minecraft.client.MinecraftClient;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class Configs implements IConfigHandler {

    private static final String CONFIG_FILE_NAME = "casestats.json";
    private static final int CONFIG_VERSION = 1;

    public static class Generic {
        public static final ConfigStringList StatsPlayer = new ConfigStringList("StatsPlayer", ImmutableList.of(MinecraftClient.getInstance().getGameProfile().getName()), "Get Stats from Player");
        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                StatsPlayer
        );
    }

    public static void loadFromFile() {
        File oldConfigFile = new File(fi.dy.masa.malilib.util.FileUtils.getConfigDirectory(), CONFIG_FILE_NAME);
        File configFile = new File(CaseStats.getConfigDirectory(), CONFIG_FILE_NAME);
        if (oldConfigFile.exists()) {
            try {
                FileUtils.moveFile(oldConfigFile, configFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (!configFile.exists()) {
            saveToFile();
        }

        if (configFile.exists() && configFile.isFile() && configFile.canRead()) {
            JsonElement element = JsonUtils.parseJsonFile(configFile);

            if (element != null && element.isJsonObject()) {
                JsonObject root = element.getAsJsonObject();
                ConfigUtils.readConfigBase(root, "Generic", Generic.OPTIONS);
            }
        }
    }

    public static void saveToFile() {
        File dir = CaseStats.getConfigDirectory();

        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs()) {
            JsonObject root = new JsonObject();

            ConfigUtils.writeConfigBase(root, "Generic", Generic.OPTIONS);

            root.add("config_version", new JsonPrimitive(CONFIG_VERSION));

            JsonUtils.writeJsonToFile(root, new File(dir, CONFIG_FILE_NAME));

            CaseStats.LOGGER.info("[CaseStats] Config Saved");
        }
    }

    @Override
    public void load() {
        loadFromFile();
    }

    @Override
    public void save() {
        saveToFile();
    }

}
