package io.github.tanguygab.keycard.events;

import io.github.tanguygab.keycard.scanner.Scanner;
import org.bukkit.inventory.ItemStack;

public class KeyCardLinkEvent extends KeyCardEvent {

    private final boolean isUnlink;

    public KeyCardLinkEvent(ItemStack card, Scanner scanner, String type, boolean isUnlink) {
        super(card, scanner, type);
        this.isUnlink = isUnlink;
    }

    public boolean isUnlink() {
        return isUnlink;
    }
}
