package io.github.tanguygab.keycard.scanner;

import io.github.tanguygab.keycard.KeyCardPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;


public class RenamingScanner {

    private final Player player;
    private final Entity frame;

    public RenamingScanner(Player p, Entity frame) {
        player = p;
        this.frame = frame;
        p.sendMessage("Send the name of your Scanner here");
    }


    public boolean renamed(String name) {
        if (KeyCardPlugin.get().scanners.containsKey(name)) {
            player.sendMessage("This scanner name is already taken!");
            return false;
        }
        player.sendMessage("This scanner is now called `"+name+"`.");
        KeyCardPlugin.get().addScanner(new Scanner(name,frame.getUniqueId(),player.getUniqueId()));
        return true;
    }
}
