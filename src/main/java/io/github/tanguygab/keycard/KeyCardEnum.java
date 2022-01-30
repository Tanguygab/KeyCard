package io.github.tanguygab.keycard;

import org.bukkit.Material;

public enum KeyCardEnum {

    KEYCARD("Keycard",Material.PAPER,(byte)1),
    MULTI_CARD("Multi-card",Material.BOOK,(byte)2),
    REMOTE_CARD("Remote Card",Material.REPEATER,(byte)3);

    private final String name;
    private final Material mat;
    private final byte num;

    KeyCardEnum(String name, Material mat, byte num) {
        this.name = name;
        this.mat = mat;
        this.num = num;
    }

    public String getName() {
        return name;
    }
    public Material getMat() {
        return mat;
    }
    public byte getNum() {
        return num;
    }

    public static KeyCardEnum get(String str) {
        for (KeyCardEnum card : values()) {
            if (card.getName().replace(" ","-").equalsIgnoreCase(str))
                return card;
        }
        return KEYCARD;
    }
}
