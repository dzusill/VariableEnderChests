package me.saif.betterenderchests.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Minimal NBT byte reader/writer for the stored enderchest container
 * ({@code size: int, items: [{Slot: int, ...item...}, ...]}).
 * <p>
 * It never interprets an item: every item compound is kept as the raw bytes it was stored with, so the
 * server's own item codec (Paper's {@code ItemStack.deserializeBytes}) can turn it back into an item. That keeps
 * reading and writing independent of the NBT-API version, which breaks on every new Minecraft release.
 */
final class RawNbt {

    private static final int TAG_END = 0;
    private static final int TAG_BYTE = 1;
    private static final int TAG_SHORT = 2;
    private static final int TAG_INT = 3;
    private static final int TAG_LONG = 4;
    private static final int TAG_FLOAT = 5;
    private static final int TAG_DOUBLE = 6;
    private static final int TAG_BYTE_ARRAY = 7;
    private static final int TAG_STRING = 8;
    private static final int TAG_LIST = 9;
    private static final int TAG_COMPOUND = 10;
    private static final int TAG_INT_ARRAY = 11;
    private static final int TAG_LONG_ARRAY = 12;

    private RawNbt() {
    }

    /**
     * One stored item: its slot and the bytes of its compound payload (tags followed by TAG_End, no root header).
     */
    static final class Entry {
        final int slot;
        final byte[] payload;

        Entry(int slot, byte[] payload) {
            this.slot = slot;
            this.payload = payload;
        }
    }

    static final class Container {
        final int size;
        final List<Entry> items;

        Container(int size, List<Entry> items) {
            this.size = size;
            this.items = Collections.unmodifiableList(items);
        }
    }

    /**
     * @param raw the uncompressed bytes of the stored container
     * @throws IllegalArgumentException if the bytes are not a container
     */
    static Container parse(byte[] raw) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(raw);
            if ((buf.get() & 0xFF) != TAG_COMPOUND)
                throw new IllegalArgumentException("root tag is not a compound");
            skipString(buf);

            int size = -1;
            List<Entry> items = new ArrayList<>();
            while (true) {
                int type = buf.get() & 0xFF;
                if (type == TAG_END)
                    break;
                String name = readString(buf);

                if (type == TAG_INT && name.equals("size")) {
                    size = buf.getInt();
                } else if (type == TAG_LIST && name.equals("items")) {
                    int elementType = buf.get() & 0xFF;
                    int count = buf.getInt();
                    if (count > 0 && elementType != TAG_COMPOUND)
                        throw new IllegalArgumentException("items is not a list of compounds");
                    for (int i = 0; i < count; i++) {
                        int start = buf.position();
                        skipPayload(buf, TAG_COMPOUND, 0);
                        byte[] payload = new byte[buf.position() - start];
                        System.arraycopy(raw, start, payload, 0, payload.length);
                        items.add(new Entry(slotOf(payload), payload));
                    }
                } else {
                    skipPayload(buf, type, 0);
                }
            }

            if (size < 0)
                throw new IllegalArgumentException("Stored enderchest data has no size tag");
            return new Container(size, items);
        } catch (BufferUnderflowException | IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("truncated NBT data", e);
        }
    }

    /**
     * @return the container bytes (uncompressed) in the layout {@link #parse} reads
     */
    static byte[] assemble(int size, List<Entry> items) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(TAG_COMPOUND);
            out.writeShort(0);

            out.writeByte(TAG_INT);
            writeString(out, "size");
            out.writeInt(size);

            out.writeByte(TAG_LIST);
            writeString(out, "items");
            out.writeByte(TAG_COMPOUND);
            out.writeInt(items.size());
            for (Entry entry : items)
                out.write(entry.payload);

            out.writeByte(TAG_END);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param itemPayload the payload of an item compound
     * @return the same payload with a {@code Slot} tag in front
     */
    static byte[] withSlot(byte[] itemPayload, int slot) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(itemPayload.length + 16);
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(TAG_INT);
            writeString(out, "Slot");
            out.writeInt(slot);
            out.write(itemPayload);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Wraps a compound payload into a complete uncompressed NBT document, the form item codecs read.
     */
    static byte[] asDocument(byte[] payload) {
        byte[] doc = new byte[payload.length + 3];
        doc[0] = TAG_COMPOUND;
        System.arraycopy(payload, 0, doc, 3, payload.length);
        return doc;
    }

    /**
     * Inverse of {@link #asDocument}: drops the root tag header and returns the compound payload.
     *
     * @throws IllegalArgumentException if the bytes are not a compound document
     */
    static byte[] payloadOfDocument(byte[] doc) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(doc);
            if ((buf.get() & 0xFF) != TAG_COMPOUND)
                throw new IllegalArgumentException("root tag is not a compound");
            skipString(buf);
            byte[] payload = new byte[doc.length - buf.position()];
            System.arraycopy(doc, buf.position(), payload, 0, payload.length);
            return payload;
        } catch (BufferUnderflowException | IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("truncated NBT data", e);
        }
    }

    /**
     * @return the value of the top-level int tag {@code Slot}, or -1 if there is none
     */
    private static int slotOf(byte[] payload) {
        ByteBuffer buf = ByteBuffer.wrap(payload);
        while (true) {
            int type = buf.get() & 0xFF;
            if (type == TAG_END)
                return -1;
            String name = readString(buf);
            if (type == TAG_INT && name.equals("Slot"))
                return buf.getInt();
            skipPayload(buf, type, 0);
        }
    }

    private static void skipPayload(ByteBuffer buf, int type, int depth) {
        if (depth > 512)
            throw new IllegalArgumentException("NBT nested too deep");
        switch (type) {
            case TAG_BYTE:
                skip(buf, 1);
                break;
            case TAG_SHORT:
                skip(buf, 2);
                break;
            case TAG_INT:
            case TAG_FLOAT:
                skip(buf, 4);
                break;
            case TAG_LONG:
            case TAG_DOUBLE:
                skip(buf, 8);
                break;
            case TAG_BYTE_ARRAY:
                skip(buf, buf.getInt());
                break;
            case TAG_STRING:
                skip(buf, buf.getShort() & 0xFFFF);
                break;
            case TAG_LIST: {
                int elementType = buf.get() & 0xFF;
                int count = buf.getInt();
                for (int i = 0; i < count; i++)
                    skipPayload(buf, elementType, depth + 1);
                break;
            }
            case TAG_COMPOUND:
                while (true) {
                    int inner = buf.get() & 0xFF;
                    if (inner == TAG_END)
                        break;
                    skipString(buf);
                    skipPayload(buf, inner, depth + 1);
                }
                break;
            case TAG_INT_ARRAY:
                skip(buf, Math.multiplyExact(buf.getInt(), 4));
                break;
            case TAG_LONG_ARRAY:
                skip(buf, Math.multiplyExact(buf.getInt(), 8));
                break;
            default:
                throw new IllegalArgumentException("unknown NBT tag type " + type);
        }
    }

    private static void skip(ByteBuffer buf, int bytes) {
        if (bytes < 0 || bytes > buf.remaining())
            throw new IllegalArgumentException("invalid NBT length " + bytes);
        buf.position(buf.position() + bytes);
    }

    private static void skipString(ByteBuffer buf) {
        skip(buf, buf.getShort() & 0xFFFF);
    }

    private static String readString(ByteBuffer buf) {
        byte[] bytes = new byte[buf.getShort() & 0xFFFF];
        if (bytes.length > buf.remaining())
            throw new IllegalArgumentException("invalid NBT string length");
        buf.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeString(DataOutputStream out, String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        out.writeShort(bytes.length);
        out.write(bytes);
    }
}
