package io.github.tanguygab.keycard.events;

import io.github.tanguygab.keycard.scanner.Scanner;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;

public class KeyCardCheckEvent extends KeyCardEvent implements Cancellable {

    private boolean isCancelled;

    public KeyCardCheckEvent(ItemStack item, Scanner scanner, String type) {
        super(item,scanner,type);
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }
}
