package io.github.tanguygab.keycard.scanner;

import org.bukkit.Material;

public enum ScannerMode {

    ACTIVE_ON_SWIPE(Material.REDSTONE,"Active on swipe"),
    INACTIVE_ON_SWIPE(Material.REDSTONE_TORCH,"Inactive on swipe");

    private final Material mat;
    private final String desc;

    ScannerMode(Material mat,String desc) {
        this.mat = mat;
        this.desc = desc;
    }

    public Material getMat() {
        return mat;
    }
    public String getDesc() {
        return desc;
    }

    public static ScannerMode switchMode(ScannerMode oldMode) {
        return oldMode == ACTIVE_ON_SWIPE ? INACTIVE_ON_SWIPE : ACTIVE_ON_SWIPE;
    }
}
