package com.terriblefriends.booktrolling;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtTagSizeTracker;
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
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeItemStack(this.stack);
            this.results.byteSize = buf.readableBytes();

            buf.readBoolean();
            ItemStack itemStack = new ItemStack(buf.readRegistryValue(Registries.ITEM), buf.readByte());

            AtomicLong byteTracker = new AtomicLong(0);

            int i = buf.readerIndex();
            byte b = buf.readByte();
            if (b != 0) {
                buf.readerIndex(i);
                ByteBufInputStream BBIS = new ByteBufInputStream(buf);

                NbtTagSizeTracker tracker = new NbtTagSizeTracker(0L) {
                    public void add(long bytes) {
                        byteTracker.set(byteTracker.get() + bytes);
                    }
                };
                itemStack.setNbt(NbtIo.read(BBIS, tracker));
            }

            this.results.nbtSize = byteTracker.get();

            PacketByteBuf compressionBuf = new PacketByteBuf(Unpooled.buffer());
            if (this.results.byteSize > 0 && this.results.byteSize <= 2147483645) {
                buf.resetReaderIndex();

                byte[] bs = buf.getWrittenBytes();
                compressionBuf.writeVarInt(bs.length);
                deflater.setInput(bs, 0, (int) this.results.byteSize);
                deflater.finish();

                while (!this.deflater.finished()) {
                    int j = this.deflater.deflate(this.deflateBuffer);
                    compressionBuf.writeBytes(this.deflateBuffer, 0, j);
                }

                this.deflater.reset();

                this.results.uncompressible = PacketByteBuf.getVarIntLength(compressionBuf.readableBytes()) > 3;
                this.results.compressedSize = compressionBuf.readableBytes();
            } else {
                this.results.moreThanIntLimit = true;
            }

                    /*if (byteCount >= 1024 && byteCount < 1048576)
                        tempSize = String.format("%.2f kb", byteCount / (float) 1024);
                    else if (byteCount >= 1048576 && byteCount < 1073741824)
                        tempSize = String.format("%.2f Mb", byteCount / (float) 1048576);
                    else if (byteCount >= 1073741824) tempSize = String.format("%.2f Gb", byteCount / (float) 1073741824);
                    else tempSize = String.format("%d bytes", byteCount);*/

        } catch (Exception e) {
            this.results.error = true;
            LogUtils.getLogger().error("Error calculating stack size!", e);
        }
    }

    public class Results {
        public long byteSize = -1;
        public long nbtSize = -1;
        public long compressedSize = -1;
        public boolean uncompressible = false;
        public boolean moreThanIntLimit = false;
        public boolean error = false;
    }

}
