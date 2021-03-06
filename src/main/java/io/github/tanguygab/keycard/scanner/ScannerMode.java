package io.github.tanguygab.keycard.scanner;

import io.github.tanguygab.keycard.KeyCardPlugin;
import org.bukkit.Material;

import java.util.List;
import java.util.function.Consumer;

public enum ScannerMode {

    ACTIVE_ON_SWIPE(Material.REDSTONE,"Active on swipe",scanner-> scanner.setStatus(true), scanner-> scanner.setStatus(false)),
    INACTIVE_ON_SWIPE(Material.REDSTONE_TORCH,"Inactive on swipe",scanner-> scanner.setStatus(false), scanner-> scanner.setStatus(true)),
    LEVER(Material.LEVER,"Lever",scanner-> {
        boolean activated = !KeyCardPlugin.get().scannersFile.getBoolean(scanner.getName()+".lever",false);
        KeyCardPlugin.get().scannersFile.set(scanner.getName()+".lever",activated);
        scanner.setStatus(activated);
    }, scanner-> {
        scanner.setStatus(KeyCardPlugin.get().scannersFile.getBoolean(scanner.getName()+".lever",false));
    });

    private final Material mat;
    private final String desc;
    private final Consumer<Scanner> load;
    private final Consumer<Scanner> unload;

    public static List<ScannerMode> allowedModes;

    ScannerMode(Material mat, String desc, Consumer<Scanner> load, Consumer<Scanner> unload) {
        this.mat = mat;
        this.desc = desc;
        this.load = load;
        this.unload = unload;
    }

    public Material getMat() {
        return mat;
    }
    public String getDesc() {
        return desc;
    }

    public void load(Scanner scanner) {
        load.accept(scanner);
    }
    public void unload(Scanner scanner) {
        unload.accept(scanner);
    }

    public static ScannerMode switchMode(ScannerMode oldMode) {

        if (allowedModes.isEmpty()) return ACTIVE_ON_SWIPE;

        ScannerMode newMode;
        if (allowedModes.indexOf(oldMode)+1 >= allowedModes.size()) newMode = allowedModes.get(0);
        else newMode = allowedModes.get(allowedModes.indexOf(oldMode) + 1);

        return newMode == null ? ACTIVE_ON_SWIPE : newMode;
    }

    public static ScannerMode get(String str, boolean checkAllowed) {
        str = str.replace(" ","_");
        for (ScannerMode mode : values()) {
            if (mode.toString().equalsIgnoreCase(str) && (!checkAllowed || allowedModes.contains(mode)))
                return mode;
        }
        return allowedModes.get(0);
    }
}
