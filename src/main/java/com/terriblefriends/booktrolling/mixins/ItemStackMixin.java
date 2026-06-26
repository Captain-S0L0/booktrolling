package com.terriblefriends.booktrolling.mixins;

import com.mojang.logging.LogUtils;
import com.terriblefriends.booktrolling.Config;
import com.terriblefriends.booktrolling.ItemSizeResults;
import com.terriblefriends.booktrolling.LongCountingDataOutputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    // magic numbers and constants
    @Unique
    private static final Component WARNING_TEXT = Component.literal(" (WARNING)").withStyle(ChatFormatting.GOLD);
    @Unique
    private static final Component OVERSIZED_TEXT = Component.literal(" (OVERSIZED)").withStyle(ChatFormatting.DARK_RED);
    @Unique
    private static final Component GENERIC_ERROR_TEXT = Component.literal("ERROR CALCULATING ITEM SIZE! SEE LOGS!").withStyle(ChatFormatting.DARK_RED);

    // static finals
    @Unique
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(1);
    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();

    // statics
    @Unique
    private static ItemStack lastCalculatedStack = null;
    @Unique
    private static ItemSizeResults lastResults = null;
    @Unique
    private static Future<ItemSizeResults> currentTask = null;

    // this accessor
    @Unique
    private final ItemStack instance = (ItemStack) (Object) this;

    @Inject(method = "addDetailsToTooltip", at = @At("TAIL"))
    private void booktrolling$handleItemSizeDebug(Item.TooltipContext context, TooltipDisplay displayComponent, Player player, TooltipFlag type, Consumer<Component> textConsumer, CallbackInfo ci) {
        if (!Config.get().itemSizeDebug || this.instance.isEmpty()) {
            return;
        }

        boolean taskStackEqual = lastCalculatedStack != null && (this.instance == lastCalculatedStack || ItemStack.isSameItemSameComponents(this.instance, lastCalculatedStack));

        if (taskStackEqual) {
            // check if current task is done
            if (currentTask != null && currentTask.isDone()) {
                try {
                    lastResults = currentTask.get();
                }
                catch (InterruptedException | ExecutionException e) {
                    LOGGER.error("Failed to get result from future when it should have been done! Reason:", e);
                    textConsumer.accept(GENERIC_ERROR_TEXT);
                    return;
                }
                finally {
                    currentTask = null;
                }
            }

            // if lastResults is null then the thread is still calculating, so hold on
            if (lastResults == null) {
                textConsumer.accept(Component.literal("Calculating...").withStyle(ChatFormatting.RED));
                return;
            }

            appendData(textConsumer, lastResults);
            return;
        }

        // if the requested stack is different then terminate the current task and start a new one
        lastResults = null;
        lastCalculatedStack = this.instance;
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        currentTask = threadPool.submit(() -> {
            Minecraft minecraftClient = Minecraft.getInstance();
            if (minecraftClient == null || minecraftClient.level == null) {
                LOGGER.error("Failed to calculate item size! Reason: MinecraftClient or MinecraftClient.world was null!");
                return new ItemSizeResults(true, -1, -1, -1, -1);
            }

            long rawDiskBytes = -1;
            long compressedDiskBytes = -1;
            int rawPacketBytes = -1;
            int compressedPacketBytes = -1;

            try {
                // calculate disk sizes
                CompoundTag itemTag = (CompoundTag)ItemStack.CODEC.encode(this.instance, minecraftClient.level.registryAccess().createSerializationContext(NbtOps.INSTANCE), new CompoundTag()).getOrThrow();

                LongCountingDataOutputStream compressedDataOutputStream = new LongCountingDataOutputStream(OutputStream.nullOutputStream());
                GZIPOutputStream gzipOutputStream = new GZIPOutputStream(compressedDataOutputStream);
                LongCountingDataOutputStream rawDataOutputStream = new LongCountingDataOutputStream(gzipOutputStream);
                itemTag.write(rawDataOutputStream);
                gzipOutputStream.finish();
                // allow garbage collection of item tag
                itemTag = null;

                rawDiskBytes = rawDataOutputStream.size();
                compressedDiskBytes = compressedDataOutputStream.size();

                // calculate packet sizes
                RegistryFriendlyByteBuf packetBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), minecraftClient.level.registryAccess());

                ItemStack.STREAM_CODEC.encode(packetBuf, this.instance);
                rawPacketBytes = packetBuf.readableBytes();

                // 256 bytes is the default compression threshold
                if (rawPacketBytes < 256) {
                    compressedPacketBytes = rawPacketBytes + 1;
                } else {
                    FriendlyByteBuf compressionBuf = new FriendlyByteBuf(Unpooled.buffer());
                    byte[] deflateBuffer = new byte[8192];
                    Deflater deflater = new Deflater();

                    byte[] bs = new byte[rawPacketBytes];
                    packetBuf.readBytes(bs);
                    VarInt.write(compressionBuf, bs.length);

                    deflater.setInput(bs, 0, bs.length);
                    deflater.finish();

                    while(!deflater.finished()) {
                        int j = deflater.deflate(deflateBuffer);
                        compressionBuf.writeBytes(deflateBuffer, 0, j);
                    }

                    compressedPacketBytes = compressionBuf.readableBytes();
                }

                return new ItemSizeResults(false, rawDiskBytes, compressedDiskBytes, rawPacketBytes, compressedPacketBytes);
            } catch (Throwable t) {
                LOGGER.error("Failed to calculate item size! Reason:", t);
                return new ItemSizeResults(true, -1, -1, -1, -1);
            }
        });
    }

    @Unique
    private static void optionalAppendLabel(MutableComponent text, long value, long max, long warningThreshold) {
        if (value >= max) {
            text.append(OVERSIZED_TEXT);
        }
        else if (value >= max - warningThreshold) {
            text.append(WARNING_TEXT);
        }
    }

    @Unique
    private static void appendData(Consumer<Component> textConsumer, ItemSizeResults results) {
        if (results.error()) {
            textConsumer.accept(GENERIC_ERROR_TEXT);
            return;
        }

        MutableComponent line;

        textConsumer.accept(Component.literal("DISK SIZES").withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE));
        textConsumer.accept(Component.literal("Raw: " + toReadableNumber(results.diskSize())).withStyle(ChatFormatting.RED));

        line = Component.literal("Compressed: " + toReadableNumber(results.diskSizeCompressed())).withStyle(ChatFormatting.RED);
        // chunks cannot save if they contain more than the array limit of bytes after compression as in the
        // chunk saving process, the bytes are sent to a byte array before being flushed to disk
        // arrays are limited to approximately Integer.MAX_VALUE - 8 elements, JVM dependent.

        // a warning of 10 MB should be fine
        optionalAppendLabel(line, results.diskSizeCompressed(), Integer.MAX_VALUE - 8, 10485760);
        textConsumer.accept(line);

        textConsumer.accept(Component.literal("PACKET SIZES").withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE));

        line = Component.literal("Raw: " + toReadableNumber(results.packetSize())).withStyle(ChatFormatting.RED);
        // packets cannot have more than 8388608 bytes of raw data (net.minecraft.network.handler.PacketDeflater)

        // a warning of 128 KB should be fine
        optionalAppendLabel(line, results.packetSize(), 8388608, 128000);
        textConsumer.accept(line);

        line = Component.literal("Compressed: " + toReadableNumber(results.packetSizeCompressed())).withStyle(ChatFormatting.RED);
        // packets cannot have more than 2097152 bytes of compressed data (net.minecraft.network.handler.SizePrepender)

        // a warning of 128 KB should be fine
        optionalAppendLabel(line, results.packetSizeCompressed(), 2097152, 128000);
        textConsumer.accept(line);
    }

    @Unique
    private static String toReadableNumber(long value) {
        if (Config.get().itemSizeDebugRawSizes) {
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
