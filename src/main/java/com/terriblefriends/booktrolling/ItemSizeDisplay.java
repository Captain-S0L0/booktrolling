package com.terriblefriends.booktrolling;

import com.mojang.logging.LogUtils;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.encoding.VarInts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ItemSizeDisplay {
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(1);
    private static ItemStack lastStack = null;
    private static ItemSizeThread.Results oldResults = null;
    private static Future<?> currentTask = null;
    private static ItemSizeThread currentThread = null;

    private static final int WARNING_THRESHOLD = 8192;
    private static final int PACKET_RAW_LIMIT = 8388608;
    private static final int NBT_SIZE_TRACKER_LIMIT = 2097152;

    private static final Text WARNING_TEXT = Text.literal(" (WARNING)").formatted(Formatting.GOLD);
    private static final Text OVERSIZED_TEXT = Text.literal(" (OVERSIZED)").formatted(Formatting.DARK_RED);
    public void handleItemSizeDebug(ItemStack itemStack, Item.TooltipContext tooltipContext, TooltipType tooltipType, List<Text> tooltip){
        if (!Booktrolling.itemSizeDebug || itemStack.isEmpty()) {
            return;
        }

        if (oldResults != null && ItemStack.areEqual(lastStack, itemStack)) {
            appendData(tooltip, oldResults);

            return;
        }
        if (currentTask != null) {
            if (!currentTask.isDone()) {
                tooltip.add(Text.literal("Calculating...").formatted(Formatting.RED));
            }
            else {
                oldResults = currentThread.getResults();
                try {
                    currentTask.get();
                }
                catch (ExecutionException | InterruptedException e) {
                    LogUtils.getLogger().error("Error calculating item size!", e);
                    oldResults.error = true;
                }
                appendData(tooltip, oldResults);
                currentTask = null;
                currentThread = null;
            }
        }
        else {
            lastStack = itemStack;
            oldResults = null;
            currentThread = new ItemSizeThread(itemStack);
            currentTask = threadPool.submit(currentThread);
        }
    }
    private static void appendData(List<Text> tooltip, ItemSizeThread.Results results) {
        if (results.error) {
            tooltip.add(Text.literal("ERROR CALCULATING SIZE! See logs!").formatted(Formatting.DARK_RED));
            return;
        }

        // limit imposed by PacketEncoder.encode()

        MutableText line;
        line = Text.literal(String.format("RAW: %s", toReadableNumber(results.packetSize))).formatted(Formatting.RED);
        if (results.packetSize > PACKET_RAW_LIMIT)
            line.append(OVERSIZED_TEXT);
        else if (results.packetSize > PACKET_RAW_LIMIT - WARNING_THRESHOLD) {
            line.append(WARNING_TEXT);
        }
        tooltip.add(line);

        // limit imposed by SizePrepender.encode() in ClientConnection

        if (results.compressedSize < 0) {
            tooltip.add(Text.literal("UNCOMPRESSIBLE, AKA > 2.147 Gigabytes raw. Are you sure this is a good idea?").formatted(Formatting.DARK_RED));
        }
        else {
            line = Text.literal(String.format("COMPRESSED: %s", toReadableNumber(results.compressedSize))).formatted(Formatting.RED);

            if (results.compressedSize > Integer.MAX_VALUE || VarInts.getSizeInBytes((int)results.compressedSize) > 3) {
                line.append(OVERSIZED_TEXT);
            }
            else if (VarInts.getSizeInBytes((int)results.compressedSize + WARNING_THRESHOLD) > 3) {
                line.append(WARNING_TEXT);
            }
        }
        tooltip.add(line);
    }

    private static String toReadableNumber(long value) {
        if (Booktrolling.rawSizes) {
            return value + " bytes";
        }

        if (value >= 1073741824) {
            return String.format("%.2f GB", value / 1073741824D);
        }
        if (value >= 1048576) {
            return String.format("%.2f MB", value / 1048576D);
        }
        if (value >= 1024) {
            return String.format("%.2f KB", value / 1024D);
        }
        return value + " bytes";
    }
}
