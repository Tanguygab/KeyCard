package io.github.tanguygab.keycard;

import io.github.tanguygab.keycard.scanner.Scanner;
import io.github.tanguygab.keycard.scanner.ScannerMode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static NamespacedKey scannerIdKey = NamespacedKey.fromString("scanner-id",KeyCardPlugin.get());
    public static NamespacedKey isScannerKey = NamespacedKey.fromString("is-scanner",KeyCardPlugin.get());
    public static NamespacedKey isKeycardKey = NamespacedKey.fromString("is-keycard",KeyCardPlugin.get());

    public static String colors(String str) {
        return ChatColor.translateAlternateColorCodes('&',str);
    }

    public static void addMap(Entity entity, Scanner scanner) {
        ItemFrame frame = (ItemFrame) entity;
        if (!frame.isFixed()) frame.setFixed(true);
        ItemStack map = frame.getItem().getType() == Material.FILLED_MAP ? frame.getItem() : new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) map.getItemMeta();
        MapView view = Bukkit.getServer().createMap(Bukkit.getServer().getWorlds().get(0));
        view.getRenderers().clear();
        if (KeyCardPlugin.get().configFile.getBoolean("scanner.image",true) && KeyCardPlugin.get().image != null)
            view.addRenderer(new MapRender());
        meta.setMapView(view);
        map.setItemMeta(meta);
        frame.setItem(map);

        Block block = entity.getLocation().getBlock();
        if (block.getType() != Material.STONE_BUTTON)
            block.setType(Material.STONE_BUTTON);
        BlockData data = Bukkit.getServer().createBlockData(Material.STONE_BUTTON);
        if (data instanceof Directional directional) directional.setFacing(entity.getFacing());
        if (data instanceof Powerable powerable) powerable.setPowered(scanner.getMode() == ScannerMode.INACTIVE_ON_SWIPE);
        block.setBlockData(data);
    }


    public static ItemStack craftScanner() {
        ItemStack scanner = new ItemStack(Material.ITEM_FRAME);
        ItemMeta meta = scanner.getItemMeta();
        meta.setDisplayName(colors("&8[&6Scanner&8]"));
        meta.getPersistentDataContainer().set(isScannerKey, PersistentDataType.BYTE,(byte)1);
        scanner.setItemMeta(meta);
        return scanner;
    }
    public static ItemStack craftKeycard() {
        ItemStack keycard = new ItemStack(Material.PAPER);
        ItemMeta meta = keycard.getItemMeta();
        meta.setDisplayName(colors("&8[&6Keycard&8]"));
        meta.setLore(List.of("",colors("&7Scanner: &fNot Linked")));
        meta.getPersistentDataContainer().set(isKeycardKey,PersistentDataType.BYTE,(byte)1);
        keycard.setItemMeta(meta);
        return keycard;
    }

    public static ItemStack craftMultiKeycard() {
        ItemStack keycard = new ItemStack(Material.BOOK);
        ItemMeta meta = keycard.getItemMeta();
        meta.setDisplayName(colors("&8[&6Multi-KeyCard&8]"));
        meta.setLore(List.of("",colors("&7Scanners:")));
        meta.getPersistentDataContainer().set(isKeycardKey,PersistentDataType.BYTE,(byte)2);
        keycard.setItemMeta(meta);
        return keycard;
    }

    public static boolean isKeycard(ItemStack card) {
        return card != null && !card.getType().isAir() && card.getItemMeta() != null && card.getItemMeta().getPersistentDataContainer().has(isKeycardKey,PersistentDataType.BYTE);
    }

    public static void linkScannerToCard(ItemStack card, Scanner scanner) {
        ItemMeta meta = card.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        switch (data.get(isKeycardKey,PersistentDataType.BYTE)) {
            case 1 -> {
                if (scanner.getFrameID().toString().equals(data.get(scannerIdKey,PersistentDataType.STRING))) return;
                data.set(scannerIdKey,PersistentDataType.STRING,scanner.getFrameID().toString());
                meta.setLore(List.of("",colors("&7Scanner: &f")+scanner.getName()));
            }
            case 2 -> {
                String scanners = data.get(scannerIdKey,PersistentDataType.STRING);
                if (scanners == null) scanners = "";
                if (List.of(scanners.split("\\|\\|")).contains(scanner.getFrameID().toString())) return;
                scanners+=(scanners.equals("") ? "" : "||")+scanner.getFrameID().toString();
                data.set(scannerIdKey,PersistentDataType.STRING,String.join("||",scanners));
                List<String> lore = meta.getLore();
                lore.add(colors(" &7- &f"+scanner.getName()));
                meta.setLore(lore);
            }
        }
        card.setItemMeta(meta);
    }

    public static void unlinkScannerToCard(ItemStack card, Scanner scanner) {
        ItemMeta meta = card.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        switch (data.get(isKeycardKey,PersistentDataType.BYTE)) {
            case 1 -> {
                meta.setLore(List.of("",colors("&7Scanner: &fNot Linked")));
                meta.getPersistentDataContainer().remove(scannerIdKey);
            }
            case 2 -> {
                List<String> lore = meta.getLore();
                lore.remove(colors(" &7- &f" + scanner.getName()));
                meta.setLore(lore);
                String scanners = data.get(scannerIdKey,PersistentDataType.STRING);
                List<String> scannersList = new ArrayList<>(List.of(scanners.split("\\|\\|")));
                scannersList.remove(scanner.getFrameID().toString());
                data.set(scannerIdKey,PersistentDataType.STRING,String.join("||",scannersList));
            }
        }
        card.setItemMeta(meta);
    }

}
