package me.saif.betterenderchests.utils;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The stored chest container is read and written without the NBT-API (which fails on Minecraft versions it
 * does not know). These tests build containers by hand with every kind of tag an item can contain.
 */
class RawNbtTest {

    /** An item payload with nested compounds, lists of compounds, strings, arrays and every number type. */
    private static byte[] item(String id, int count) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);

        tag(out, 3, "DataVersion");
        out.writeInt(4903);
        tag(out, 8, "id");
        string(out, id);
        tag(out, 3, "count");
        out.writeInt(count);

        tag(out, 10, "components");
        {
            tag(out, 10, "minecraft:enchantments");
            tag(out, 3, "minecraft:mending");
            out.writeInt(1);
            out.writeByte(0);

            tag(out, 8, "minecraft:custom_name");
            string(out, "{\"text\":\"Raptor\"}");

            tag(out, 9, "minecraft:lore");
            out.writeByte(8);
            out.writeInt(2);
            string(out, "a");
            string(out, "b");

            tag(out, 9, "minecraft:container");
            out.writeByte(10);
            out.writeInt(2);
            for (int i = 0; i < 2; i++) {
                tag(out, 3, "slot");
                out.writeInt(i);
                tag(out, 10, "item");
                tag(out, 8, "id");
                string(out, "minecraft:diamond");
                out.writeByte(0);
                out.writeByte(0);
            }

            tag(out, 1, "b");
            out.writeByte(1);
            tag(out, 2, "s");
            out.writeShort(2);
            tag(out, 4, "l");
            out.writeLong(3);
            tag(out, 5, "f");
            out.writeFloat(1.5f);
            tag(out, 6, "d");
            out.writeDouble(2.5);
            tag(out, 7, "ba");
            out.writeInt(3);
            out.write(new byte[]{1, 2, 3});
            tag(out, 11, "ia");
            out.writeInt(2);
            out.writeInt(7);
            out.writeInt(8);
            tag(out, 12, "la");
            out.writeInt(1);
            out.writeLong(9);

            tag(out, 9, "empty");
            out.writeByte(0);
            out.writeInt(0);
        }
        out.writeByte(0);

        out.writeByte(0);
        return bytes.toByteArray();
    }

    private static void tag(DataOutputStream out, int type, String name) throws IOException {
        out.writeByte(type);
        string(out, name);
    }

    private static void string(DataOutputStream out, String s) throws IOException {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        out.writeShort(b.length);
        out.write(b);
    }

    private static byte[] container(int size, int[] slots, byte[][] payloads) throws IOException {
        List<RawNbt.Entry> entries = new ArrayList<>();
        for (int i = 0; i < slots.length; i++)
            entries.add(new RawNbt.Entry(slots[i], RawNbt.withSlot(payloads[i], slots[i])));
        return RawNbt.assemble(size, entries);
    }

    @Test
    void parsesAssembledContainer() throws IOException {
        byte[] sword = item("minecraft:netherite_sword", 1);
        byte[] apples = item("minecraft:enchanted_golden_apple", 55);

        RawNbt.Container parsed = RawNbt.parse(container(54, new int[]{0, 39}, new byte[][]{sword, apples}));

        assertEquals(54, parsed.size);
        assertEquals(2, parsed.items.size());
        assertEquals(0, parsed.items.get(0).slot);
        assertEquals(39, parsed.items.get(1).slot);
        assertArrayEquals(RawNbt.withSlot(sword, 0), parsed.items.get(0).payload);
        assertArrayEquals(RawNbt.withSlot(apples, 39), parsed.items.get(1).payload);
    }

    @Test
    void roundTripKeepsEveryItemByteForByte() throws IOException {
        byte[] first = container(27, new int[]{3, 4, 26}, new byte[][]{item("minecraft:stone", 64), item("minecraft:bow", 1), item("minecraft:arrow", 12)});

        RawNbt.Container parsed = RawNbt.parse(first);
        byte[] second = RawNbt.assemble(parsed.size, parsed.items);

        assertArrayEquals(first, second);
    }

    @Test
    void emptyChestHasNoItems() {
        RawNbt.Container parsed = RawNbt.parse(RawNbt.assemble(54, new ArrayList<>()));

        assertEquals(54, parsed.size);
        assertTrue(parsed.items.isEmpty());
    }

    @Test
    void documentWrapperRoundTrips() throws IOException {
        byte[] payload = item("minecraft:trident", 1);

        assertArrayEquals(payload, RawNbt.payloadOfDocument(RawNbt.asDocument(payload)));
    }

    @Test
    void readsDocumentWithNamedRoot() throws IOException {
        byte[] payload = item("minecraft:trident", 1);
        byte[] named = new byte[payload.length + 3 + 4];
        named[0] = 10;
        named[2] = 4;
        System.arraycopy("root".getBytes(StandardCharsets.UTF_8), 0, named, 3, 4);
        System.arraycopy(payload, 0, named, 7, payload.length);

        assertArrayEquals(payload, RawNbt.payloadOfDocument(named));
    }

    @Test
    void itemWithoutSlotIsReportedWithSlotMinusOne() throws IOException {
        List<RawNbt.Entry> entries = new ArrayList<>();
        entries.add(new RawNbt.Entry(-1, item("minecraft:stone", 1)));

        RawNbt.Container parsed = RawNbt.parse(RawNbt.assemble(9, entries));

        assertEquals(-1, parsed.items.get(0).slot);
    }

    @Test
    void containerWithoutSizeIsRejected() {
        byte[] noSize = {10, 0, 0, 0};

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> RawNbt.parse(noSize));
        assertTrue(e.getMessage().contains("no size tag"));
    }

    @Test
    void truncatedDataIsRejectedNotSilentlyShortened() throws IOException {
        byte[] full = container(54, new int[]{0}, new byte[][]{item("minecraft:stone", 1)});

        for (int cut : new int[]{1, 5, full.length / 2, full.length - 1})
            assertThrows(IllegalArgumentException.class, () -> RawNbt.parse(Arrays.copyOf(full, cut)));
    }

    @Test
    void nonCompoundRootIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> RawNbt.parse(new byte[]{9, 0, 0, 0, 0, 0, 0, 0}));
    }
}
