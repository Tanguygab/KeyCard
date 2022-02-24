package io.github.tanguygab.keycard.scanner;

import io.github.tanguygab.keycard.KeyCardPlugin;
import io.github.tanguygab.keycard.Utils;
import io.github.tanguygab.keycard.events.KeyCardCheckEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Powerable;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.block.CraftBlock;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockRedstoneEvent;
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
    private int ticks;

    public Scanner(String name, UUID frameID, UUID owner) {
        this(name,frameID,owner,ScannerMode.ACTIVE_ON_SWIPE,15);
    }

    public Scanner(String name, UUID frameID, UUID owner, ScannerMode mode, int ticks) {
        this.name = name;
        this.frameID = frameID;
        this.owner = owner;
        this.mode = mode;
        this.ticks = ticks;

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
    public int getTicks() {
        return ticks;
    }
    public void addTicks(int ticks) {
        setTicks(this.ticks+=ticks);
    }
    public void setTicks(int ticks) {
        this.ticks = ticks;
        KeyCardPlugin.get().scannersFile.set(name+".ticks",ticks+"");
    }
    public boolean isLoaded() {
        return loaded;
    }
    public void setLoaded() {
        loaded = true;
    }

    public boolean canUse(ItemStack keycard) {
        if (keycard == null || keycard.getItemMeta() == null) return false;
        PersistentDataContainer data = keycard.getItemMeta().getPersistentDataContainer();
        String cardType = data.get(Utils.keycardTypeKey,PersistentDataType.STRING);
        if (cardType == null) return false;
        String str = data.get(Utils.scannerIdKey,PersistentDataType.STRING);
        switch (cardType) {
            case "normal","remote" -> {return frameID.toString().equals(str);}
            case "multi" -> {return str != null && List.of(str.split("\\|\\|")).contains(frameID.toString());}
            default -> {
                KeyCardCheckEvent event = new KeyCardCheckEvent(keycard,this,cardType);
                Bukkit.getServer().getPluginManager().callEvent(event);
                return event.isCancelled();
            }
        }
    }

    public ScannerMode switchMode() {
        if (mode == ScannerMode.LEVER) KeyCardPlugin.get().scannersFile.set(name+".lever",null);
        mode = ScannerMode.switchMode(mode);
        KeyCardPlugin.get().scannersFile.set(name+".mode",mode+"");

        mode.unload(this);

        return mode;
    }

    public void setStatus(boolean activated) {
        Block block = Bukkit.getServer().getEntity(frameID).getLocation().getBlock();
        Entity entity = Bukkit.getServer().getEntity(frameID);

        if (block.getType() != Material.STONE_BUTTON) Utils.addMap(entity,this);

        Powerable data = (Powerable) block.getBlockData();
        data.setPowered(activated);
        block.setBlockData(data);

        Location loc = entity.getLocation();
        BlockRedstoneEvent eventRedstone = new BlockRedstoneEvent(block, activated ? 15 : 0, activated ? 0 : 15);
        Bukkit.getServer().getPluginManager().callEvent(eventRedstone);

        Directional directional = (Directional) block.getBlockData();
        CraftBlock cblock = (CraftBlock) block;
        ((CraftWorld)loc.getWorld()).getHandle().b(cblock.getPosition().a(CraftBlock.blockFaceToNotch(directional.getFacing().getOppositeFace())),cblock.getNMS().b());
    }

    public void open(Player p) {

        Inventory inv = Bukkit.getServer().createInventory(null, InventoryType.HOPPER, "Scanner menu: "+name);

        ItemStack linker = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta metaLinker = linker.getItemMeta();
        metaLinker.setDisplayName(Utils.colors("&fLink Keycard"));
        metaLinker.setLore(List.of("",Utils.colors("&7Insert your keycard in"),Utils.colors("&7the next slot to link it")));
        linker.setItemMeta(metaLinker);
        inv.setItem(0,linker);

        ItemStack delay = new ItemStack(Material.CLOCK);
        ItemMeta metaDelay = delay.getItemMeta();
        metaDelay.setDisplayName(Utils.colors("&fChange Delay"));
        metaDelay.setLore(List.of("",Utils.colors("&7Ticks: ")+getTicks()));
        delay.setItemMeta(metaDelay);
        inv.setItem(2,delay);

        ItemStack mode = new ItemStack(this.mode.getMat());
        ItemMeta metaMode = mode.getItemMeta();
        metaMode.setDisplayName(Utils.colors("&fSwitch Mode"));
        metaMode.setLore(List.of("",Utils.colors("&7Mode: ")+this.mode.getDesc()));
        mode.setItemMeta(metaMode);
        inv.setItem(3,mode);

        ItemStack delete = new ItemStack(Material.BARRIER);
        ItemMeta metaDelete = delete.getItemMeta();
        metaDelete.setDisplayName(Utils.colors("&4Delete Scanner"));
        metaDelete.setLore(List.of("",Utils.colors("&cDeletes this scanner!")));
        delete.setItemMeta(metaDelete);
        inv.setItem(4,delete);

        p.openInventory(inv);
    }
}
