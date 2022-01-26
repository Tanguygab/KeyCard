package io.github.tanguygab.keycard;

import io.github.tanguygab.keycard.config.ConfigurationFile;
import io.github.tanguygab.keycard.config.YamlConfigurationFile;
import io.github.tanguygab.keycard.scanner.Scanner;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class KeyCardPlugin extends JavaPlugin implements CommandExecutor {

    private static KeyCardPlugin instance;

    public ConfigurationFile scannersFile;
    public final Map<String, Scanner> scanners = new HashMap<>();
    public NamespacedKey scannerIdKey = NamespacedKey.fromString("scanner-id",this);
    public NamespacedKey isScannerKey = NamespacedKey.fromString("is-scanner",this);
    public NamespacedKey isKeycardKey = NamespacedKey.fromString("is-keycard",this);

    public static KeyCardPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        File file = new File(getDataFolder(), "scanners.yml");
        try {
            if (!file.exists()) file.createNewFile();
            scannersFile = new YamlConfigurationFile(null, file);
        } catch (Exception e) {e.printStackTrace();}
        scannersFile.getValues().forEach((name,map)-> scanners.put(name,new Scanner(name, (Map<String, Object>) map)));
        getServer().getPluginManager().registerEvents(new Listener(this),this);
    }
    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(colors("&m                                        \n"
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
                        ItemStack keycard = new ItemStack(Material.PAPER);
                        ItemMeta meta = keycard.getItemMeta();
                        meta.setDisplayName(colors("&8[&6Keycard&8]"));
                        meta.setLore(List.of("",colors("&7Scanner: &fNot Linked")));
                        meta.getPersistentDataContainer().set(isKeycardKey,PersistentDataType.BYTE,(byte)1);
                        keycard.setItemMeta(meta);
                        inv.addItem(keycard);
                    }
                    case "scanner" -> {
                        ItemStack scanner = new ItemStack(Material.ITEM_FRAME);
                        ItemMeta meta = scanner.getItemMeta();
                        meta.setDisplayName(colors("&8[&6Scanner&8]"));
                        meta.getPersistentDataContainer().set(isScannerKey,PersistentDataType.BYTE,(byte)1);
                        scanner.setItemMeta(meta);
                        inv.addItem(scanner);
                    }
                    default -> sender.sendMessage("You have to specify `keycard` or `scanner`");
                }
            }
            case "name" -> {

            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("give","name");
        if ("give".equalsIgnoreCase(args[0]) && args.length == 2) return List.of("keycard","scanner");
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

    public static String colors(String str) {
        return ChatColor.translateAlternateColorCodes('&',str);
    }
}
