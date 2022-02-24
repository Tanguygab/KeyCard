package io.github.tanguygab.keycard.events;

import io.github.tanguygab.keycard.scanner.Scanner;
import org.bukkit.inventory.ItemStack;

public class KeyCardCheckEvent extends KeyCardEvent {

    private Result result = Result.DEFAULT;

    public KeyCardCheckEvent(ItemStack item, Scanner scanner, String type) {
        super(item,scanner,type);
    }

    public void setResult(Result newResult) {
        result = newResult;
    }

    public Result getResult() {
        return result;
    }
}
