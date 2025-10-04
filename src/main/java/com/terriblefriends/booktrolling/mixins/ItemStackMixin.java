package com.terriblefriends.booktrolling.mixins;

import com.mojang.logging.LogUtils;
import com.terriblefriends.booktrolling.Config;
import com.terriblefriends.booktrolling.ItemSizeResults;
import com.terriblefriends.booktrolling.LongCountingDataOutputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.encoding.VarInts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
    private static final Text WARNING_TEXT = Text.literal(" (WARNING)").formatted(Formatting.GOLD);
    @Unique
    private static final Text OVERSIZED_TEXT = Text.literal(" (OVERSIZED)").formatted(Formatting.DARK_RED);
    @Unique
    private static final Text GENERIC_ERROR_TEXT = Text.literal("ERROR CALCULATING ITEM SIZE! SEE LOGS!").formatted(Formatting.DARK_RED);

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

    @Inject(method = "appendTooltip", at = @At("TAIL"))
    private void booktrolling$handleItemSizeDebug(Item.TooltipContext context, TooltipDisplayComponent displayComponent, PlayerEntity player, TooltipType type, Consumer<Text> textConsumer, CallbackInfo ci) {
        if (!Config.get().itemSizeDebug || this.instance.isEmpty()) {
            return;
        }

        boolean taskStackEqual = lastCalculatedStack != null && (this.instance == lastCalculatedStack || ItemStack.areItemsAndComponentsEqual(this.instance, lastCalculatedStack));

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
                textConsumer.accept(Text.literal("Calculating...").formatted(Formatting.RED));
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
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            if (minecraftClient == null || minecraftClient.world == null) {
                LOGGER.error("Failed to calculate item size! Reason: MinecraftClient or MinecraftClient.world was null!");
                return new ItemSizeResults(true, -1, -1, -1, -1);
            }

            long rawDiskBytes = -1;
            long compressedDiskBytes = -1;
            int rawPacketBytes = -1;
            int compressedPacketBytes = -1;

            try {
                // calculate disk sizes
                NbtCompound itemTag = (NbtCompound)ItemStack.CODEC.encode(this.instance, minecraftClient.world.getRegistryManager().getOps(NbtOps.INSTANCE), new NbtCompound()).getOrThrow();

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
                RegistryByteBuf packetBuf = new RegistryByteBuf(Unpooled.buffer(), minecraftClient.world.getRegistryManager());

                ItemStack.PACKET_CODEC.encode(packetBuf, this.instance);
                rawPacketBytes = packetBuf.readableBytes();

                // 256 bytes is the default compression threshold
                if (rawPacketBytes < 256) {
                    compressedPacketBytes = rawPacketBytes + 1;
                } else {
                    PacketByteBuf compressionBuf = new PacketByteBuf(Unpooled.buffer());
                    byte[] deflateBuffer = new byte[8192];
                    Deflater deflater = new Deflater();

                    byte[] bs = new byte[rawPacketBytes];
                    packetBuf.readBytes(bs);
                    VarInts.write(compressionBuf, bs.length);

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
    private static void optionalAppendLabel(MutableText text, long value, long max, long warningThreshold) {
        if (value >= max) {
            text.append(OVERSIZED_TEXT);
        }
        else if (value >= max - warningThreshold) {
            text.append(WARNING_TEXT);
        }
    }

    @Unique
    private static void appendData(Consumer<Text> textConsumer, ItemSizeResults results) {
        if (results.error()) {
            textConsumer.accept(GENERIC_ERROR_TEXT);
            return;
        }

        MutableText line;

        textConsumer.accept(Text.literal("DISK SIZES").formatted(Formatting.RED, Formatting.UNDERLINE));
        textConsumer.accept(Text.literal("Raw: " + toReadableNumber(results.diskSize())).formatted(Formatting.RED));

        line = Text.literal("Compressed: " + toReadableNumber(results.diskSizeCompressed())).formatted(Formatting.RED);
        // chunks cannot save if they contain more than the array limit of bytes after compression as in the
        // chunk saving process, the bytes are sent to a byte array before being flushed to disk
        // arrays are limited to approximately Integer.MAX_VALUE - 8 elements, JVM dependent.

        // a warning of 10 MB should be fine
        optionalAppendLabel(line, results.diskSizeCompressed(), Integer.MAX_VALUE - 8, 10485760);
        textConsumer.accept(line);

        textConsumer.accept(Text.literal("PACKET SIZES").formatted(Formatting.RED, Formatting.UNDERLINE));

        line = Text.literal("Raw: " + toReadableNumber(results.packetSize())).formatted(Formatting.RED);
        // packets cannot have more than 8388608 bytes of raw data (net.minecraft.network.handler.PacketDeflater)

        // a warning of 128 KB should be fine
        optionalAppendLabel(line, results.packetSize(), 8388608, 128000);
        textConsumer.accept(line);

        line = Text.literal("Compressed: " + toReadableNumber(results.packetSizeCompressed())).formatted(Formatting.RED);
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
