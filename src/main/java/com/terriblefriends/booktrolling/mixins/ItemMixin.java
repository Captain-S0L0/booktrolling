package com.terriblefriends.booktrolling.mixins;

import com.mojang.logging.LogUtils;
import com.terriblefriends.booktrolling.Booktrolling;
import com.terriblefriends.booktrolling.ItemSizeThread;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
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
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(1);
    private static ItemStack oldStack = null;
    private static ItemSizeThread.Results oldResults = null;
    private static Future currentTask = null;
    private static ItemSizeThread currentThread = null;

    @Inject(at=@At("HEAD"),method = "Lnet/minecraft/item/Item;appendTooltip(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Ljava/util/List;Lnet/minecraft/client/item/TooltipContext;)V")
    private void booktrolling$handleItemSizeDebug(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context, CallbackInfo ci) {
        if (!Booktrolling.itemSizeDebug || stack.isEmpty()) {
            return;
        }

        if (oldResults != null && ItemStack.areEqual(oldStack, stack)) {
            appendData(tooltip, oldResults);
            return;
        }
        if (currentTask != null) {
            if (!currentTask.isDone()) {
                tooltip.add(Text.literal("Calculating...").formatted(Formatting.RED));
                return;
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
                return;
            }
        }
        else {
            oldStack = stack;
            oldResults = null;
            currentThread = new ItemSizeThread(stack);
            currentTask = threadPool.submit(currentThread);
            return;
        }
    }

    private static void appendData(List<Text> tooltip, ItemSizeThread.Results results) {
        if (results.error) {
            tooltip.add(Text.literal("ERROR CALCULATING SIZE! See logs").formatted(Formatting.DARK_RED));
        }
        else {
            if (results.byteSize > 8388608) {
                tooltip.add(Text.literal("BYTES: " + results.byteSize).formatted(Formatting.RED).append(Text.literal(" (OVERSIZED)").formatted(Formatting.DARK_RED)));
            }
            else {
                tooltip.add(Text.literal("BYTES: " + results.byteSize).formatted(Formatting.RED));
            }

            if (results.nbtSize > 2097152) {
                tooltip.add(Text.literal("NBT: " + results.nbtSize).formatted(Formatting.RED).append(Text.literal(" (OVERSIZED)").formatted(Formatting.DARK_RED)));
            }
            else {
                tooltip.add(Text.literal("NBT: " + results.nbtSize).formatted(Formatting.RED));
            }

            if (results.moreThanIntLimit) {
                tooltip.add(Text.literal("UNCOMPRESSIBLE, AKA > 2.147 Gigabytes raw. Are you sure this is a good idea?").formatted(Formatting.DARK_RED));
            }
            else if (results.uncompressible) {
                tooltip.add(Text.literal("COMPRESS: "+results.compressedSize).formatted(Formatting.RED).append(Text.literal(" (OVERSIZED)").formatted(Formatting.DARK_RED)));
            }
            else {
                tooltip.add(Text.literal("COMPRESS: "+results.compressedSize).formatted(Formatting.RED));
            }
        }
    }
}
