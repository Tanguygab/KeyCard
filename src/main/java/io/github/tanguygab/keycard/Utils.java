package io.github.tanguygab.keycard;

import io.github.tanguygab.keycard.events.KeyCardLinkEvent;
import io.github.tanguygab.keycard.scanner.Scanner;
import io.github.tanguygab.keycard.scanner.ScannerMode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
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
    public static NamespacedKey keycardTypeKey = NamespacedKey.fromString("keycard-type",KeyCardPlugin.get());

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
        if (data instanceof Directional directional) {
            if (entity.getFacing() != BlockFace.DOWN && entity.getFacing() != BlockFace.UP)
                directional.setFacing(entity.getFacing());
            else if (data instanceof FaceAttachable fa)
                fa.setAttachedFace(entity.getFacing() == BlockFace.UP ? FaceAttachable.AttachedFace.FLOOR : FaceAttachable.AttachedFace.CEILING);
        }
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
    public static ItemStack craftKeycard(KeyCardEnum type) {
        ItemStack keycard = new ItemStack(type.getMat());
        ItemMeta meta = keycard.getItemMeta();
        meta.setDisplayName(colors("&8[&6"+type.getName()+"&8]"));
        meta.setLore(List.of("",colors("&7Scanner"+(type == KeyCardEnum.MULTI_CARD ? "s:" : ": &fNot Linked"))));
        meta.getPersistentDataContainer().set(keycardTypeKey,PersistentDataType.STRING,type.getType());
        keycard.setItemMeta(meta);
        return keycard;
    }

    public static boolean isKeycard(ItemStack card) {
        return getKeyCardType(card) != null;
    }
    public static String getKeyCardType(ItemStack card) {
        if (card == null) return null;
        ItemMeta meta = card.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        if (!data.has(keycardTypeKey,PersistentDataType.STRING)) return null;
        return data.get(keycardTypeKey,PersistentDataType.STRING);
    }
    public static Scanner getScanner(ItemStack card) {
        if (card == null) return null;
        ItemMeta meta = card.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        if (!data.has(scannerIdKey,PersistentDataType.STRING)) return null;
        String uuid = data.get(scannerIdKey,PersistentDataType.STRING);
        for (Scanner scanner : KeyCardPlugin.get().scanners.values()) {
            if (scanner.getFrameID().toString().equals(uuid))
                return scanner;
        }
        return null;
    }

    public static void linkScannerToCard(ItemStack card, Scanner scanner) {
        ItemMeta meta = card.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        String type = data.get(keycardTypeKey,PersistentDataType.STRING);
        if (type == null) return;
        switch (type) {
            case "normal","remote" -> {
                if (scanner.getFrameID().toString().equals(data.get(scannerIdKey,PersistentDataType.STRING))) return;
                data.set(scannerIdKey,PersistentDataType.STRING,scanner.getFrameID().toString());
                meta.setLore(List.of("",colors("&7Scanner: &f")+scanner.getName()));
            }
            case "multi" -> {
                String scanners = data.get(scannerIdKey,PersistentDataType.STRING);
                if (scanners == null) scanners = "";
                if (List.of(scanners.split("\\|\\|")).contains(scanner.getFrameID().toString())) return;
                scanners+=(scanners.equals("") ? "" : "||")+scanner.getFrameID().toString();
                data.set(scannerIdKey,PersistentDataType.STRING,String.join("||",scanners));
                List<String> lore = meta.getLore();
                lore.add(colors(" &7- &f"+scanner.getName()));
                meta.setLore(lore);
            }
            default -> {
                KeyCardLinkEvent event = new KeyCardLinkEvent(card,scanner,type,false);
                Bukkit.getServer().getPluginManager().callEvent(event);
            }
        }
        card.setItemMeta(meta);
    }

    public static void unlinkScannerToCard(ItemStack card, Scanner scanner) {
        ItemMeta meta = card.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        String type = data.get(keycardTypeKey,PersistentDataType.STRING);
        if (type == null) return;
        switch (type) {
            case "normal","remote" -> {
                meta.setLore(List.of("",colors("&7Scanner: &fNot Linked")));
                meta.getPersistentDataContainer().remove(scannerIdKey);
            }
            case "multi" -> {
                List<String> lore = meta.getLore();
                lore.remove(colors(" &7- &f" + scanner.getName()));
                meta.setLore(lore);
                String scanners = data.get(scannerIdKey,PersistentDataType.STRING);
                List<String> scannersList = new ArrayList<>(List.of(scanners.split("\\|\\|")));
                scannersList.remove(scanner.getFrameID().toString());
                data.set(scannerIdKey,PersistentDataType.STRING,String.join("||",scannersList));
            }
            default -> {
                KeyCardLinkEvent event = new KeyCardLinkEvent(card,scanner,type,true);
                Bukkit.getServer().getPluginManager().callEvent(event);
            }
        }
        card.setItemMeta(meta);
    }

}
