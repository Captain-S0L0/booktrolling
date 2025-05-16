# booktrolling

A collection of tools for ~~exploiting~~ working with written books in Minecraft.

This is a FABRIC mod.


**Changes provided:**
- Allows up to 1,024 characters / page regardless of character width (limit of WritableBookContentComponent)
- Allows titles up to 32 characters in length (limit of WrittenBookContentComponent)
- Toggleable item size debug information in hover tooltip (toggleable on in-game pause menu, default false)

**Book Presets:**
- Vanilla: 100 pages each with 1,023 3 byte characters (this is achievable in vanilla by utilizing a resource pack to edit the
width of characters, then limited by a hardcoded < 1024 character check in the book edit GUI)
- Max: 100 pages each with 1,024 3 byte characters (limit of WritableBookContentComponent)
- Paper: 100 pages each with 320 3 byte characters (respects default limits of PaperMC servers and its forks)
- Clear: removes all contents of a book
- Auto Sign: automatically sign book when using presets (toggleable, default false)
- Randomize Chars: generate random characters, or use a single one (toggleable, default false)
- Drop: automatically drop book when using presets (toggleable, default false)


**Item Size Debug**:

The item size debug tooltip can help provide approximates for relevant size information. It is not expected to be exact.

Two statistics are provided: disk size and packet size, each with a raw and a compressed value.

Raw disk size is a decent approximation as to what is utilized in RAM. Useful for OutOfMemory suppression or similar.

If the compressed disk size of a chunk is more than the 32-bit integer limit of bytes (~2.147 GB), then the chunk will never
be able to save as the process to save a chunk to disk includes creating a byte array with the compressed data. Arrays cannot
exceed the 32-bit integer limit of elements in practically all JVM implementations (plus or minus a few for header stuff).

Raw packet size cannot exceed 8,388,608 bytes, or the server will kick any player who would receive such a packet.

Compressed packet size cannot exceed 2,097,152 bytes, or the server will kick any player who would receive such a packet.

**Old Versions**:
This readme contains relevant information for Minecraft versions 1.20.5 (24w09a) and newer. For 1.20.4 (24w07a) and lower, please see
the old readme at [README-pre-24w09a.md](https://github.com/Captain-S0L0/booktrolling/README-pre-24w09a.md).