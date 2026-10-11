package me.saif.betterenderchests.enderchest;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Counts who is looking into each ender chest block, so the lid opens for the first viewer and closes only after the
 * last one leaves. Paper keeps a single open flag per block: closing it for one viewer used to shut the lid on
 * everyone else still looking in.
 * <p>
 * Viewers that open the chest silently (staff in vanish) are never added, so they never move the lid.
 *
 * @param <B> the block key, {@link org.bukkit.block.Block} on the server
 */
public final class LidTracker<B> {

    private final Map<B, Set<UUID>> viewers = new ConcurrentHashMap<>();

    /**
     * @return true when this is the first viewer of the block, so the lid has to open
     */
    public boolean open(B block, UUID viewer) {
        AtomicBoolean first = new AtomicBoolean();
        this.viewers.compute(block, (key, set) -> {
            Set<UUID> current = set == null ? new HashSet<>() : set;
            first.set(current.isEmpty());
            current.add(viewer);
            return current;
        });
        return first.get();
    }

    /**
     * @return true when the last viewer of the block left, so the lid has to close. False for a viewer that was
     * never counted (a silent one).
     */
    public boolean close(B block, UUID viewer) {
        AtomicBoolean last = new AtomicBoolean();
        this.viewers.computeIfPresent(block, (key, set) -> {
            if (!set.remove(viewer))
                return set;
            if (!set.isEmpty())
                return set;
            last.set(true);
            return null;
        });
        return last.get();
    }
}
