package me.saif.betterenderchests.utils;

import de.tr7zw.changeme.nbtapi.*;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.utils.DataFixerUtil;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ItemStackSerializer {

    /**
     * Items are written with the server's own item codec ({@link ItemStack#serializeAsBytes()}), so saving keeps
     * working on Minecraft versions the bundled NBT-API does not know yet. The NBT-API is only the fallback for
     * servers without that method. Both produce the same {@code nbtbytes:} layout.
     */
    public static String serialize(ItemStack[] items) {
        try {
            return serializeWithServerCodec(items);
        } catch (Exception | LinkageError e) {
            return serializeWithNbtApi(items);
        }
    }

    static String serializeWithServerCodec(ItemStack[] items) {
        try {
            List<RawNbt.Entry> entries = new ArrayList<>();
            for (int i = 0; i < items.length; ++i) {
                ItemStack item = items[i];
                if (item != null && item.getType() != Material.AIR) {
                    byte[] payload = RawNbt.payloadOfDocument(gunzip(item.serializeAsBytes()));
                    entries.add(new RawNbt.Entry(i, RawNbt.withSlot(payload, i)));
                }
            }
            return "nbtbytes:" + Base64Coder.encodeLines(gzip(RawNbt.assemble(items.length, entries)));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    private static String serializeWithNbtApi(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            NBTContainer container = new NBTContainer();
            container.setInteger("size", items.length);
            NBTCompoundList list = container.getCompoundList("items");

            for(int i = 0; i < items.length; ++i) {
                ItemStack item = items[i];
                if (item != null && item.getType() != Material.AIR) {
                    NBTListCompound entry = list.addCompound();
                    entry.setInteger("Slot", i);
                    entry.mergeCompound(NBT.itemStackToNBT(item));
                }
            }
            NBTReflectionUtil.writeApiNBT(container, outputStream);

            outputStream.close();
            return "nbtbytes:" + Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    public static String serializeJson(ItemStack[] obj) {
        return "json:" + de.tr7zw.changeme.nbtapi.NBT.itemStackArrayToNBT(obj);
    }

    @Deprecated
    public static String serializeOld(ItemStack[] obj) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            dataOutput.writeInt(obj.length);

            for (int i = 0; i < obj.length; i++) {
                dataOutput.writeObject(obj[i]);
            }

            dataOutput.close();
            outputStream.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    public static ItemStack[] deserialize(String str) {
        if (str.startsWith("nbtbytes:")) {
            return deserializeNBTBytes(str.substring(9));
        } else if (str.startsWith("json:")) {
            return deserializeJson(str.substring(5));
        } else {
            return deserializeOld(str);
        }
    }

    public static ItemStack[] deserializeNBTBytes(String str) {
        RawNbt.Container container;
        try {
            container = RawNbt.parse(gunzip(Base64Coder.decodeLines(str)));
        } catch (Exception | LinkageError e) {
            throw new ItemDeserializationException("Stored enderchest data could not be read", e);
        }

        ItemStack[] rebuild = new ItemStack[container.size];

        for (int i = 0; i < rebuild.length; ++i) {
            rebuild[i] = new ItemStack(Material.AIR);
        }

        // A single unreadable item must never be silently dropped: the chest is saved
        // again later and the item would be gone for good. Collect every failure and
        // refuse to hand out a partial chest.
        int total = 0;
        int failed = 0;
        Throwable firstCause = null;
        StringBuilder failedSlots = new StringBuilder();

        for (RawNbt.Entry entry : container.items) {
            total++;
            try {
                if (entry.slot < 0 || entry.slot >= rebuild.length)
                    throw new IllegalStateException("item has no valid slot (" + entry.slot + ")");

                rebuild[entry.slot] = readItem(entry);
            } catch (Exception | LinkageError e) {
                failed++;
                if (firstCause == null)
                    firstCause = e;
                if (failedSlots.length() > 0)
                    failedSlots.append(", ");
                failedSlots.append(entry.slot);
            }
        }

        if (failed > 0) {
            throw new ItemDeserializationException(failed + " of " + total + " stored items could not be read (slots: "
                    + failedSlots + ")", firstCause);
        }

        return rebuild;
    }

    /**
     * The server's own item codec first (it data-fixes older items itself and does not depend on the NBT-API
     * knowing the running Minecraft version); the NBT-API only for items the server codec rejects.
     */
    private static ItemStack readItem(RawNbt.Entry entry) throws Exception {
        byte[] gzipped = gzip(RawNbt.asDocument(entry.payload));
        try {
            ItemStack item = ItemStack.deserializeBytes(gzipped);
            if (item != null)
                return item;
        } catch (Exception | LinkageError ignored) {
            // fall through to the NBT-API
        }

        ReadWriteNBT lcomp = new NBTContainer(new ByteArrayInputStream(gzipped));
        if (lcomp.hasTag("Count") && MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_20_R4)) {
            lcomp = DataFixerUtil.fixUpItemData(lcomp, 3700, DataFixerUtil.getCurrentVersion());
        }

        ItemStack item = NBT.itemStackFromNBT(lcomp);
        if (item == null)
            throw new IllegalStateException("item could not be parsed from its stored data");
        return item;
    }

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
            out.write(data);
        }
        return bytes.toByteArray();
    }

    private static byte[] gunzip(byte[] data) throws IOException {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(data))) {
            return in.readAllBytes();
        }
    }

    public static ItemStack[] deserializeJson(String str) {
        ItemStack[] stacks;
        try {
            stacks = NBT.itemStackArrayFromNBT(NBT.parseNBT(str));
        } catch (Exception | LinkageError e) {
            throw new ItemDeserializationException("Stored enderchest data could not be read", e);
        }

        if (stacks == null)
            throw new ItemDeserializationException("Stored enderchest data could not be parsed into items");

        return stacks;
    }


    public static ItemStack[] deserializeOld(String str) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(str));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];

            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            dataInput.close();
            return items;
        } catch (Exception | LinkageError e) {
            throw new ItemDeserializationException("Stored enderchest data could not be read", e);
        }
    }


}
