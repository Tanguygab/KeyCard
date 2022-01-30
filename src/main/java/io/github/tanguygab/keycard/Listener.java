package io.github.tanguygab.keycard;

import io.github.tanguygab.keycard.scanner.NamingScanner;
import io.github.tanguygab.keycard.scanner.Scanner;
import io.github.tanguygab.keycard.scanner.ScannerMode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
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
    private final Map<Player, NamingScanner> naming = new HashMap<>();

    public Listener(KeyCardPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onScannerPlace(HangingPlaceEvent e) {
        ItemStack item = e.getItemStack();
        if (item == null || item.getType() != Material.ITEM_FRAME || item.getItemMeta() == null) return;
        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        if (!data.has(Utils.isScannerKey, PersistentDataType.BYTE)) return;
        boolean isScanner = data.get(Utils.isScannerKey,PersistentDataType.BYTE) == 1;
        if (!isScanner) return;
        Entity entity = e.getEntity();
        if (entity.getType() != EntityType.ITEM_FRAME) return;
        ((ItemFrame)entity).setFixed(true);
        Player p = e.getPlayer();
        naming.put(p,new NamingScanner(p,entity));
    }

    @EventHandler
    public void onScannerBreakInCreative(HangingBreakByEntityEvent e) {
        for (Player p : naming.keySet()) {
            NamingScanner rs = naming.get(p);
            if (rs.getFrame() == e.getEntity()) {
                naming.remove(p);
                p.sendMessage("Scanner creation cancelled");
                return;
            }
        }
        Scanner scanner = plugin.getScanner(e.getEntity());
        if (scanner == null) return;
        if (e.getRemover() == null || !e.getRemover().getUniqueId().toString().equals(scanner.getOwner().toString())){
            e.setCancelled(true);
            return;
        }
        plugin.removeScanner(scanner);
        Location loc = e.getEntity().getLocation();
        loc.getBlock().setType(Material.AIR);
        ((Player)e.getRemover()).getInventory().addItem(Utils.craftScanner());
    }

    @EventHandler
    public void onScannerRename(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!naming.containsKey(p)) return;
        e.setCancelled(true);
        String name = e.getMessage();
        NamingScanner name2 = naming.get(p);
        plugin.getServer().getScheduler().runTask(plugin,()->{
            boolean renamed = name2.named(name);
            if (renamed) naming.remove(p);
        });
    }

    @EventHandler
    public void onScannerClick(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        Entity entity = e.getRightClicked();
        Scanner scanner = plugin.getScanner(entity);
        if (scanner == null) return;
        e.setCancelled(true);
        onClick(p,scanner,e.getHand());
    }

    public void onClick(Player p, Scanner scanner, EquipmentSlot hand) {
        ItemStack card = p.getInventory().getItem(hand);
        byte type = Utils.getKeyCardType(card);
        if (type == 3 && !plugin.configFile.getBoolean("remote-card.enabled",true)) return;

        if (scanner.getOwner().toString().equals(p.getUniqueId().toString()) && p.isSneaking()) {
            if (hand == EquipmentSlot.HAND)
                scanner.open(p);
            return;
        }

        if (type == 2 && !plugin.configFile.getBoolean("multi-card.enabled",true)) return;
        if (!scanner.canUse(card)) return;

        ScannerMode mode = scanner.getMode();
        mode.load(scanner);
        plugin.getServer().getScheduler().runTaskLater(plugin,()-> mode.unload(scanner),15);
    }

    @EventHandler
    public void onScannerButtonClick(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        if (block == null || block.getType() != Material.STONE_BUTTON) return;

        Location loc = block.getLocation();
        for (Scanner scanner : plugin.scanners.values()) {
            Entity entity = plugin.getServer().getEntity(scanner.getFrameID());
            if (entity != null && entity.getLocation().getBlock().getLocation().equals(loc)) {
                onClick(e.getPlayer(),scanner,e.getHand());
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onRemoteCardClick(PlayerInteractEvent e) {
        ItemStack card = e.getItem();
        if (card == null || card.getType() != KeyCardEnum.REMOTE_CARD.getMat() || Utils.getKeyCardType(card) != 3) return;
        e.setCancelled(true);

        Scanner scanner = Utils.getScanner(card);
        if (scanner == null) return;
        onClick(e.getPlayer(),scanner,e.getHand());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().startsWith("Scanner menu: ")) return;
        e.setCancelled(true);

        Player p = plugin.getServer().getPlayer(e.getWhoClicked().getUniqueId());
        assert p != null;
        String scannerName = e.getView().getTitle().replace("Scanner menu: ", "");
        Scanner scanner = plugin.scanners.get(scannerName);
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
                Utils.linkScannerToCard(card,scanner);
                p.sendMessage("Keycard linked!");
                switchLinkItem(item,true);
            }
            case "Unlink Keycard" -> {
                ItemStack card = e.getView().getItem(1);
                if (!Utils.isKeycard(card)) {
                    p.sendMessage("You need to insert a keycard to link!");
                    return;
                }

                Utils.unlinkScannerToCard(card,scanner);
                p.sendMessage("Keycard unlinked!");
                switchLinkItem(item,false);
            }
            case "Switch Mode" -> {
                ScannerMode newMode = scanner.switchMode();

                ItemMeta meta = item.getItemMeta();
                meta.setLore(List.of("",Utils.colors("&7Mode: ")+newMode.getDesc()));
                item.setItemMeta(meta);
                item.setType(newMode.getMat());
            }
             case "Delete Scanner" -> {
                p.closeInventory();
                plugin.removeScanner(scanner);
                Entity entity = plugin.getServer().getEntity(scanner.getFrameID());
                entity.getLocation().getBlock().setType(Material.AIR);
                entity.remove();
                p.getInventory().addItem(Utils.craftScanner());
            }
            default -> {
                ItemStack cursor = e.getCursor();
                if (item.getType().isAir()) {
                    processKeyCardMove(cursor,e,scanner);
                } else {
                    processKeyCardMove(item,e,scanner);
                }
            }
        }
    }

    public void processKeyCardMove(ItemStack item, InventoryClickEvent e, Scanner scanner) {
        if (e.isShiftClick()) return; // because ofc you can't get the slot where it got shift clicked
        if (item == null || item.getItemMeta() == null) return;
        if (!item.getItemMeta().getPersistentDataContainer().has(Utils.isKeycardKey,PersistentDataType.BYTE)) return;
        e.setCancelled(false);

        if (e.getRawSlot() != 1) return;
        ItemStack card = e.getCursor();
        ItemStack linkItem = e.getView().getItem(0);
        boolean canUse = scanner.canUse(card);
        switchLinkItem(linkItem,canUse);


    }
    public void switchLinkItem(ItemStack linkItem, boolean canUse) {
        ItemMeta linkItemMeta = linkItem.getItemMeta();
        String linkItemName = ChatColor.stripColor(linkItemMeta.getDisplayName());
        if (linkItemName.equals("Link Keycard") && canUse) {
            linkItemMeta.setDisplayName(Utils.colors("&fUnlink Keycard"));
            linkItemMeta.setLore(List.of("",Utils.colors("&7Keycard already linked!"),Utils.colors("&7Click to unlink it")));
        } else if (linkItemName.equals("Unlink Keycard") && !canUse) {
            linkItemMeta.setDisplayName(Utils.colors("&fLink Keycard"));
            linkItemMeta.setLore(List.of("",Utils.colors("&7Insert your keycard in"),Utils.colors("&7the next slot to link it")));
        }
        linkItem.setItemMeta(linkItemMeta);
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
