package me.saif.betterenderchests.enderchest;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LidTrackerTest {

    private final LidTracker<String> lids = new LidTracker<>();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    @Test
    void singleViewerOpensAndClosesTheLid() {
        assertTrue(lids.open("chest", alice));
        assertTrue(lids.close("chest", alice));
    }

    @Test
    void lidStaysOpenUntilTheLastViewerLeaves() {
        assertTrue(lids.open("chest", alice));
        assertFalse(lids.open("chest", bob), "the lid is already open");

        assertFalse(lids.close("chest", alice), "bob is still looking in");
        assertTrue(lids.close("chest", bob));
    }

    @Test
    void silentViewerNeverClosesTheLid() {
        assertTrue(lids.open("chest", alice));

        //a vanished player is never counted, so their close must not shut alice's lid
        assertFalse(lids.close("chest", bob));
        assertTrue(lids.close("chest", alice));
    }

    @Test
    void blocksAreCountedSeparately() {
        assertTrue(lids.open("a", alice));
        assertTrue(lids.open("b", bob));

        assertTrue(lids.close("a", alice));
        assertTrue(lids.close("b", bob));
    }

    @Test
    void lidOpensAgainAfterEveryoneLeft() {
        lids.open("chest", alice);
        lids.close("chest", alice);

        assertTrue(lids.open("chest", bob));
    }

    @Test
    void closingTwiceOrUnknownBlockDoesNothing() {
        assertFalse(lids.close("chest", alice));

        lids.open("chest", alice);
        assertTrue(lids.close("chest", alice));
        assertFalse(lids.close("chest", alice));
    }
}
