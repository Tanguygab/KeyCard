package io.github.tanguygab.keycard;

import io.github.tanguygab.keycard.config.ConfigurationFile;
import io.github.tanguygab.keycard.config.YamlConfigurationFile;
import io.github.tanguygab.keycard.scanner.Scanner;
import io.github.tanguygab.keycard.scanner.ScannerMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public final class KeyCardPlugin extends JavaPlugin implements CommandExecutor {

    private static KeyCardPlugin instance;

    public ConfigurationFile scannersFile;
    public final Map<String, Scanner> scanners = new HashMap<>();
    public Image image;

    public static KeyCardPlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        image = new ImageIcon(getClass().getResource("/scanner.png")).getImage();
        File file = new File(getDataFolder(), "scanners.yml");
        try {
            if (!file.exists()) file.createNewFile();
            scannersFile = new YamlConfigurationFile(null, file);
            Map<String,Object> cfgmap = new HashMap<>(scannersFile.getValues());
            for (String name : cfgmap.keySet()) {
                Map<String,String> map = (Map<String, String>) cfgmap.get(name);
                Scanner scanner = new Scanner(name, UUID.fromString(map.get("frameID")), UUID.fromString(map.get("player")), ScannerMode.valueOf(map.get("mode")));
                addScanner(scanner);
            }
        } catch (Exception e) {e.printStackTrace();}
        getServer().getPluginManager().registerEvents(new Listener(this),this);

    }
    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        scanners.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Utils.colors("&m                                        \n"
                    + "&a[KeyCard] &7" + getDescription().getVersion() + "\n"
                    + " - &3/keycard\n"
                    + "   &8| &aDefault help page\n"
                    + " - &3/keycard give <keycard|scanner> [player]\n"
                    + "   &8| &aGive yourself or a player a keycard or scanner\n"
                    + " - &3/keycard name <name>\n"
                    + "   &8| &aName the keycard you're holding\n"
                    + "&m                                        "));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "give" -> {
                String arg = args.length > 1 ? args[1] : "";
                PlayerInventory inv = ((Player)sender).getInventory();
                switch (arg.toLowerCase()) {
                    case "keycard" -> {
                        ItemStack keycard = Utils.craftKeycard();
                        inv.addItem(keycard);
                    }
                    case "multicard" -> {
                        ItemStack multicard = Utils.craftMultiKeycard();
                        inv.addItem(multicard);
                    }
                    case "scanner" -> {
                        ItemStack scanner = Utils.craftScanner();
                        inv.addItem(scanner);
                    }
                    default -> sender.sendMessage("You have to specify `keycard` or `scanner`");
                }
            }
            case "reload" -> {
                onDisable();
                onEnable();
            }
            case "scanners" -> {
                String msg = "&aList of scanners &8(&7"+scanners.size()+"&8)&a:";
                for (Scanner scanner : scanners.values()) {
                    msg += "\n &8- &3" + scanner.getName() + " &8(&7" + getServer().getOfflinePlayer(scanner.getOwner()).getName()+"&8)";
                    Entity entity = getServer().getEntity(scanner.getFrameID());
                    if (entity == null) continue;
                    Location loc = entity.getLocation();
                    msg+=" &b"+loc.getX()+" &8|&b "+loc.getY()+" &8|&b "+loc.getZ();
                }
                sender.sendMessage(Utils.colors(msg));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("give","reload", "scanners");
        if ("give".equalsIgnoreCase(args[0]) && args.length == 2) return List.of("keycard","scanner","multicard");
        return null;
    }

    public Scanner getScanner(Entity entity) {
        if (entity.getType() != EntityType.ITEM_FRAME) return null;
        for (Scanner scanner : scanners.values()) {
            if (scanner.getFrameID().toString().equals(entity.getUniqueId().toString()))
                return scanner;
        }
        return null;
    }

    public void addScanner(Scanner scanner) {
        String name = scanner.getName();
        scanners.put(name,scanner);
        scannersFile.set(name+".player",scanner.getOwner().toString());
        scannersFile.set(name+".frameID",scanner.getFrameID().toString());
        scannersFile.set(name+".mode",scanner.getMode()+"");
    }
    public void removeScanner(Scanner scanner) {
        String name = scanner.getName();
        scanners.remove(name);
        scannersFile.set(name,null);
    }

}
