package com.terriblefriends.booktrolling;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

public class Booktrolling implements ModInitializer {
    public static boolean itemSizeDebug = false;
    public static boolean rawSizes = false;
    public static ItemSizeDisplay sizeDisplay;

    @Override
    public void onInitialize() {
        sizeDisplay = new ItemSizeDisplay();
        ItemTooltipCallback.EVENT.register((itemStack, tooltipContext, tooltipType, list) -> {
            sizeDisplay.handleItemSizeDebug(itemStack, tooltipContext, tooltipType, list);
        });
    }
}
