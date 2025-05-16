package com.terriblefriends.booktrolling;

import net.fabricmc.api.ClientModInitializer;

public class BookTrolling implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Config.load();

        Runtime.getRuntime().addShutdownHook(new Thread(Config::save));
    }
}
