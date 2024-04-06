package studio.magemonkey.genesis.addon.inventorycommands;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.genesis.managers.ClassManager;
import studio.magemonkey.genesis.managers.misc.InputReader;

import java.util.ArrayList;
import java.util.List;

public class GSMItem {
    @Getter
    private final String       path;
    private       ItemStack    item;
    private final String       open_shop;
    private final List<String> commands;
    private final List<String> playercommands;
    private final int          inventory_location;
    private final boolean      give_on_join;
    private       String[]     worlds;


    public GSMItem(InventoryCommands plugin, ConfigurationSection section) {
        this.path = section.getName();

        open_shop = section.getString("OpenShop");
        commands = InputReader.readStringList(section.get("Command"));
        playercommands = InputReader.readStringList(section.get("PlayerCommand"));
        inventory_location = InputReader.getInt(section.get("InventoryLocation"), 1) - 1;
        give_on_join = InputReader.getBoolean(section.getString("GiveOnJoin"), true);

        List<String> list = InputReader.readStringList(section.get("Look"));
        if (list != null) {
            this.item = plugin.getGenesis().getClassManager().getItemStackCreator().createItemStack(list, false);
        }

        String world = section.getString("World");
        if (world != null) {
            worlds = world.split(":");
        }
    }

    public ItemStack getItemStack() {
        return item;
    }

    public boolean isValid() {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        return item.getItemMeta().hasDisplayName();
    }

    public boolean isCorrespondingItem(ItemStack item) {
        if (!isValid()) {
            return false;
        }

        if (!item.hasItemMeta()) {
            return false;
        }

        if (!item.getItemMeta().hasDisplayName()) {
            return false;
        }

        if (!ClassManager.manager.getItemStackChecker().isEqualShopItemAdvanced(item, this.item, false,
                false, true, null)) {
            return false;
        }


        return item.getItemMeta().getDisplayName().equals(this.item.getItemMeta().getDisplayName());
    }


    public void giveItem(Player p, GSMItems.GSMGiveItemsReason reason) {
        if (reason == GSMItems.GSMGiveItemsReason.COMMAND) {
            giveItem(p);
            return;
        }

        if (reason == GSMItems.GSMGiveItemsReason.WORLD_CHANGED) {
            if ((p.getInventory().contains(item.getType())) && (hasItem(p))) { //Player already has item
                if (!isWorldSupported(p.getWorld())) { //World does not support item
                    removeItem(p);
                    return;
                }
            }
        }


        if (give_on_join) {
            if ((p.getInventory().contains(item.getType())) && (hasItem(p))) { //Player already has item
                return;
            }
            if (!isWorldSupported(p.getWorld())) {
                return;
            }

            giveItem(p);
        }

    }

    @Deprecated
    public void giveItem(Player p) {
        boolean bad_location = false;

        int loc = inventory_location;

        if ((loc >= p.getInventory().getSize()) || (loc < 0)) {
            loc = 0;
            bad_location = true;
        }

        ItemStack real_item = ClassManager.manager.getItemStackTranslator()
                .translateItemStack(null, null, null, item.clone(), p, true);

        if ((!bad_location) || (p.getInventory().getItem(loc) == null)) {
            p.getInventory().setItem(loc, real_item);
        } else {
            p.getInventory().addItem(real_item);
        }
    }

    public void removeItem(Player p) {
        List<ItemStack> to_remove = null;
        for (ItemStack s : p.getInventory().getContents()) {
            if (s != null) {
                if (isSameItem(s, p)) {
                    if (to_remove == null) {
                        to_remove = new ArrayList<ItemStack>();
                    }
                    to_remove.add(s);
                }
            }
        }

        for (ItemStack remove : to_remove) {
            p.getInventory().remove(remove);
        }
    }

    public boolean hasItem(Player p) {
        for (ItemStack s : p.getInventory().getContents()) {
            if (s != null) {
                if (isSameItem(s, p)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isSameItem(ItemStack i, Player p) {
        if (ClassManager.manager.getItemStackChecker().isEqualShopItemAdvanced(item, i, false,
                false, true, p)) {

            String displayname = item.getItemMeta().getDisplayName();

            if (ClassManager.manager.getStringManager().checkStringForFeatures(null, null, null,
                    displayname)) { //contains special variables
                displayname = ClassManager.manager.getStringManager().transform(displayname, p);
            }

            return displayname.equalsIgnoreCase(i.getItemMeta().getDisplayName());

        }
        return false;
    }

    public boolean isWorldSupported(World w) {
        if (worlds == null) {
            return true;
        }
        for (String world : worlds) {
            if (world.equalsIgnoreCase(w.getName())) {
                return true;
            }
        }
        return false;
    }


    public boolean playerClicked(InventoryCommands plugin, PlayerInteractEvent e) {
        if (!isCorrespondingItem(e.getItem())) {
            return false;
        }

        if (open_shop != null) {
            plugin.getGenesis().getAPI().openShop(e.getPlayer(), open_shop);
        }

        if (commands != null) {
            for (String command : commands) {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                        ClassManager.manager.getStringManager().transform(command, e.getPlayer()));
            }
        }

        if (playercommands != null) {
            for (String command : playercommands) {
                e.getPlayer().performCommand(ClassManager.manager.getStringManager().transform(command, e.getPlayer()));
            }
        }

        e.getPlayer().updateInventory();
        return true;
    }

}
