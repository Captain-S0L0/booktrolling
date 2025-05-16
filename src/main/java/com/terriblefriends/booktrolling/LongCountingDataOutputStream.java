package com.terriblefriends.booktrolling;

import org.jetbrains.annotations.NotNull;

import java.io.*;

// counting logic from java.io.DataOutputStream
public class LongCountingDataOutputStream extends OutputStream implements DataOutput {
    private long written;
    private final DataOutputStream out;

    public LongCountingDataOutputStream(OutputStream out) {
        this.out = new DataOutputStream(out);
    }

    private void incCount(long value) {
        long temp = written + value;
        if (temp < 0) {
            temp = Long.MAX_VALUE;
        }
        written = temp;
    }

    public synchronized void write(int b) throws IOException {
        out.write(b);
        incCount(1);
    }

    @Override
    public void write(byte @NotNull [] b) throws IOException {
        out.write(b);
        incCount(b.length);
    }

    public synchronized void write(byte @NotNull [] b, int off, int len)
            throws IOException
    {
        out.write(b, off, len);
        incCount(len);
    }

    public void flush() throws IOException {
        out.flush();
    }

    public final void writeBoolean(boolean v) throws IOException {
        out.writeBoolean(v);
        incCount(1);
    }

    public final void writeByte(int v) throws IOException {
        out.writeByte(v);
        incCount(1);
    }

    public final void writeShort(int v) throws IOException {
        out.writeShort(v);
        incCount(2);
    }

    public final void writeChar(int v) throws IOException {
        out.writeChar(v);
        incCount(2);
    }

    public final void writeInt(int v) throws IOException {
        out.writeInt(v);
        incCount(4);
    }

    public final void writeLong(long v) throws IOException {
        out.writeLong(v);
        incCount(8);
    }

    public final void writeFloat(float v) throws IOException {
        out.writeFloat(v);
        incCount(4);
    }

    public final void writeDouble(double v) throws IOException {
        out.writeDouble(v);
        incCount(8);
    }

    public final void writeBytes(@NotNull String s) throws IOException {
        out.writeBytes(s);
        incCount(s.length());
    }

    public final void writeChars(@NotNull String s) throws IOException {
        out.writeChars(s);
        incCount(s.length() * 2L);
    }

    public final void writeUTF(@NotNull String str) throws IOException {
        out.writeUTF(str);

        final int strlen = str.length();
        int utflen = strlen; // optimized for ASCII

        for (int i = 0; i < strlen; i++) {
            int c = str.charAt(i);
            if (c >= 0x80 || c == 0)
                utflen += (c >= 0x800) ? 2 : 1;
        }
        incCount(utflen + 2);
    }

    public final long size() {
        return written;
    }
}
