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
import org.bukkit.persistence.PersistentDataType;

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
        ItemStack map =  frame.getItem().getType() == Material.FILLED_MAP ? frame.getItem() : new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) map.getItemMeta();
        MapView view = Bukkit.getServer().createMap(Bukkit.getServer().getWorlds().get(0));
        view.getRenderers().clear();
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


    public static ItemStack craftKeycard() {
        ItemStack keycard = new ItemStack(Material.PAPER);
        ItemMeta meta = keycard.getItemMeta();
        meta.setDisplayName(Utils.colors("&8[&6Keycard&8]"));
        meta.setLore(List.of("",Utils.colors("&7Scanner: &fNot Linked")));
        meta.getPersistentDataContainer().set(isKeycardKey,PersistentDataType.BYTE,(byte)1);
        keycard.setItemMeta(meta);
        return keycard;
    }

    public static ItemStack craftScanner() {
        ItemStack scanner = new ItemStack(Material.ITEM_FRAME);
        ItemMeta meta = scanner.getItemMeta();
        meta.setDisplayName(Utils.colors("&8[&6Scanner&8]"));
        meta.getPersistentDataContainer().set(isScannerKey, PersistentDataType.BYTE,(byte)1);
        scanner.setItemMeta(meta);
        return scanner;
    }

    public static boolean isKeycard(ItemStack card) {
        return card != null && !card.getType().isAir() && card.getItemMeta() != null && card.getItemMeta().getPersistentDataContainer().has(Utils.isKeycardKey,PersistentDataType.BYTE);
    }

}
