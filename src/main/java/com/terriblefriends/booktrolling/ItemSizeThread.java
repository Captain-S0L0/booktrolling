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
    private ItemStack stack = null;
    public long diskSize = -1;
    public long nbtSize = -1;
    public long compressedSize = -1;
    public boolean uncompressible = false;
    private boolean forceStop = false;
    private boolean changedStack = false;
    private final Deflater deflater = new Deflater();
    private final byte[] deflateBuffer = new byte[8192];

    public void setStack(ItemStack stack) {
        if (stack != null && stack != this.stack) {
            this.stack = stack;
            this.diskSize = -1;
            this.nbtSize = -1;
            this.compressedSize = -1;
            this.uncompressible = false;
            this.changedStack = true;
        }
    }

    public void forceStop() {
        this.forceStop = true;
    }


    @Override
    public void run() {
        while (!forceStop) {
            if (stack != null && changedStack) {
                try {
                    changedStack = false;
                    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                    buf.writeItemStack(this.stack);
                    this.diskSize = buf.readableBytes();

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

                    this.nbtSize = byteTracker.get();

                    PacketByteBuf compressionBuf = new PacketByteBuf(Unpooled.buffer());
                    if (this.diskSize > 0 && this.diskSize <= 2147483645) {
                        buf.resetReaderIndex();
                        byte[] bs = buf.getWrittenBytes();
                        compressionBuf.writeVarInt(bs.length);
                        deflater.setInput(bs, 0, (int)this.diskSize);
                        deflater.finish();

                        while (!this.deflater.finished()) {
                            int j = this.deflater.deflate(this.deflateBuffer);
                            compressionBuf.writeBytes(this.deflateBuffer, 0, j);
                        }

                        this.deflater.reset();

                        if (PacketByteBuf.getVarIntLength(compressionBuf.readableBytes()) > 3) {
                            this.uncompressible = true;
                        }
                        else {
                            uncompressible = false;
                        }
                        this.compressedSize = compressionBuf.readableBytes();
                    }
                    else {
                        uncompressible = true;
                        this.compressedSize = -9001;
                    }

                    /*if (byteCount >= 1024 && byteCount < 1048576)
                        tempSize = String.format("%.2f kb", byteCount / (float) 1024);
                    else if (byteCount >= 1048576 && byteCount < 1073741824)
                        tempSize = String.format("%.2f Mb", byteCount / (float) 1048576);
                    else if (byteCount >= 1073741824) tempSize = String.format("%.2f Gb", byteCount / (float) 1073741824);
                    else tempSize = String.format("%d bytes", byteCount);*/

                } catch (Exception e) {
                    this.diskSize = -1137;
                    LogUtils.getLogger().error("Error calculating stack size!", e);
                }
            }
            else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        LogUtils.getLogger().error("Item Size Thread Death!");
    }

}
