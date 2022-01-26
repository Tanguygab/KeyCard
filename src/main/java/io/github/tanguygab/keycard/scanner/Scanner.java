package io.github.tanguygab.keycard.scanner;

import io.github.tanguygab.keycard.KeyCardPlugin;
import io.github.tanguygab.keycard.MapRender;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scanner {

    private final String name;
    private final UUID frameID;
    private final UUID owner;
    private ScannerMode mode;

    public Scanner(String name, UUID frameID, UUID owner) {
        this(name,frameID,owner, ScannerMode.ACTIVE_ON_SWIPE);
    }

    public Scanner(String name, UUID frameID, UUID owner, ScannerMode mode) {
        this.name = name;
        this.frameID = frameID;
        this.owner = owner;
        this.mode = mode;
        Entity entity = Bukkit.getServer().getEntity(frameID);
        if (entity == null) {
            KeyCardPlugin.getInstance().removeScanner(this);
            return;
        }

        ItemStack map = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) map.getItemMeta();
        MapView view = Bukkit.getServer().createMap(Bukkit.getServer().getWorlds().get(0));
        view.addRenderer(new MapRender());
        meta.setMapView(view);
        map.setItemMeta(meta);
        ((ItemFrame)entity).setItem(map);

        Block block = entity.getLocation().getBlock();
        if (block.getType() != Material.STONE_BUTTON)
            block.setType(Material.STONE_BUTTON);
        BlockData data = Bukkit.getServer().createBlockData(Material.STONE_BUTTON);
        if (data instanceof Directional directional) directional.setFacing(entity.getFacing());
        if (data instanceof Powerable powerable) powerable.setPowered(mode == ScannerMode.INACTIVE_ON_SWIPE);
        block.setBlockData(data);
    }

    public Scanner(String name, Map<String,Object> config) {
        this(name,UUID.fromString(config.get("frameID")+""),UUID.fromString(config.get("player")+""),ScannerMode.valueOf(config.get("mode")+""));
    }

    public String getName() {
        return name;
    }
    public UUID getFrameID() {
        return frameID;
    }
    public UUID getOwner() {
        return owner;
    }
    public ScannerMode getMode() {
        return mode;
    }

    public boolean canUse(ItemStack keycard) {
        if (keycard.getItemMeta() == null || !keycard.getItemMeta().getPersistentDataContainer().has(KeyCardPlugin.getInstance().scannerIdKey,PersistentDataType.STRING)) return false;
        return name.equals(keycard.getItemMeta().getPersistentDataContainer().get(KeyCardPlugin.getInstance().scannerIdKey, PersistentDataType.STRING));
    }

    public ScannerMode switchMode() {
        mode = ScannerMode.switchMode(mode);
        KeyCardPlugin.getInstance().scannersFile.set(name+".mode",mode+"");

        Block block = Bukkit.getServer().getEntity(frameID).getLocation().getBlock();
        Powerable data = (Powerable)block.getBlockData();
        data.setPowered(mode == ScannerMode.INACTIVE_ON_SWIPE);
        block.setBlockData(data);

        return mode;
    }

    public void open(Player p) {

        Inventory inv = Bukkit.getServer().createInventory(null, InventoryType.HOPPER, "Scanner menu: "+name);

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta metaFiller = filler.getItemMeta();
        metaFiller.setDisplayName(KeyCardPlugin.colors("&r"));
        filler.setItemMeta(metaFiller);
        inv.setItem(2,filler);
        inv.setItem(3,filler);

        ItemStack linker = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta metaLinker = linker.getItemMeta();
        metaLinker.setDisplayName(KeyCardPlugin.colors("&fLink Keycard"));
        metaLinker.setLore(List.of("",KeyCardPlugin.colors("&7Insert your keycard in"),KeyCardPlugin.colors("&7the next slot to link it")));
        linker.setItemMeta(metaLinker);
        inv.setItem(0,linker);

        ItemStack mode = new ItemStack(this.mode.getMat());
        ItemMeta metaMode = mode.getItemMeta();
        metaMode.setDisplayName(KeyCardPlugin.colors("&fSwitch Mode"));
        metaMode.setLore(List.of("",KeyCardPlugin.colors("&7Mode: ")+this.mode.getDesc()));
        mode.setItemMeta(metaMode);
        inv.setItem(4,mode);

        p.openInventory(inv);
    }
}
