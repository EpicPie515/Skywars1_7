package net.kjnine.skywars.game.listeners;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.inventivetalent.menubuilder.inventory.InventoryMenuBuilder;

import net.kjnine.skywars.GameManager;
import net.kjnine.skywars.Messages;
import net.kjnine.skywars.SkywarsPlugin;
import net.kjnine.skywars.game.Killboost;
import net.kjnine.skywars.game.Messaging;
import net.kjnine.skywars.game.SkywarsGame;

public class PreGameListener implements Listener {

	private GameManager gm;
	private SkywarsGame game;
	
	public PreGameListener(GameManager gm, SkywarsGame game) {
		this.gm = gm;
		this.game = game;
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		if(!game.getPlayers().contains(e.getPlayer().getUniqueId())) return;
		e.setQuitMessage(null);
		game.messaging().announce(game.gameState.isPregame() ? Messages.PLAYER_LEAVE : Messages.DEATH_OTHER, 
				(e.getPlayer().hasMetadata("rankcolor") ? e.getPlayer().getMetadata("rankcolor").get(0).asString() : ChatColor.GRAY) + e.getPlayer().getDisplayName());
		game.removeFromGame(e.getPlayer());
	}
	
	private HashMap<UUID, Inventory> killboostMenus = new HashMap<>();
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		if(!game.getPlayers().contains(e.getPlayer().getUniqueId())) return;
		if(!game.gameState.isPregame()) return;
		if(gm.isInteractiveMode(e.getPlayer())) return;
		UUID u = e.getPlayer().getUniqueId();
		if(e.getItem() != null && e.getItem().getType().equals(Material.GOLD_SWORD)) {
			updateKillboostMenu(u);
			e.getPlayer().openInventory(killboostMenus.get(u));
		}
		
		e.setCancelled(true);
	}
	
	public void updateKillboostMenu(UUID u) {
		if(!killboostMenus.containsKey(u)) {
			Inventory menu = 
					new InventoryMenuBuilder(9)
					.withTitle(Messaging.colorcode("&6&lKillboost&r Selector"))
					.withItem(0, Killboost.RESISTANCE.getItem(game, u))
					.withItem(1, Killboost.REGEN.getItem(game, u))
					.withItem(2, Killboost.ABSORPTION.getItem(game, u))
					.withItem(3, Killboost.STRENGTH.getItem(game, u))
					.build();
			killboostMenus.put(u, menu);
		} else {
			Inventory menu = killboostMenus.get(u);
			menu.setItem(0, Killboost.RESISTANCE.getItem(game, u));
			menu.setItem(1, Killboost.REGEN.getItem(game, u));
			menu.setItem(2, Killboost.ABSORPTION.getItem(game, u));
			menu.setItem(3, Killboost.STRENGTH.getItem(game, u));
			killboostMenus.put(u, menu);
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onVehicleExit(VehicleExitEvent e) {
		if(!(e.getExited() instanceof Player)) return;
		if(!game.getPlayers().contains(e.getExited().getUniqueId())) return;
		if(!game.gameState.isPregame()) return;
		if(gm.isInteractiveMode((Player)e.getExited())) return;
		
		e.setCancelled(true);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent e) {
		if(!game.getPlayers().contains(e.getPlayer().getUniqueId())) return;
		if(!game.gameState.isPregame()) return;
		if(gm.isInteractiveMode(e.getPlayer())) return;
		if(e.getFrom().getBlockX() == e.getTo().getBlockX() && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;
		e.setCancelled(true);
		e.getPlayer().teleport(e.getFrom());
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent e) {
		if(!(e.getEntity() instanceof Player)) return;
		Player p = (Player) e.getEntity();
		if(!game.getPlayers().contains(p.getUniqueId())) return;
		if(!game.gameState.isPregame()) return;
		
		e.setCancelled(true);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
		if(!game.getPlayers().contains(e.getPlayer().getUniqueId())) return;
		if(!game.gameState.isPregame()) return;
		if(gm.isInteractiveMode(e.getPlayer())) return;
		
		e.setCancelled(true);
	}
	
	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent e) {
		if(!game.getPlayers().contains(e.getPlayer().getUniqueId())) return;
		if(!game.gameState.isPregame()) return;
		if(!(e.getPlayer() instanceof Player)) return;
		if(gm.isInteractiveMode((Player)e.getPlayer())) return;
		
		if(ChatColor.stripColor(e.getInventory().getTitle()).equals("Killboost Selector")) {
			e.setCancelled(false);
			return;
		}
		
		e.setCancelled(true);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent e) {
		if(!game.getPlayers().contains(e.getWhoClicked().getUniqueId())) return;
		if(!(e.getWhoClicked() instanceof Player)) return;
		if(e.getInventory().getTitle().equals(Messaging.colorcode("&6&lKillboost&r Selector"))) {
			Player p = (Player) e.getWhoClicked();
			UUID u = p.getUniqueId();
			switch(e.getSlot()) {
			case 0: 
				if(Killboost.hasAccess(u, Killboost.RESISTANCE)) {
					if(!game.getSelectedKillboost(p).equals(Killboost.RESISTANCE)) {
						p.setMetadata("killboost", new FixedMetadataValue(SkywarsPlugin.getInstance(), Killboost.RESISTANCE.toString()));
						p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1f, 1f);
						game.messaging().send(p, Messages.KILLBOOST_SELECT, Killboost.RESISTANCE.getFormattedName());
						updateKillboostMenu(u);
					}
				} else p.playSound(p.getLocation(), Sound.NOTE_BASS, 1f, 0.5f);
				break;
			case 1: 
				if(Killboost.hasAccess(u, Killboost.REGEN)) {
					if(!game.getSelectedKillboost(p).equals(Killboost.REGEN)) {
						p.setMetadata("killboost", new FixedMetadataValue(SkywarsPlugin.getInstance(), Killboost.REGEN.toString()));
						p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1f, 1f);
						game.messaging().send(p, Messages.KILLBOOST_SELECT, Killboost.REGEN.getFormattedName());
						updateKillboostMenu(u);
					}
				} else p.playSound(p.getLocation(), Sound.NOTE_BASS, 1f, 0.5f);
				break;
			case 2:
				if(Killboost.hasAccess(u, Killboost.ABSORPTION)) {
					if(!game.getSelectedKillboost(p).equals(Killboost.ABSORPTION)) {
						p.setMetadata("killboost", new FixedMetadataValue(SkywarsPlugin.getInstance(), Killboost.ABSORPTION.toString()));
						p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1f, 1f);
						game.messaging().send(p, Messages.KILLBOOST_SELECT, Killboost.ABSORPTION.getFormattedName());
						updateKillboostMenu(u);
					}
				} else p.playSound(p.getLocation(), Sound.NOTE_BASS, 1f, 0.5f);
				break;
			case 3:
				if(Killboost.hasAccess(u, Killboost.STRENGTH)) {
					if(!game.getSelectedKillboost(p).equals(Killboost.STRENGTH)) {
						p.setMetadata("killboost", new FixedMetadataValue(SkywarsPlugin.getInstance(), Killboost.STRENGTH.toString()));
						p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1f, 1f);
						game.messaging().send(p, Messages.KILLBOOST_SELECT, Killboost.STRENGTH.getFormattedName());
						updateKillboostMenu(u);
					}
				} else p.playSound(p.getLocation(), Sound.NOTE_BASS, 1f, 0.5f);
				break;
			}
			
			e.setCancelled(true);
		}
		
		if(!game.gameState.isPregame()) return;
		if(gm.isInteractiveMode((Player)e.getWhoClicked())) return;
		
		
		e.setCancelled(true);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onInventoryInteract(InventoryInteractEvent e) {
		if(!game.getPlayers().contains(e.getWhoClicked().getUniqueId())) return;
		if(!game.gameState.isPregame()) return;
		if(!(e.getWhoClicked() instanceof Player)) return;
		if(gm.isInteractiveMode((Player)e.getWhoClicked())) return;
		
		e.setCancelled(true);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onCraftItem(CraftItemEvent e) {
		if(!game.getPlayers().contains(e.getWhoClicked().getUniqueId())) return;
		if(!game.gameState.isPregame()) return;
		if(!(e.getWhoClicked() instanceof Player)) return;
		if(gm.isInteractiveMode((Player)e.getWhoClicked())) return;
		
		e.setCancelled(true);
	}
	
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerItemConsume(PlayerItemConsumeEvent e) {
		if(!game.getPlayers().contains(e.getPlayer().getUniqueId())) return;
		if(!game.gameState.isPregame()) return;
		if(gm.isInteractiveMode(e.getPlayer())) return;
		
		e.setCancelled(true);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerItemDamage(PlayerItemDamageEvent e) {
		if(!game.getPlayers().contains(e.getPlayer().getUniqueId())) return;
		if(!game.gameState.isPregame()) return;
		if(gm.isInteractiveMode(e.getPlayer())) return;
		
		e.setCancelled(true);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerPickupItem(PlayerPickupItemEvent e) {
		if(!game.getPlayers().contains(e.getPlayer().getUniqueId())) return;
		if(!game.gameState.isPregame()) return;
		if(gm.isInteractiveMode(e.getPlayer())) return;
		
		e.setCancelled(true);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerToggleSneak(PlayerToggleSneakEvent e) {
		if(!game.getPlayers().contains(e.getPlayer().getUniqueId())) return;
		if(!game.gameState.isPregame()) return;
		if(gm.isInteractiveMode(e.getPlayer())) return;
		
		e.setCancelled(true);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent e) {
		if(!game.getPlayers().contains(e.getPlayer().getUniqueId())) return;
		if(!game.gameState.isPregame()) return;
		if(gm.isInteractiveMode(e.getPlayer())) return;
		
		e.setCancelled(true);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent e) {
		if(!game.getPlayers().contains(e.getPlayer().getUniqueId())) return;
		if(!game.gameState.isPregame()) return;
		if(gm.isInteractiveMode(e.getPlayer())) return;
		
		e.setCancelled(true);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onBlockDamage(BlockDamageEvent e) {
		if(!game.getPlayers().contains(e.getPlayer().getUniqueId())) return;
		if(!game.gameState.isPregame()) return;
		if(gm.isInteractiveMode(e.getPlayer())) return;
		
		e.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlock(BlockBurnEvent e) {
		if(!e.getBlock().getWorld().getName().equals(game.getGameWorld().getName())) return;
		if(game == null) return;
		if(!game.gameState.isPregame()) return;
		
		e.setCancelled(true);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onBlock(BlockFadeEvent e) {
		if(!e.getBlock().getWorld().getName().equals(game.getGameWorld().getName())) return;
		if(game == null) return;
		if(!game.gameState.isPregame()) return;
		
		e.setCancelled(true);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onBlock(BlockIgniteEvent e) {
		if(!e.getBlock().getWorld().getName().equals(game.getGameWorld().getName())) return;
		if(game == null) return;
		if(!game.gameState.isPregame()) return;
		if(e.getPlayer() != null) {
			if(gm.isInteractiveMode(e.getPlayer())) return;
		}
		
		e.setCancelled(true);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onBlock(BlockPhysicsEvent e) {
		if(!e.getBlock().getWorld().getName().equals(game.getGameWorld().getName())) return;	
		if(game == null) return;
		if(!game.gameState.isPregame()) return;
		
		e.setCancelled(true);
	}
	
	
}
