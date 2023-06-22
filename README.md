# booktrolling

A collection of tools for ~~exploiting~~ working with written books in Minecraft.


**Changes provided:**
  - Allows up to 8,192 characters / page in multiplayer (limit of BookUpdateC2SPacket.write()), or 32,767 in singleplayer (limit of WritableBookItem.isValid())
  - Allows titles up to 128 characters in length in multiplayer (limit of BookUpdateC2SPacket.write()), or 65,535 in singleplayer (limit of NbtString.write())
  - Allows written books with titles > 31 characters in length to be parsed (in vanilla, books with titles longer than 31 characters are treated as "invalid" and will not display contents on book screen)
  - Prevents client-side kicks as a result of NbtTagSizeTracker tracking more than 2,097,152 bytes
  - Toggleable item size debug information in hover tooltip (toggleable on in-game pause menu, default false)


**Book Presets:**
  - Vanilla: 100 pages each with 1,023 3 byte characters (this is achieveable in vanilla by utilizing a resource pack to edit the width of characters, then limited by a hardcoded < 1024 character check in the book edit GUI)<br>
  - Singleplayer: 100 pages each with 21,837 3 byte characters (singleplayer only, limit of NbtString.write())
  - Multiplayer: 100 pages each with 8,192 3 byte characters (limit of BookUpdateC2SPacket.write())
  - Paper: 100 pages each with 320 3 byte characters (respects limits of PaperMC servers and its forks)
  - Clear: removes all contents of a book
  - AutoSign: automatically sign book when using presets (toggleable, default false)
  - RandomizeChars: generate random characters, or use a single one (toggleable, default true)

**Item Size Debug**:

The item size debug tooltip can help provide approximates for relavent size information. It is not expected to be exact.
  - DISK: bytes that are written as a result of PacketByteBuf.writeItemStack. A decent approximation of what data is written to disk and what is utilized in RAM
  - NBT: bytes counted from NbtTagSizeTracker during NbtIo.read. Important to consider for bookbanning/kicking, as if > 2,097,152, it will kick vanilla clients during attempted read
  - COMPRESS: bytes written from compressing PacketByteBuf.writeItemStack with java.util.zip.Deflater. Important to consider if resulting item packets are incompressible (PacketByteBuf.getVarIntLength(buf.readableBytes()) > 3), if true results in unavoidable server-side kicks when trying to send packets containing the item
