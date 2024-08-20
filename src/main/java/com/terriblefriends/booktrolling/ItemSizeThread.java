package com.terriblefriends.booktrolling;

import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
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
    private static final MinecraftClient MINECRAFT = MinecraftClient.getInstance();

    public Results getResults() {
        return results;
    }

    @Override
    public void run() {
        try {
            if (MINECRAFT.world == null) {
                return;
            }

            // write item to packet byte buf for raw packet size

            RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), MINECRAFT.world.getRegistryManager());
            ItemStack.OPTIONAL_PACKET_CODEC.encode(buf, this.stack);
            this.results.packetSize = buf.readableBytes();

            // read item from packet byte buf for nbt size tracker
            // I don't believe this is really important anymore with the migration to components so I'm not gonna fix it ¯\_(ツ)_/¯

            /*
            ItemStack.OPTIONAL_PACKET_CODEC.decode(buf);

            AtomicLong nbtSize = new AtomicLong(0);

            NbtSizeTracker tracker = new NbtSizeTracker(Long.MAX_VALUE,Integer.MAX_VALUE) {
                public void add(long bytes) {
                    nbtSize.addAndGet(bytes);
                }
            };

            buf.readNbt(tracker);

            this.results.nbtSize = nbtSize.get();

            */

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

            this.results.error = false;
        } catch (Exception e) {
            LogUtils.getLogger().error("Error calculating stack size!", e);
        }
    }

    public static class Results {
        public long packetSize = -1;
        public long compressedSize = -1;
        public boolean error = true;
    }

}
