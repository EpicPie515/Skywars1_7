package net.kjnine.skywars;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import com.boydti.fawe.object.schematic.Schematic;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.ClipboardFormats;

import net.kjnine.skywars.game.GameState;
import net.kjnine.skywars.game.SkywarsGame;
import net.kjnine.skywars.game.listeners.PreGameListener;
import net.kjnine.skywars.game.loot.BasicLootTable;
import net.kjnine.skywars.game.loot.SkyIsland;
import net.kjnine.skywars.world.EmptyWorldGenerator;

public class GameManager {
	
	private SkywarsPlugin plugin;
	
	public Set<SkywarsGame> games = new HashSet<>();
	private PlayerStats stats;
	public int nextGameNumber;
	
	public GameManager(SkywarsPlugin plugin, int initGameNum) {
		this.plugin = plugin;
		this.nextGameNumber = initGameNum;
		
		plugin.getServer().getPluginManager().registerEvents(new PingListener(), plugin);
		this.stats = new PlayerStats(plugin, this);
	}
	
	public SkywarsGame generateNewGame(String mapName) throws FileNotFoundException {
		int gamenum = nextGameNumber++;
		File mapsFolder = new File(plugin.getDataFolder(), plugin.getConfig().getString("maps-folder"));
		if(!mapsFolder.exists()) mapsFolder.mkdirs();
		File map = new File(mapsFolder, mapName + ".schematic");
		if(!map.exists()) throw new FileNotFoundException("Map schematic file not found: " + mapName);
		File mapConf = new File(mapsFolder, mapName + ".json");
		if(!mapConf.exists()) throw new FileNotFoundException("Map config file not found: " + mapName);
		Schematic schem;
		try {
			schem = ClipboardFormats.findByFile(map).load(map);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		World world = plugin.getServer().createWorld(
				WorldCreator.name("sw"+gamenum)
				.environment(Environment.NORMAL)
				.type(WorldType.FLAT)
				.seed(1)
				.generateStructures(false)
				.generator(new EmptyWorldGenerator()));
		world.setDifficulty(Difficulty.HARD);
		for(int x = -6; x < 6; x++) {
			for(int z = -6; z < 6; z++) {
				world.loadChunk(x, z);
			}
		}
		schem.paste(new BukkitWorld(world), new Vector(0, 50, 0), false, false, null);
		
		Set<SkyIsland> islands = new HashSet<>();
		Set<Block> centChests = new HashSet<>();
		Set<Block> centBrewing = new HashSet<>();
		JsonParser parser = new JsonParser();
		try {
			JsonElement je = parser.parse(new FileReader(mapConf));
			JsonObject mainObj = je.getAsJsonObject();
			JsonArray islandArray = mainObj.get("islands").getAsJsonArray();
			JsonObject center = mainObj.get("center").getAsJsonObject();
			JsonArray centerChests = center.get("chests").getAsJsonArray();
			JsonArray centerBrewing = center.get("brewingStands").getAsJsonArray();
			Location ref = world.getBlockAt(0, 50, 0).getLocation();
			for(JsonElement islandEl : islandArray) {
				JsonObject island = islandEl.getAsJsonObject();
				JsonArray chests = island.get("chests").getAsJsonArray();
				JsonArray brewingStands = island.get("brewingStands").getAsJsonArray();
				JsonObject spawnPoint = island.get("spawnPoint").getAsJsonObject();
				Location spawn = ref.clone().add( 
						spawnPoint.get("x").getAsInt(), 
						spawnPoint.get("y").getAsInt(), 
						spawnPoint.get("z").getAsInt())
						.add(0.5, 0.5, 0.5);
				Block[] chestArr = new Block[chests.size()];
				for(int i = 0; i < chests.size(); i++) {
					JsonObject chest = chests.get(i).getAsJsonObject();
					Location loc = ref.clone().add(
							chest.get("x").getAsInt(), 
							chest.get("y").getAsInt(), 
							chest.get("z").getAsInt());
					chestArr[i] = world.getBlockAt(loc);
				}
				Block[] brewingArr = new Block[brewingStands.size()];
				for(int i = 0; i < brewingStands.size(); i++) {
					JsonObject brewingStand = brewingStands.get(i).getAsJsonObject();
					Location loc = ref.clone().add(
							brewingStand.get("x").getAsInt(), 
							brewingStand.get("y").getAsInt(), 
							brewingStand.get("z").getAsInt());
					brewingArr[i] = world.getBlockAt(loc);
				}
				islands.add(new SkyIsland(chestArr, brewingArr, spawn));
			}
			for(int i = 0; i < centerChests.size(); i++) {
				JsonObject chest = centerChests.get(i).getAsJsonObject();
				Location loc = ref.clone().add(
						chest.get("x").getAsInt(), 
						chest.get("y").getAsInt(), 
						chest.get("z").getAsInt());
				centChests.add(world.getBlockAt(loc));
			}
			for(int i = 0; i < centerBrewing.size(); i++) {
				JsonObject brewingStand = centerBrewing.get(i).getAsJsonObject();
				Location loc = ref.clone().add(
						brewingStand.get("x").getAsInt(), 
						brewingStand.get("y").getAsInt(), 
						brewingStand.get("z").getAsInt());
				centBrewing.add(world.getBlockAt(loc));
			}
		} catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
			e.printStackTrace();
		}
		
		SkywarsGame game = new SkywarsGame(this, gamenum, mapName, islands.size(), world, islands, centChests, centBrewing, new BasicLootTable());
		games.add(game);
		game.fillChestLoot(0);
		game.gameState = GameState.WAITING_FOR_PLAYERS;
		game.enableCages();
		registerPreGameListener(game);
		return game;
	}
	
	public void addPlayerToGame(Player p, int gameNum) {
		SkywarsGame game = games.stream()
				.filter(g -> (g.getGameNumber() == gameNum))
				.findFirst().get();
		
		game.addToGame(p);
		if(game.getPlayers().size() >= (game.getMaxPlayers() / 2) && game.pregameTimer == null)
			game.startPregame();
	}
	
	public void addPlayerToSpectate(Player p, int gameNum) {
		SkywarsGame game = games.stream()
				.filter(g -> (g.getGameNumber() == gameNum))
				.findFirst().get();
		
		game.addSpectator(p);
	}
	
	/**
	 * Finds a game that is in pregame with available slots.
	 * @return a joinable {@link SkywarsGame} or <code>null</code> if none available.
	 */
	public SkywarsGame findGameJoinable() {
		Optional<SkywarsGame> og = games.stream()
				.filter(game -> game.gameState.isPregame())
				.filter(game -> game.getPlayers().size() < game.getMaxPlayers())
				.sequential()
				.sorted((g2, g1) -> Integer.compare(g1.getPlayers().size(), g2.getPlayers().size()))
				.findFirst();
		if(!og.isPresent()) return null;
		return og.get();
	}
	
	/**
	 * Gets the game that the player is either playing or spectating.
	 */
	public SkywarsGame getGameForPlayer(Player p ) {
		Optional<SkywarsGame> opt = games.stream().filter(g -> g.isAlive(p) || g.isSpectator(p)).findFirst();
		if(opt.isPresent()) return opt.get();
		else return null;
	}
	
	public SkywarsGame getGameForWorld(World w) {
		Optional<SkywarsGame> ga = games.stream().filter(g -> g.getGameWorld().getName().equals(w.getName())).findFirst();
		if(ga.isPresent()) return ga.get();
		return null;
	}
	
	public void registerPreGameListener(SkywarsGame game) {
		PreGameListener pgl = new PreGameListener(this, game);
		plugin.getServer().getPluginManager().registerEvents(pgl, plugin);
		game.setPreGameListener(pgl);
	}
	
	public boolean isStaffMode(Player p) {
		// TODO impl staffmode
		return false;
	}
	
	/**
	 * Checks if a player is in interactive mode (with perms) 
	 * interactive mode: (able to block place/break, change gameplay, cheat commands, etc.)
	 * @return
	 */
	public boolean isInteractiveMode(Player p) {
		// TODO impl staffmode
		return false;
	}
	
	public Set<SkywarsGame> getGames() {
		return games;
	}
	
	public PlayerStats getStats() {
		return stats;
	}
	
	private class PingListener implements Listener {
		
		@EventHandler
		public void onPing(ServerListPingEvent e) {
			String[] pcounts = new String[games.size()];
			int i = 0;
			String format = "%d:%d:%d";
			// Gamenum, Players ingame, Slots left(0 if ingame)
			for(SkywarsGame g : games) {
				pcounts[i++] = String.format(format, g.getGameNumber(), g.getPlayers().size(), g.gameState.isPregame() ? (g.getMaxPlayers() - g.getPlayers().size()) : 0);
			}
			e.setMotd(String.join(";", pcounts));
		}
		
	}
	
}
