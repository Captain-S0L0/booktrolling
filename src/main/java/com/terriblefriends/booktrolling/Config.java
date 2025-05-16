package com.terriblefriends.booktrolling;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.*;

public class Config {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final File file = new File(FabricLoader.getInstance().getConfigDir().toFile(), "booktrolling.json");
    private static Config INSTANCE = new Config();

    public boolean itemSizeDebugRawSizes = false;
    public boolean itemSizeDebug = false;
    public boolean autoSign = false;
    public boolean autoDrop = false;
    public boolean randomizeCharacters = false;

    public static void load() {
        Gson gson = new Gson();

        try (Reader reader = new FileReader(file)) {
            INSTANCE = gson.fromJson(reader, Config.class);
        } catch (Exception e) {
            if (file.exists()) {
                LOGGER.error("BookTrolling could not load the config! Using defaults.", e);
            } else {
                LOGGER.info("BookTrolling could not find a config, so we're creating a new one.");
            }
        }
    }

    public static void save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (FileWriter writer = new FileWriter(file)) {

            gson.toJson(INSTANCE, writer);
        } catch (IOException e) {
            LOGGER.error("BookTrolling failed to save the config!", e);
        }
    }

    public static Config get() {
        return INSTANCE;
    }
}
