package io.github.tanguygab.keycard;

import io.github.tanguygab.keycard.scanner.RenamingScanner;
import io.github.tanguygab.keycard.scanner.Scanner;
import io.github.tanguygab.keycard.scanner.ScannerMode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Listener implements org.bukkit.event.Listener {

    private final KeyCardPlugin plugin;
    private final Map<Player, RenamingScanner> renaming = new HashMap<>();

    public Listener(KeyCardPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onScannerPlace(HangingPlaceEvent e) {
        ItemStack item = e.getItemStack();
        if (item == null || item.getType() != Material.ITEM_FRAME || item.getItemMeta() == null) return;
        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        if (!data.has(Utils.isScannerKey, PersistentDataType.BYTE)) return;
        boolean isScanner = data.get(Utils.isScannerKey,PersistentDataType.BYTE) == (byte)1;
        if (!isScanner) return;
        Entity entity = e.getEntity();
        if (entity.getType() != EntityType.ITEM_FRAME) return;
        Player p = e.getPlayer();
        renaming.put(p,new RenamingScanner(p,entity));
    }

    @EventHandler
    public void onScannerBreak(HangingBreakEvent e) {
        Scanner scanner = plugin.getScanner(e.getEntity());
        if (scanner == null) return;
        onScannerBreak(e.getEntity().getLocation(),scanner);
    }

    @EventHandler
    public void onItemInFrameBreak(EntityDamageByEntityEvent e) {
        Scanner scanner = plugin.getScanner(e.getEntity());
        if (scanner == null) return;
        Entity entity = e.getEntity();
        Location loc = entity.getLocation();
        onScannerBreak(loc,scanner);
        entity.remove();
        loc.getBlock().setType(Material.AIR);
    }

    public void onScannerBreak(Location loc, Scanner scanner) {
        plugin.removeScanner(scanner);
        loc.getWorld().dropItem(loc,Utils.craftScanner());
    }

    @EventHandler
    public void onScannerRename(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!renaming.containsKey(p)) return;
        e.setCancelled(true);
        String name = e.getMessage();
        RenamingScanner rename = renaming.get(p);
        plugin.getServer().getScheduler().runTask(plugin,()->{
            boolean renamed = rename.renamed(name);
            if (renamed) renaming.remove(p);
        });
    }

    @EventHandler
    public void onScannerClick(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        Entity entity = e.getRightClicked();
        Scanner scanner = plugin.getScanner(entity);
        if (scanner == null) return;
        e.setCancelled(true);
        onClick(p,scanner,e.getHand(),entity.getLocation().getBlock());
    }

    public boolean onClick(Player p, Scanner scanner, EquipmentSlot hand, Block block) {
        if (scanner.getOwner().toString().equals(p.getUniqueId().toString()) && p.isSneaking()) {
            if (hand == EquipmentSlot.HAND)
                scanner.open(p);
            return false;
        }

        if (!scanner.canUse(p.getInventory().getItem(hand))) return false;
        Powerable data = (Powerable) block.getBlockData();
        boolean mode = scanner.getMode() == ScannerMode.ACTIVE_ON_SWIPE;
        data.setPowered(mode);

        block.setBlockData(data);
        plugin.getServer().getScheduler().runTaskLater(plugin,()->{
            data.setPowered(!mode);
            block.setBlockData(data);
        },15);
        return true;
    }

    @EventHandler
    public void onScannerButtonClick(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        if (block == null || block.getType() != Material.STONE_BUTTON) return;

        Location loc = block.getLocation();
        for (Scanner scanner : plugin.scanners.values()) {
            Entity entity = plugin.getServer().getEntity(scanner.getFrameID());
            if (entity.getLocation().getBlock().getLocation().equals(loc)) {
                e.setCancelled(!onClick(e.getPlayer(),scanner,e.getHand(),block));
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().startsWith("Scanner menu: ")) return;
        e.setCancelled(true);

        Player p = plugin.getServer().getPlayer(e.getWhoClicked().getUniqueId());
        String scannerName = e.getView().getTitle().replace("Scanner menu: ", "");
        ItemStack item = e.getCurrentItem();
        if (item == null) return;

        String name = item.getItemMeta() == null ? "" : ChatColor.stripColor(item.getItemMeta().getDisplayName());
        switch (name) {
            case "Link Keycard" -> {
                ItemStack card = e.getView().getItem(1);
                if (!Utils.isKeycard(card)) {
                    p.sendMessage("You need to insert a keycard to link!");
                    return;
                }
                ItemMeta meta = card.getItemMeta();
                meta.setLore(List.of("",Utils.colors("&7Scanner: &f")+scannerName));
                meta.getPersistentDataContainer().set(Utils.scannerIdKey,PersistentDataType.STRING,scannerName);
                card.setItemMeta(meta);
                p.sendMessage("Keycard linked!");
            }
            case ("Switch Mode") -> {
                Scanner scanner = plugin.scanners.get(scannerName);
                ScannerMode newMode = scanner.switchMode();

                ItemMeta meta = item.getItemMeta();
                meta.setLore(List.of("",Utils.colors("&7Mode: ")+newMode.getDesc()));
                item.setItemMeta(meta);
                item.setType(newMode.getMat());
            }
            default -> {
                ItemStack cursor = e.getCursor();
                if (item.getType().isAir()) {
                    if (cursor.getItemMeta() == null) return;
                    if (cursor.getItemMeta().getPersistentDataContainer().has(Utils.isKeycardKey,PersistentDataType.BYTE))
                        e.setCancelled(false);
                } else {
                    if (item.getItemMeta() == null) return;
                    if (item.getItemMeta().getPersistentDataContainer().has(Utils.isKeycardKey, PersistentDataType.BYTE))
                        e.setCancelled(false);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!e.getView().getTitle().startsWith("Scanner menu: ")) return;
        ItemStack card = e.getView().getItem(1);
        if (Utils.isKeycard(card)) e.getPlayer().getInventory().addItem(card);
    }

    @EventHandler
    public void checkingIfItemFramesAreLoaded(ChunkLoadEvent e) {
        plugin.scanners.forEach((s, scanner) -> {
            if (scanner.isLoaded()) return;
            Entity entity = Bukkit.getServer().getEntity(scanner.getFrameID());
            if (entity == null) return;
            Utils.addMap(entity,scanner);
            scanner.setLoaded();
        });
    }

}
