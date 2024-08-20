package com.terriblefriends.booktrolling.mixins;

import com.mojang.logging.LogUtils;
import com.terriblefriends.booktrolling.Booktrolling;
import com.terriblefriends.booktrolling.ItemSizeThread;
import net.minecraft.client.item.TooltipType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.encoding.VarInts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Mixin(Item.class)
public class ItemMixin {
    @Unique
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(1);
    @Unique
    private static ItemStack lastStack = null;
    @Unique
    private static ItemSizeThread.Results oldResults = null;
    @Unique
    private static Future<?> currentTask = null;
    @Unique
    private static ItemSizeThread currentThread = null;

    @Unique
    private static final int WARNING_THRESHOLD = 8192;
    @Unique
    private static final int PACKET_RAW_LIMIT = 8388608;
    @Unique
    private static final int NBT_SIZE_TRACKER_LIMIT = 2097152;

    @Unique
    private static final Text WARNING_TEXT = Text.literal(" (WARNING)").formatted(Formatting.GOLD);
    @Unique
    private static final Text OVERSIZED_TEXT = Text.literal(" (OVERSIZED)").formatted(Formatting.DARK_RED);

    @Inject(at=@At("HEAD"),method = "Lnet/minecraft/item/Item;appendTooltip(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/Item$TooltipContext;Ljava/util/List;Lnet/minecraft/client/item/TooltipType;)V")
    private void booktrolling$handleItemSizeDebug(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type, CallbackInfo ci) {
        if (!Booktrolling.itemSizeDebug || stack.isEmpty()) {
            return;
        }

        if (oldResults != null && ItemStack.areEqual(lastStack, stack)) {
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
            lastStack = stack;
            oldResults = null;
            currentThread = new ItemSizeThread(stack);
            currentTask = threadPool.submit(currentThread);
        }
    }

    @Unique
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

    @Unique
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
