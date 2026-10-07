package me.saif.betterenderchests.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The live server's ender chest title is written as {@code <#C21807> X &#C21807&lE&#C81806&ln...}.
 * It used to show a literal '&' in front of every letter and a '<>' pair at the start.
 */
class ColorUtilTest {

    private static final String HEX = "§x§c§2§1§8§0§7";

    @Test
    void ampersandHexIsAColorNotLiteralText() {
        assertEquals(HEX + "§lE", ColorUtil.translate("&#C21807&lE"));
    }

    @Test
    void tagHexIsAColorNotLiteralText() {
        assertEquals(HEX + " Chest", ColorUtil.translate("<#C21807> Chest"));
    }

    @Test
    void plainHexStillWorks() {
        assertEquals(HEX + "Chest", ColorUtil.translate("#C21807Chest"));
    }

    @Test
    void legacyHexAndNormalCodesStillWork() {
        assertEquals("§x§c§2§1§8§0§7§lA", ColorUtil.translate("&x&c&2&1&8&0&7&lA"));
        assertEquals("§7Ender", ColorUtil.translate("&7Ender"));
    }

    @Test
    void ownersRealTitleHasNoLeftoverMarkup() {
        String title = "<#C21807> 🪎 &#C21807&lE&#C81806&ln&#CE1806&ld&#D41805&le&#DA1804&lr "
                + "&#E71803&lC&#ED1802&lh&#F31801&le&#F91801&ls&#FF1800&lt";

        String out = ColorUtil.translate(title);

        assertFalse(out.contains("&"), out);
        assertFalse(out.contains("<"), out);
        assertFalse(out.contains(">"), out);
        assertEquals("Ender Chest", out.replaceAll("§.", "").replace("🪎", "").replaceAll("\\s+", " ").trim());
    }

    @Test
    void placeholdersAreLeftAlone() {
        assertEquals("<player>'s Chest", ColorUtil.translate("<player>'s Chest"));
    }
}
