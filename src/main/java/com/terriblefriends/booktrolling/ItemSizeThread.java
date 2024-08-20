package com.terriblefriends.booktrolling;

import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;

import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.Deflater;

public class ItemSizeThread extends Thread {

    public ItemSizeThread(ItemStack stack) {
        this.stack = stack;
    }

    private final ItemStack stack;
    private final Deflater deflater = new Deflater();
    private final byte[] deflateBuffer = new byte[8192];

    private final Results results = new Results();
    public Results getResults() {
        return results;
    }

    @Override
    public void run() {
        try {
            // write item to packet byte buf for raw packet size
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeItemStack(this.stack);
            this.results.packetSize = buf.readableBytes();

            // read item from packet byte buf for nbt size tracker

            // is stack empty (ignore)
            buf.readBoolean();
            // read item
            buf.readRegistryValue(Registries.ITEM);
            // read count
            buf.readByte();

            AtomicLong nbtSize = new AtomicLong(0);

            NbtSizeTracker tracker = new NbtSizeTracker(Long.MAX_VALUE,Integer.MAX_VALUE) {
                public void add(long bytes) {
                    nbtSize.addAndGet(bytes);
                }
            };

            buf.readNbt(tracker);

            this.results.nbtSize = nbtSize.get();

            // compress and read the size

            PacketByteBuf compressionBuf = new PacketByteBuf(Unpooled.buffer());
            if (this.results.packetSize > 0 && this.results.packetSize <= 2147483645) {
                buf.resetReaderIndex();

                byte[] bs = buf.array();
                compressionBuf.writeVarInt(bs.length);
                deflater.setInput(bs, 0, (int) this.results.packetSize);
                deflater.finish();

                while (!this.deflater.finished()) {
                    int j = this.deflater.deflate(this.deflateBuffer);
                    compressionBuf.writeBytes(this.deflateBuffer, 0, j);
                }

                this.deflater.reset();

                this.results.compressedSize = compressionBuf.readableBytes();
            }
        } catch (Exception e) {
            this.results.error = true;
            LogUtils.getLogger().error("Error calculating stack size!", e);
        }
    }

    public static class Results {
        public long packetSize = -1;
        public long nbtSize = -1;
        public long compressedSize = -1;
        public boolean error = false;
    }

}
