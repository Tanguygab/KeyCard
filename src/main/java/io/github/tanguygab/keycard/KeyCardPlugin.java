package io.github.tanguygab.keycard;

import io.github.tanguygab.keycard.config.ConfigurationFile;
import io.github.tanguygab.keycard.config.YamlConfigurationFile;
import io.github.tanguygab.keycard.scanner.Scanner;
import io.github.tanguygab.keycard.scanner.ScannerMode;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;

public final class KeyCardPlugin extends JavaPlugin implements CommandExecutor {

    private static KeyCardPlugin instance;

    public ConfigurationFile configFile;
    public ConfigurationFile scannersFile;
    public final Map<String, Scanner> scanners = new HashMap<>();
    public Image image;

    public static KeyCardPlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        File fileImage = new File(getDataFolder(),("/scanner.png"));
        try {
            image = fileImage.exists() ? new ImageIcon(fileImage.toURI().toURL()).getImage() : new ImageIcon(getClass().getResource("/scanner.png")).getImage();
            if (image != null) {
                BufferedImage resizedImage = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics2D = resizedImage.createGraphics();
                graphics2D.drawImage(image, 0, 0, 128, 128, null);
                graphics2D.dispose();
                image = resizedImage;
            }
            configFile = new YamlConfigurationFile(getResource("config.yml"), new File(getDataFolder(), "config.yml"));

            File fileScanners = new File(getDataFolder(), "scanners.yml");
            if (!fileScanners.exists()) fileScanners.createNewFile();
            scannersFile = new YamlConfigurationFile(null, fileScanners);

            ScannerMode.allowedModes = new ArrayList<>();
            List<String> modes = configFile.getStringList("scanner.allowed-modes");
            modes.forEach(str-> {
                ScannerMode mode = ScannerMode.get(str,false);
                if (mode != null)
                    ScannerMode.allowedModes.add(mode);
            });
            if (ScannerMode.allowedModes.isEmpty()) ScannerMode.allowedModes.add(ScannerMode.ACTIVE_ON_SWIPE);

            NamespacedKey scannerKey = new NamespacedKey(this, "scanner");
            if (configFile.getBoolean("scanner.craft",true)) {
                ShapedRecipe scannerRecipe = new ShapedRecipe(scannerKey, Utils.craftScanner());
                scannerRecipe.shape(
                        " R ",
                        "BF ",
                        " R ");
                scannerRecipe.setIngredient('B', Material.STONE_BUTTON);
                scannerRecipe.setIngredient('F', Material.ITEM_FRAME);
                scannerRecipe.setIngredient('R', Material.REDSTONE);
                if (getServer().getRecipe(scannerKey) != null) getServer().removeRecipe(scannerKey);
                getServer().addRecipe(scannerRecipe);
            } else if (getServer().getRecipe(scannerKey) != null) getServer().removeRecipe(scannerKey);

            NamespacedKey keycardKey = new NamespacedKey(this, "keycard");
            if (configFile.getBoolean("keycard.craft",true)) {
                ShapelessRecipe keycardRecipe = new ShapelessRecipe(keycardKey, Utils.craftKeycard(KeyCardEnum.KEYCARD));
                keycardRecipe.addIngredient(Material.PAPER);
                keycardRecipe.addIngredient(Material.REDSTONE);
                if (getServer().getRecipe(keycardKey) != null) getServer().removeRecipe(keycardKey);
                getServer().addRecipe(keycardRecipe);
            } else if (getServer().getRecipe(keycardKey) != null) getServer().removeRecipe(keycardKey);

            NamespacedKey multiCardKey = new NamespacedKey(this, "multi-card");
            if (configFile.getBoolean("multi-card.enabled",true) && configFile.getBoolean("multi-card.craft",true)) {
                ShapedRecipe multiCardRecipe = new ShapedRecipe(multiCardKey, Utils.craftKeycard(KeyCardEnum.MULTI_CARD));
                multiCardRecipe.shape(
                        " R ",
                        " B ",
                        "PPP");
                multiCardRecipe.setIngredient('B', Material.BOOK);
                multiCardRecipe.setIngredient('P', Material.PAPER);
                multiCardRecipe.setIngredient('R', Material.REDSTONE);
                if (getServer().getRecipe(multiCardKey) != null) getServer().removeRecipe(multiCardKey);
                getServer().addRecipe(multiCardRecipe);
            } else if (getServer().getRecipe(scannerKey) != null) getServer().removeRecipe(scannerKey);

            NamespacedKey remoteCardKey = new NamespacedKey(this, "remote-card");
            if (configFile.getBoolean("remote-card.enabled",true) && configFile.getBoolean("remote-card.craft",true)) {
                ShapedRecipe remoteCardRecipe = new ShapedRecipe(remoteCardKey, Utils.craftKeycard(KeyCardEnum.REMOTE_CARD));
                remoteCardRecipe.shape(
                        " R ",
                        "ECE",
                        "PPP");
                remoteCardRecipe.setIngredient('E', Material.ENDER_PEARL);
                remoteCardRecipe.setIngredient('C', Material.COMPARATOR);
                remoteCardRecipe.setIngredient('P', Material.PAPER);
                remoteCardRecipe.setIngredient('R', Material.REDSTONE);
                if (getServer().getRecipe(remoteCardKey) != null) getServer().removeRecipe(remoteCardKey);
                getServer().addRecipe(remoteCardRecipe);
            } else if (getServer().getRecipe(scannerKey) != null) getServer().removeRecipe(scannerKey);

            Map<String,Object> cfgmap = new HashMap<>(scannersFile.getValues());
            for (String name : cfgmap.keySet()) {
                Map<String,String> map = (Map<String, String>) cfgmap.get(name);
                Scanner scanner = new Scanner(name, UUID.fromString(map.get("frameID")), UUID.fromString(map.get("player")), ScannerMode.get(map.get("mode"),true));
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
                Player player = args.length > 2 ? getServer().getPlayer(args[1]) : (Player) sender;
                if (player == null) {
                    sender.sendMessage("This player isn't online!");
                    return true;
                }
                PlayerInventory inv = player.getInventory();
                ItemStack item = null;
                switch (arg.toLowerCase()) {
                    case "keycard" -> item = Utils.craftKeycard(KeyCardEnum.KEYCARD);
                    case "multi-card","remote-card" -> {
                        if (!configFile.getBoolean(arg.toLowerCase()+".enabled",true)) {
                            sender.sendMessage("This keycard is disabled.");
                            return true;
                        }
                        item = Utils.craftKeycard(KeyCardEnum.get(arg));
                    }
                    case "scanner" -> item = Utils.craftScanner();
                    default -> sender.sendMessage("You have to specify `keycard` or `scanner`");
                }
                if (item != null) {
                    inv.addItem(item);
                    sender.sendMessage("Gave 1 "+item.getItemMeta().getDisplayName()+ ChatColor.RESET +" to "+player.getName());
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
        if ("give".equalsIgnoreCase(args[0]) && args.length == 2) {
            List<String> items = new ArrayList<>(List.of("keycard","scanner"));
            if (configFile.getBoolean("multi-card.enabled",true)) items.add("multi-card");
            if (configFile.getBoolean("remote-card.enabled",true)) items.add("remote-card");
            return items;
        }
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
