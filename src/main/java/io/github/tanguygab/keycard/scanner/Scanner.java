package io.github.tanguygab.keycard.scanner;

import io.github.tanguygab.keycard.KeyCardPlugin;
import io.github.tanguygab.keycard.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public class Scanner {

    private final String name;
    private final UUID frameID;
    private final UUID owner;
    private ScannerMode mode;
    private boolean loaded = false;

    public Scanner(String name, UUID frameID, UUID owner) {
        this(name,frameID,owner, ScannerMode.ACTIVE_ON_SWIPE);
    }

    public Scanner(String name, UUID frameID, UUID owner, ScannerMode mode) {
        this.name = name;
        this.frameID = frameID;
        this.owner = owner;
        this.mode = mode;

        Entity entity = Bukkit.getServer().getEntity(frameID);
        if (entity == null) return;
        Utils.addMap(entity,this);
        setLoaded();

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
    public boolean isLoaded() {
        return loaded;
    }
    public void setLoaded() {
        loaded = true;
    }

    public boolean canUse(ItemStack keycard) {
        if (keycard.getItemMeta() == null || !keycard.getItemMeta().getPersistentDataContainer().has(Utils.scannerIdKey,PersistentDataType.STRING)) return false;
        PersistentDataContainer data = keycard.getItemMeta().getPersistentDataContainer();
        byte cardType = data.get(Utils.isKeycardKey,PersistentDataType.BYTE);
        String str = data.get(Utils.scannerIdKey,PersistentDataType.STRING);
        switch (cardType) {
            case 1 -> {return name.equals(str);}
            case 2 -> {return List.of(str.split("\\|\\|")).contains(name);}
            default -> {return false;}
        }
    }

    public ScannerMode switchMode() {
        mode = ScannerMode.switchMode(mode);
        KeyCardPlugin.get().scannersFile.set(name+".mode",mode+"");

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
        metaFiller.setDisplayName(Utils.colors("&r"));
        filler.setItemMeta(metaFiller);
        inv.setItem(2,filler);
        inv.setItem(3,filler);

        ItemStack linker = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta metaLinker = linker.getItemMeta();
        metaLinker.setDisplayName(Utils.colors("&fLink Keycard"));
        metaLinker.setLore(List.of("",Utils.colors("&7Insert your keycard in"),Utils.colors("&7the next slot to link it")));
        linker.setItemMeta(metaLinker);
        inv.setItem(0,linker);

        ItemStack mode = new ItemStack(this.mode.getMat());
        ItemMeta metaMode = mode.getItemMeta();
        metaMode.setDisplayName(Utils.colors("&fSwitch Mode"));
        metaMode.setLore(List.of("",Utils.colors("&7Mode: ")+this.mode.getDesc()));
        mode.setItemMeta(metaMode);
        inv.setItem(4,mode);

        p.openInventory(inv);
    }
}
