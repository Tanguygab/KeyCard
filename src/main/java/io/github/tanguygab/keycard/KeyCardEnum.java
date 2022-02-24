package io.github.tanguygab.keycard;

import org.bukkit.Material;

public enum KeyCardEnum {

    KEYCARD("Keycard",Material.PAPER,"normal"),
    MULTI_CARD("Multi-card",Material.BOOK,"multi"),
    REMOTE_CARD("Remote Card",Material.REPEATER,"remote");

    private final String name;
    private final Material mat;
    private final String type;

    KeyCardEnum(String name, Material mat, String type) {
        this.name = name;
        this.mat = mat;
        this.type = type;
    }

    public String getName() {
        return name;
    }
    public Material getMat() {
        return mat;
    }
    public String getType() {
        return type;
    }

    public static KeyCardEnum get(String str) {
        for (KeyCardEnum card : values()) {
            if (card.getName().replace(" ","-").equalsIgnoreCase(str))
                return card;
        }
        return KEYCARD;
    }
}
