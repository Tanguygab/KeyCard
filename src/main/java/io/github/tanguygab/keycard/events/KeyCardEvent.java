package io.github.tanguygab.keycard.events;

import io.github.tanguygab.keycard.scanner.Scanner;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class KeyCardEvent extends Event {

    private final HandlerList handlers = new HandlerList();

    private final ItemStack card;
    private final Scanner scanner;
    private final String type;

    public KeyCardEvent(ItemStack card, Scanner scanner, String type) {
        this.card = card;
        this.scanner = scanner;
        this.type = type;
    }

    public ItemStack getCard() {
        return card;
    }

    public Scanner getScanner() {
        return scanner;
    }

    public String getType() {
        return type;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
