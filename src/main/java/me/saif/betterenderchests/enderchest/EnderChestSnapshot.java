package me.saif.betterenderchests.enderchest;

import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.UUID;

public class EnderChestSnapshot {

    private final int rows;
    private final ItemStack[] contents;
    private final String name;
    private final UUID uuid;
    private final boolean loadFailed;

    public EnderChestSnapshot(UUID uuid, String name, ItemStack[] contents, int rows) {
        this(uuid, name, contents, rows, false);
    }

    private EnderChestSnapshot(UUID uuid, String name, ItemStack[] contents, int rows, boolean loadFailed) {
        this.uuid = uuid;
        this.name = name;
        this.contents = contents.length == 54 ? contents : Arrays.copyOf(contents, 54);
        this.rows = rows;
        this.loadFailed = loadFailed;
    }

    /**
     * Snapshot standing in for a chest whose stored data could not be read. It has no items and
     * must never be written back, otherwise the unreadable (but intact) data would be overwritten.
     */
    public static EnderChestSnapshot loadFailed(UUID uuid, String name, int rows) {
        return new EnderChestSnapshot(uuid, name, new ItemStack[54], rows, true);
    }

    protected EnderChestSnapshot(EnderChest enderChest) {
        this.name = enderChest.getName();
        this.uuid = enderChest.getUUID();
        this.rows = enderChest.getLastNumRows();
        this.contents = enderChest.getContents();
        this.loadFailed = enderChest.isLoadFailed();
    }

    public boolean isLoadFailed() {
        return loadFailed;
    }

    public String getName() {
        return name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getRows() {
        return rows;
    }

    public int getNumContents() {
        return contents.length;
    }

    public ItemStack get(int i) {
        if (i >= contents.length || i < 0)
            return null;
        return contents[i];
    }

    public ItemStack[] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnderChestSnapshot)) return false;

        EnderChestSnapshot that = (EnderChestSnapshot) o;

        return getUuid().equals(that.getUuid());
    }
}
