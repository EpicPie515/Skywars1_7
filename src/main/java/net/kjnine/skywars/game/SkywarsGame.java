package net.kjnine.skywars.game;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.inventivetalent.itembuilder.ItemBuilder;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;

import net.kjnine.skywars.GameManager;
import net.kjnine.skywars.Messages;
import net.kjnine.skywars.SkywarsPlugin;
import net.kjnine.skywars.game.cages.EntityCage;
import net.kjnine.skywars.game.listeners.GameListener;
import net.kjnine.skywars.game.listeners.PreGameListener;
import net.kjnine.skywars.game.loot.LootTable;
import net.kjnine.skywars.game.loot.SkyIsland;
import net.kjnine.skywars.game.timer.GameTimer;
import net.kjnine.skywars.game.timer.PregameTimer;
import net.kjnine.skywars.scoreboard.ScoreboardController;
import net.minecraft.util.org.apache.commons.io.FileUtils;

public class SkywarsGame {
	
	private SkywarsPlugin plugin;
	
	private int gameNum;
	private String mapName;
	private final int maxPlayers;
	private World world;
	private LootTable lootTable;
	
	private Messaging msg;
	
	private Set<SkyIsland> islands;
	private Set<Block> centerChests = new HashSet<>();
	private Set<Block> centerBrewingStands = new HashSet<>();

	public Map<UUID, Integer> kills = new HashMap<>();
	public Map<UUID, Long> boostTimes = new HashMap<>();
	
	private Set<UUID> players = new HashSet<>();
	private Set<UUID> spectators = new HashSet<>();
	private Map<UUID, SkyIsland> spawnIsland = new HashMap<>();
	private Map<UUID, EntityCage> cages = new HashMap<>();
	
	public GameState gameState = GameState.GENERATING;
	
	public int nextPhase = 0;
	public String winnerName;
	
	private PreGameListener pgl;
	private GameListener gl;
	
	private GameManager gm;
	
	private ScoreboardController sbCon;
	
	public PregameTimer pregameTimer;
	public GameTimer gameTimer;
	
	public SkywarsGame(GameManager gm, int gameNum, String mapName, int maxplayers, World world, Set<SkyIsland> islands, Set<Block> centerChests, Set<Block> centerBrewingStands, LootTable lootTable) {
		this.plugin = SkywarsPlugin.getInstance();
		this.gm = gm;
		this.gameNum = gameNum;
		this.mapName = mapName;
		this.maxPlayers = maxplayers;
		this.world = world;
		this.islands = new LinkedHashSet<SkyIsland>(islands);
		this.lootTable = lootTable;
		this.centerChests = centerChests;
		this.centerBrewingStands = centerBrewingStands;
		this.sbCon = new ScoreboardController(plugin, this);
		this.msg = new Messaging(plugin, this);
	}
	
	public void removeFromGame(Player p) {
		players.remove(p.getUniqueId());
		boostTimes.remove(p.getUniqueId());
		players.stream().map(u -> plugin.getServer().getPlayer(u)).forEach(tp -> {
			tp.hidePlayer(p);
			sbCon.updatePlayersLeft(tp);
		});
		spectators.stream().map(u -> plugin.getServer().getPlayer(u)).forEach(tp -> sbCon.updatePlayersLeft(tp));
		if(gameState.isIngame()) {
			for(ItemStack i : p.getInventory().getContents()) {
				if(i != null && i.getType() != Material.AIR)
					world.dropItemNaturally(p.getLocation().add(0, 1, 0), i);
			}
			for(ItemStack i : p.getInventory().getArmorContents()) {
				if(i != null && i.getType() != Material.AIR)
					world.dropItemNaturally(p.getLocation().add(0, 1, 0), i);
			}
			p.getInventory().setArmorContents(new ItemStack[4]);
			p.getInventory().clear();
			p.removeMetadata("killboost", SkywarsPlugin.getInstance());
			p.updateInventory();
			p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
			p.setFoodLevel(40);
			p.setFireTicks(0);
			p.setTotalExperience(0);
			p.setMaxHealth(20);
			p.setHealth(p.getMaxHealth());
		} 
		if(gameState.isPregame())
			if(cages.containsKey(p.getUniqueId())) cages.get(p.getUniqueId()).dead = true;
		spawnIsland.remove(p.getUniqueId());
		p.teleport(new Location(world, 0.5, 100, 0.5, 0, -60));
		sbCon.updateScoreboard(p);
	}
	
	public void addToGame(Player p) {
		if(players.size() >= maxPlayers) throw new IllegalStateException("Game is full");
		p.setGameMode(GameMode.SURVIVAL);
		p.getInventory().setArmorContents(new ItemStack[4]);
		p.getInventory().clear();
		p.getInventory().addItem(new ItemBuilder(Material.GOLD_SWORD).buildMeta().withDisplayName(Messaging.colorcode("&6&lKillboost &7Select")).item().build());
		p.updateInventory();
		p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
		p.setAllowFlight(false);
		p.setFoodLevel(40);
		p.setFireTicks(0);
		p.setLevel(0);
		p.setExp(0);
		p.setTotalExperience(0);
		p.setFlying(false);
		p.setMaxHealth(20);
		p.setHealth(p.getMaxHealth());
		players.add(p.getUniqueId());
		sbCon.updateScoreboard(p);
		players.stream().map(u -> plugin.getServer().getPlayer(u)).forEach(tp -> sbCon.updatePlayersLeft(tp));
		spectators.stream().map(u -> plugin.getServer().getPlayer(u)).forEach(tp -> {
			p.hidePlayer(tp);
			sbCon.updatePlayersLeft(tp);
		});
		Optional<SkyIsland> optis = islands.stream().filter(is -> !spawnIsland.containsValue(is)).findFirst();
		if(!optis.isPresent()) throw new IllegalStateException("Game is full");
		SkyIsland island = optis.get();
		p.teleport(island.getSpawnPoint().add(0, 1.5, 0));
		if(p.getVehicle() != null) p.getVehicle().remove();
		spawnIsland.put(p.getUniqueId(), island);
		EntityCage cage = new EntityCage(p);
		cage.enableCage(island.getSpawnPoint().add(0, 1, 0));
		cages.put(p.getUniqueId(), cage);
		
		p.removeMetadata("killboost", plugin);
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> selectDefaultKillboost(p), 1L);
	}
	
	private PacketListener cagePacketHandler;
	
	public void enableCages() {
		ProtocolLibrary.getProtocolManager().addPacketListener(cagePacketHandler = new PacketListener() {
			
			@Override
			public void onPacketReceiving(PacketEvent packet) {
				if(gameState.isPregame()) {
					if(packet.getPacketType().equals(PacketType.Play.Client.STEER_VEHICLE)) {
						packet.setCancelled(true);
					}
				}
			}
			
			@Override
			public ListeningWhitelist getSendingWhitelist() {
				return null;
			}
			
			@Override
			public ListeningWhitelist getReceivingWhitelist() {
				return ListeningWhitelist.newBuilder().types(PacketType.Play.Client.STEER_VEHICLE).build();
			}
			
			@Override
			public Plugin getPlugin() {
				return plugin;
			}

			@Override
			public void onPacketSending(PacketEvent arg0) { }
		});
	}
	
	public void releaseCages() {
		ProtocolLibrary.getProtocolManager().removePacketListener(cagePacketHandler);
		cages.forEach((u, c) -> c.clearCage());
	}
	
	/**
	 * Game manager calls this, so future implementations can scatter all players and start without the timer ticking 60-then 10 (such as map voting before adding to game / going into cages)
	 */
	public void startPregame() {
		pregameTimer = new PregameTimer(this);
		pregameTimer.runTaskTimer(plugin, 2L, 2L);
		gameState = GameState.STARTING;
		players.stream().map(u -> plugin.getServer().getPlayer(u)).forEach(p -> sbCon.updateScoreboard(p));
		spectators.stream().map(u -> plugin.getServer().getPlayer(u)).forEach(p -> sbCon.updateScoreboard(p));
	}

	public void startFullGame() {
		HandlerList.unregisterAll(pgl);
		gl = new GameListener(gm, this);
		gameTimer = new GameTimer(plugin, this);
		plugin.getServer().getPluginManager().registerEvents(gl, plugin);
		gameState = GameState.IN_GAME;
		nextPhase = 1;
		players.stream().map(u -> plugin.getServer().getPlayer(u)).forEach(p -> {
			p.setGameMode(GameMode.SURVIVAL);
			p.getInventory().setArmorContents(new ItemStack[4]);
			p.getInventory().clear();
			p.updateInventory();
			p.closeInventory();
			p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
			p.setAllowFlight(false);
			p.setGameMode(GameMode.SURVIVAL);
			p.setFoodLevel(40);
			p.setFireTicks(0);
			p.setLevel(0);
			p.setExp(0);
			p.setTotalExperience(0);
			p.setFlying(false);
			p.setMaxHealth(20);
			p.setHealth(p.getMaxHealth());
			sbCon.updateScoreboard(p);
		});
		spectators.stream().map(u -> plugin.getServer().getPlayer(u)).forEach(p -> sbCon.updateScoreboard(p));
		gameTimer.runTaskTimer(plugin, 2L, 2L);
		releaseCages();
	}
	
	public Killboost getSelectedKillboost(Player p) {
		if(p.hasMetadata("killboost"))
			return Killboost.valueOf(p.getMetadata("killboost").get(0).asString());
		else
			return Killboost.RESISTANCE;
	}
	
	public void selectDefaultKillboost(Player p) {
		Killboost def;
		if(p.hasMetadata("killboost_default"))
			def = Killboost.valueOf(p.getMetadata("killboost_default").get(0).asString());
		else
			def = Killboost.RESISTANCE;

		p.setMetadata("killboost", new FixedMetadataValue(SkywarsPlugin.getInstance(), def.toString()));
		p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1f, 1f);
		messaging().send(p, Messages.KILLBOOST_SELECT, def.getFormattedName());
		pgl.updateKillboostMenu(p.getUniqueId());
	}
	
	public void applyKillboost(Player p) {
		if(p == null) return;
		
		Killboost kb = getSelectedKillboost(p);
		
		kb.apply(p);
		boostTimes.put(p.getUniqueId(), 7500L);
	}
	
	public long execNextPhase() {
		if(nextPhase == 1) {
			messaging().sound(Sound.CHEST_OPEN, 0.5f, 1f);
			messaging().announceWithSound(Sound.CHEST_CLOSE, 0.5f, 1f, Messages.CHEST_REFILLED);
			fillChestLoot(nextPhase++);
			return 180000;
		} else if(nextPhase == 2) {
			messaging().sound(Sound.CHEST_OPEN, 0.5f, 1f);
			messaging().announceWithSound(Sound.CHEST_CLOSE, 0.5f, 1f, Messages.CHEST_REFILLED);
			fillChestLoot(nextPhase++);
			return 120000;
		} else if(nextPhase == 3) {
			messaging().announceWithSound(Sound.ENDERDRAGON_GROWL, 1f, 1f, Messages.DRAGON_SPAWN, "&cEndgame Dragon");
			EnderDragon dragon = world.spawn(world.getSpawnLocation().subtract(0, 20, 0), EnderDragon.class);
			dragon.setCustomName(Messaging.colorcode("&cEndgame Dragon"));
			dragon.setMaxHealth(1000);
			dragon.setHealth(1000);
			nextPhase++;
			return 300000;
		} else if(nextPhase == 4) {
			draw();
			return 15000;
		} else if(nextPhase == 9 || nextPhase == 10) {
			kickAll();
			nextPhase = 11;
			return 10000;
		} else if(nextPhase == 11) {
			deleteGame();
		}
		return 15000;
	}
	
	public void draw() {
		winnerName = "";
		nextPhase = 9;
		gameState = GameState.GAME_OVER;
		
		players.stream().map(u -> plugin.getServer().getPlayer(u)).forEach(tp -> {
			sbCon.updateScoreboard(tp);
			tp.playSound(tp.getLocation(), Sound.WITHER_SPAWN, 0.4f, 2f);
		});
		spectators.stream().map(u -> plugin.getServer().getPlayer(u)).forEach(tp -> {
			sbCon.updateScoreboard(tp);
			tp.playSound(tp.getLocation(), Sound.WITHER_SPAWN, 0.4f, 2f);
		});
		
		List<UUID> top3u = kills.keySet()
				.stream()
				.sorted((u1, u2) -> Integer.compare(kills.getOrDefault(u1, 0), kills.getOrDefault(u2, 0)))
				.limit(3)
				.collect(Collectors.toList());
		
		List<String> top3s = top3u
				.stream()
				.map(u -> plugin.getServer().getPlayer(u))
				.map(pl -> {
					String prefix = "";
					if(pl.hasMetadata("rankcolor")) prefix += pl.getMetadata("rankcolor").get(0).asString();
					if(pl.hasMetadata("rankprefix")) prefix += pl.getMetadata("rankprefix").get(0).asString();
					return prefix + pl.getDisplayName();
				}).collect(Collectors.toList());
		int[] top3 = top3u
				.stream()
				.mapToInt(u -> kills.getOrDefault(u, 0)).toArray();
		
		String[] topKillers = new String[] {"", "", ""};
		if(top3s.size() == 0) topKillers[0] = messaging().format(Messages.WIN_TOPKILLER, 1, "None", 0);
		Iterator<String> it = top3s.iterator();
		int ind = 0;
		while(it.hasNext()) {
			String name = it.next();
			String addedLen = "     ";
			if(name.length() > 8) addedLen = "";
			topKillers[ind] = addedLen + messaging().format(Messages.WIN_TOPKILLER, ind+1, name, top3[ind]);
			ind++;
		}
		
		messaging().announce(Messages.DRAW, topKillers[0], topKillers[1], topKillers[2]);
		
	}
	
	public void win(Player p) {
		gm.getStats().addWin(p);

		winnerName = p.getDisplayName();
		nextPhase = 10;
		gameState = GameState.GAME_OVER;
		gameTimer.milRem = 15000;
		
		players.stream().map(u -> plugin.getServer().getPlayer(u)).forEach(tp -> {
			sbCon.updateScoreboard(tp);
			tp.playSound(tp.getLocation(), Sound.ENDERDRAGON_DEATH, 0.2f, 2f);
		});
		spectators.stream().map(u -> plugin.getServer().getPlayer(u)).forEach(tp -> {
			sbCon.updateScoreboard(tp);
			tp.playSound(tp.getLocation(), Sound.ENDERDRAGON_DEATH, 0.2f, 2f);
		});
		
		
		String pref = "";
		if(p.hasMetadata("rankcolor")) pref += p.getMetadata("rankcolor").get(0).asString();
		if(p.hasMetadata("rankprefix")) pref += p.getMetadata("rankprefix").get(0).asString();
		
		List<UUID> top3u = kills.keySet()
				.stream()
				.sorted((u1, u2) -> Integer.compare(kills.get(u1), kills.get(u2)))
				.limit(3)
				.collect(Collectors.toList());
		
		List<String> top3s = top3u
				.stream()
				.map(u -> plugin.getServer().getPlayer(u))
				.map(pl -> {
					String prefix = "";
					if(pl.hasMetadata("rankcolor")) prefix += pl.getMetadata("rankcolor").get(0).asString();
					if(pl.hasMetadata("rankprefix")) prefix += pl.getMetadata("rankprefix").get(0).asString();
					return prefix + pl.getDisplayName();
				}).collect(Collectors.toList());
		int[] top3 = top3u
				.stream()
				.mapToInt(u -> kills.get(u)).toArray();
		
		String[] topKillers = new String[] {"", "", ""};
		if(top3s.size() == 0) topKillers[0] = messaging().format(Messages.WIN_TOPKILLER, 1, pref + p.getDisplayName(), kills.getOrDefault(p.getUniqueId(), 0));
		Iterator<String> it = top3s.iterator();
		int ind = 0;
		while(it.hasNext()) {
			String name = it.next();
			String addedLen = "                ";
			if(name.length() > 8) addedLen = "            ";
			if(name.length() > 16) addedLen = "        ";
			topKillers[ind] = addedLen + messaging().format(Messages.WIN_TOPKILLER, ind+1, name, top3[ind]);
			ind++;
		}
		
		int len = (int) Math.ceil((pref + p.getDisplayName()).length() / 2.0);
		int spaceLen = Math.max(1, 24 - len);
		StringBuilder nameLine = new StringBuilder();
		for(int i = 0; i < spaceLen; i++) {
            nameLine.append(' ');
		}
		nameLine.append(pref + p.getDisplayName());
		
		messaging().announce(Messages.WIN, nameLine.toString(), topKillers[0], topKillers[1], topKillers[2]);
		
	}
	
	public void kickAll() {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(bout);
		try {
			dout.writeUTF("Connect");
			dout.writeUTF("SWLobby");
		} catch (IOException e) {
			e.printStackTrace();
		}
		world.getPlayers().forEach(p -> {
			messaging().send(p, Messages.KICKBUNGEE_GAMEEND);
			p.sendPluginMessage(plugin, "BungeeCord", bout.toByteArray());
		});
	}
	
	public void deleteGame() {
		if(!gameState.isPostgame()) throw new IllegalStateException("Cannot delete game before post-game");
		world.getPlayers().forEach(p -> p.kickPlayer(Messaging.colorcode(Messages.KICK_GAMEEND)));
		gameTimer.cancel();
		HandlerList.unregisterAll(gl);
		
		String worldName = world.getName();
		plugin.getServer().unloadWorld(world, false);
		world = null;
		gm.games.remove(this);
		File worldFolder = plugin.getServer().getWorldContainer();
		File worldFile = new File(worldFolder, worldName);
		plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
			if(worldFile.exists() && worldFile.isDirectory()) {
				try {
					FileUtils.forceDelete(worldFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				System.err.println("World to delete (" + worldName + ") does not exist or isnt a directory.");
			}
		}, 200L);
	}
	
	public void fillChestLoot(int phase) {
		Random rand = new Random();
		for(SkyIsland island : islands) {
			Chest[] chests = new Chest[island.getChests().length];
			BrewingStand[] brewingStands = new BrewingStand[island.getBrewingStands().length];
			for(int i = 0; i < chests.length; i++) {
				chests[i] = (Chest) island.getChests()[i].getState();
			}
			for(int i = 0; i < brewingStands.length; i++) {
				brewingStands[i] = (BrewingStand) island.getBrewingStands()[i].getState();
			}
			lootTable.fillIsland(rand, phase, chests, brewingStands);
		}
		Iterator<Block> cit = centerChests.iterator();
		Iterator<Block> bit = centerBrewingStands.iterator();
		while(cit.hasNext()) {
			lootTable.fillCenterChest(rand, phase, (Chest) cit.next().getState());
		}
		while(bit.hasNext()) {
			lootTable.fillCenterBrewer(rand, phase, (BrewingStand) bit.next().getState());
		}
	}

	public boolean isAlive(Player p) {
		return players.contains(p.getUniqueId());
	}
	
	public boolean isSpectator(Player p) {
		return spectators.contains(p.getUniqueId());
	}
	
	public void addSpectator(Player p) {
		spectators.add(p.getUniqueId());
		sbCon.updateScoreboard(p);
		players.stream().map(u -> plugin.getServer().getPlayer(u)).forEach(tp -> {
			p.showPlayer(tp);
			tp.hidePlayer(p);
		});
		spectators.stream().map(u -> plugin.getServer().getPlayer(u)).forEach(tp -> {
			p.showPlayer(tp);
			tp.showPlayer(p);
		});
	}
	
	public void removeSpectator(Player p) {
		spectators.remove(p.getUniqueId());
	}

	public Set<UUID> getPlayers() {
		return players;
	}

	public Set<UUID> getSpectators() {
		return spectators;
	}
	
	public Messaging messaging() {
		return msg;
	}

	public ScoreboardController getScoreboardController() {
		return sbCon;
	}

	public int getGameNumber() {
		return gameNum;
	}

	public int getMaxPlayers() {
		return maxPlayers;
	}

	public String getMapName() {
		return mapName;
	}
	
	public World getGameWorld() {
		return world;
	}
	
	public void setPreGameListener(PreGameListener pgl) {
		this.pgl = pgl;
	}
	
}
