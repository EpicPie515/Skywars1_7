package net.kjnine.skywars.serverlisteners;

import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import net.haoshoku.nick.NickPlugin;
import net.haoshoku.nick.api.NickAPI;
import net.kjnine.skywars.GameManager;
import net.kjnine.skywars.Messages;
import net.kjnine.skywars.SkywarsPlugin;
import net.kjnine.skywars.game.Messaging;
import net.kjnine.skywars.game.SkywarsGame;
import redis.clients.jedis.Jedis;

/**
 * GameServer player listener, for placing players into games on join.
 *
 */
public class PlayerListener implements Listener {
	
	private SkywarsPlugin plugin;
	private GameManager gm;
	
	public PlayerListener(SkywarsPlugin plugin, GameManager gm) {
		this.gm = gm;
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onLogin(PlayerLoginEvent e) {
		if(gm.findGameJoinable() == null && !gm.isStaffMode(e.getPlayer())) {
			e.setResult(Result.KICK_FULL);
			e.setKickMessage(Messaging.colorcode(Messages.KICK_NOGAMES));
		}
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		e.setJoinMessage(null);
		Player p = e.getPlayer();
		plugin.getServer().getOnlinePlayers().forEach(op -> {
			if(!p.getUniqueId().equals(op.getUniqueId())) {
				op.hidePlayer(p);
				p.hidePlayer(op);
			}
		});
		p.getInventory().clear();
		p.getInventory().setArmorContents(new ItemStack[4]);
		p.updateInventory();
		plugin.getServer().getScheduler()
				.runTaskAsynchronously(plugin, () -> {
			try (Jedis j = plugin.getRedisPool().getResource()) {
				j.auth(plugin.getConfig().getString("redis.pass"));
				String prefix = j.hget("players#" + p.getUniqueId().toString(), "rankprefix");
				String color = j.hget("players#" + p.getUniqueId().toString(), "rankcolor");
				String nick = j.hget("players#" + p.getUniqueId().toString(), "nickname");
				String kbunlocked = j.hget("players#" + p.getUniqueId().toString(), "kbunlocked");
				String kbdefault = j.hget("players#" + p.getUniqueId().toString(), "kbdefault");
				if(nick != null) {
					NickAPI api = NickPlugin.getPlugin().getAPI();
					api.nick(p, nick);
					api.setSkin(p, nick);
					api.setGameProfileName(p, nick);
					api.refreshPlayer(p);
					p.setPlayerListName(nick);
					p.setDisplayName(nick);
					p.setMetadata("nickname", new FixedMetadataValue(plugin, nick));
				}
				if(prefix != null)
					p.setMetadata("rankprefix", new FixedMetadataValue(plugin, Messaging.colorcode(prefix) + " "));
				else
					p.setMetadata("rankprefix", new FixedMetadataValue(plugin, ""));
				if(color != null)
					p.setMetadata("rankcolor", new FixedMetadataValue(plugin, ChatColor.valueOf(color).toString()));
				else
					p.setMetadata("rankcolor", new FixedMetadataValue(plugin, ChatColor.GRAY.toString()));
				if(kbunlocked != null) {
					p.setMetadata("killboosts_unlocked", new FixedMetadataValue(plugin, kbunlocked));
				}
				if(kbdefault != null) {
					p.setMetadata("killboost_default", new FixedMetadataValue(plugin, kbdefault));
				}
			}
			if(gm.isStaffMode(p)) {
				plugin.getServer().getScheduler().runTask(plugin, () -> {
					if(gm.getGames().size() == 0) {
						p.kickPlayer(Messaging.colorcode(Messages.KICK_NOGAMES));
						return;
					}
					SkywarsGame spec = gm.getGames().stream().filter(g -> !g.gameState.isPostgame()).findFirst().get();
					gm.addPlayerToSpectate(p, spec.getGameNumber());
					spec.getPlayers().stream()
					.filter(u -> !p.getUniqueId().equals(u))
					.map(u -> plugin.getServer().getPlayer(u))
					.forEach(op -> p.showPlayer(op));
					// TODO staff mode items
				});
				return;
			}
			
			SkywarsGame game = gm.findGameJoinable();
			plugin.getServer().getScheduler().runTask(plugin, () -> {
				if(game == null) {
					p.kickPlayer(Messaging.colorcode(Messages.KICK_NOGAMES));
					return;
				}
				// TODO debug
				String sn = plugin.getConfig().getString("redis.server-name");
				if(sn == null) sn = plugin.getServer().getServerName();
				game.messaging().send(p, "&aSent to SkyWars &8[%s-%s]&a on Map: &e%s", sn.substring(sn.indexOf('_')+1), game.getGameNumber(), game.getMapName());
				gm.addPlayerToGame(p, game.getGameNumber());
				game.getPlayers().stream()
				.filter(u -> !p.getUniqueId().equals(u))
				.map(u -> plugin.getServer().getPlayer(u))
				.forEach(op -> {
					op.showPlayer(p);
					p.showPlayer(op);
				});
				game.messaging().announce(Messages.PLAYER_JOIN, p.getMetadata("rankcolor").get(0).asString() + p.getDisplayName());
			});
		});
	}
	
	@EventHandler
	public void onChat(AsyncPlayerChatEvent e) {
		Player p = e.getPlayer();
		String pref = "";
		if(p.hasMetadata("rankcolor"))
			pref += p.getMetadata("rankcolor").get(0).asString();
		if(p.hasMetadata("rankprefix"))
			pref += p.getMetadata("rankprefix").get(0).asString();
		pref = Messaging.colorcode(pref);
		if(p.hasPermission("chat.color")) e.setMessage(Messaging.colorcode(e.getMessage()));
		SkywarsGame game = gm.getGameForPlayer(p);
		if(game != null) {
			e.setFormat((game.isAlive(p) ? "" : ChatColor.GRAY + "[DEAD] ") + ChatColor.RESET + pref + "%s:" + ChatColor.WHITE + " %s");
			e.getRecipients().clear();
			if(game.isAlive(p) || gm.isStaffMode(p))
				e.getRecipients().addAll(game.getPlayers().stream().map(u -> plugin.getServer().getPlayer(u)).collect(Collectors.toSet()));
			e.getRecipients().addAll(game.getSpectators().stream().map(u -> plugin.getServer().getPlayer(u)).collect(Collectors.toSet()));
		} else 
			e.setFormat(ChatColor.RESET + pref + " %1$s: %2$s");
	}
	
	@EventHandler
	public void onSpawn(EntitySpawnEvent e) {
		if(e.getEntity() instanceof Slime) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		System.out.println("Somehow " + e.getEntity().getName() + " died, force respawning in 1 tick.");
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> e.getEntity().spigot().respawn(), 1L);
	}
	
	
	
}
