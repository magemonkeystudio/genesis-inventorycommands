package studio.magemonkey.genesis.addon.inventorycommands;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import studio.magemonkey.genesis.events.GenesisRegisterTypesEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerListener implements Listener, Reloadable {
    private final InventoryCommands plugin;
    private       boolean           drop;
    private       boolean           move;
    private       boolean           place;
    private       boolean           death;
    private       boolean           respawn;
    private       boolean           accept_leftclick;
    private       boolean           clear_inv_on_join;
    private       boolean           clear_inv_on_world_change;
    private       int               join_delay;

    public PlayerListener(InventoryCommands plugin) {
        this.plugin = plugin;
        loadSettings(plugin);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!this.death) {
            List<ItemStack> toRemove = new ArrayList<>();

            for (ItemStack i : event.getDrops()) {
                if (this.plugin.getGSMItems().isShopItem(i)) {
                    toRemove.add(i);
                }

            }

            for (ItemStack i : toRemove) {
                event.getDrops().remove(i);
            }
            toRemove.clear();
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (this.respawn) {
            this.plugin.getGSMItems().giveItems(event.getPlayer(), GSMItems.GSMGiveItemsReason.RESPAWN);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (clear_inv_on_join) {
            event.getPlayer().getInventory().clear();
            event.getPlayer().updateInventory();
        }

        if (join_delay > 0) {
            final UUID playerid = event.getPlayer().getUniqueId();
            new BukkitRunnable() {
                @Override
                public void run() {
                    Player p = Bukkit.getPlayer(playerid);
                    if (p != null) {
                        plugin.getGSMItems().giveItems(p, GSMItems.GSMGiveItemsReason.SERVER_JOIN);
                    }
                }
            }.runTaskLater(plugin, join_delay);
        } else {
            this.plugin.getGSMItems().giveItems(event.getPlayer(), GSMItems.GSMGiveItemsReason.SERVER_JOIN);
        }

    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (clear_inv_on_world_change) {
            event.getPlayer().getInventory().clear();
            event.getPlayer().updateInventory();
        }
        this.plugin.getGSMItems().giveItems(event.getPlayer(), GSMItems.GSMGiveItemsReason.WORLD_CHANGED);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if ((!this.drop) && (this.plugin.getGSMItems().isShopItem(event.getItemDrop().getItemStack())))
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if ((!this.place) && (this.plugin.getGSMItems().isShopItem(event.getItemInHand()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(InventoryClickEvent event) {
        if (this.move) return;

        if (this.plugin.getGSMItems().isShopItem(event.getCurrentItem())
                || this.plugin.getGSMItems().isShopItem(event.getCursor())) {
            event.setCancelled(true);
            event.setResult(Result.DENY);
        }
    }

    @EventHandler
    public void swap(PlayerSwapHandItemsEvent event) {
        if (this.move) return;

        if (this.plugin.getGSMItems().isShopItem(event.getOffHandItem())
                || this.plugin.getGSMItems().isShopItem(event.getMainHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        this.plugin.getGSMItems().playerClicked(event, accept_leftclick);
    }


    @EventHandler
    public void onRegister(GenesisRegisterTypesEvent event) {
        new ItemDataPartGuiShopManager().register();
    }

    public void reload(InventoryCommands plugin) {
        loadSettings(plugin);
    }

    private void loadSettings(InventoryCommands plugin) {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("Settings");

        this.drop = s.getBoolean("AllowDropItems");
        this.move = s.getBoolean("AllowMoveItems");
        this.place = s.getBoolean("AllowPlaceItems");
        this.accept_leftclick = s.getBoolean("AcceptLeftClick");
        this.death = s.getBoolean("DropItemsOnDeath");
        this.respawn = s.getBoolean("GetItemsOnRespawn");
        this.clear_inv_on_join = s.getBoolean("ClearInvOnJoin");
        this.clear_inv_on_world_change = s.getBoolean("ClearInvOnWorldChange");
        this.join_delay = s.getInt("JoinDelay");
    }
}