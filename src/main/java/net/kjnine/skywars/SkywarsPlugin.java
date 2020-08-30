package net.kjnine.skywars;

import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import net.kjnine.skywars.game.cages.EntityCage;
import net.kjnine.skywars.game.commands.SkyAdminCommand;
import net.kjnine.skywars.serverlisteners.PlayerListener;
import net.kjnine.skywars.world.EmptyWorldGenerator;
import net.minecraft.server.v1_7_R4.EntityHorse;
import net.minecraft.server.v1_7_R4.EntityInsentient;
import net.minecraft.server.v1_7_R4.EntityTypes;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class SkywarsPlugin extends JavaPlugin {

	private static SkywarsPlugin inst;
	
	private JedisPool redisPool;
	
	private GameManager gameManager;
	
	private String[] mapsList = new String[] {"testskywarsmap"};
	
	@Override
	public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
		return new EmptyWorldGenerator();
	}
	
	@Override
	public void onEnable() {
		inst = this;
		saveDefaultConfig();
		reloadConfig();
		
		initRedis();
		initGameManager();
		
		initGameRunnable();
		
		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		getServer().getPluginManager().registerEvents(new PlayerListener(this, gameManager), this);
		
		initCommands();
		
		registerEntity("Horse", 100, EntityHorse.class, EntityCage.class);
		
	}
	
	public void initGameRunnable() {
		getServer().getScheduler().runTaskTimer(this, () -> {
			if(gameManager.getGames().size() < 1) {
				// rough
				try {
					gameManager.generateNewGame(mapsList[0]);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}, 20L, 1200L);
	}
	
	@Override
	public void onDisable() {
		try (Jedis j = redisPool.getResource()) {
			j.auth(getConfig().getString("redis.pass"));
			String sn = getServer().getServerName();
			if(getConfig().isSet("redis.server-name")) sn = getConfig().getString("redis.server-name");
			j.hset("gamenumber", sn, String.valueOf(gameManager.nextGameNumber));
		}
		redisPool.close();
		redisPool.destroy();
	}
	
	private void initRedis() {
		redisPool = new JedisPool(getConfig().getString("redis.host"), getConfig().getInt("redis.port"));
	}
	
	private void initGameManager() {
		try (Jedis j = redisPool.getResource()) {
			j.auth(getConfig().getString("redis.pass"));
			String sn = getServer().getServerName();
			if(getConfig().isSet("redis.server-name")) sn = getConfig().getString("redis.server-name");
			String gamenum = j.hget("gamenumber", sn);
			if(gamenum == null) gamenum = "1";
			gameManager = new GameManager(this, Integer.parseInt(gamenum));
		}
	}
	
	private void initCommands() {
		try {
			Field commandMapField = getServer().getClass().getDeclaredField("commandMap");
			commandMapField.setAccessible(true);
			SimpleCommandMap commandMap = (SimpleCommandMap) ((commandMapField.get(getServer())));
			SkyAdminCommand exec = new SkyAdminCommand(this, gameManager);
			Command cmd = new Command("skyadmin", "Skywars Admin command", "Usage: /skyadmin <command>", new ArrayList<String>()) {
				@Override
				public boolean execute(CommandSender sender, String commandLabel, String[] args) {
					return exec.onCommand(sender, this, commandLabel, args);
				}
			};
			commandMap.register("skyadmin", "skywars", cmd);
			commandMapField.setAccessible(false);
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
	}
	
	public JedisPool getRedisPool() {
		return redisPool;
	}
	
	public static SkywarsPlugin getInstance() {
		return inst;
	}

	public GameManager getGameManager() {
		return gameManager;
	}
	
	public void registerEntity(String name, int id, Class<? extends EntityInsentient> nmsClass, Class<? extends EntityInsentient> customClass){
        try {
     
            List<Map<?, ?>> dataMap = new ArrayList<Map<?, ?>>();
            for (Field f : EntityTypes.class.getDeclaredFields()){
                if (f.getType().getSimpleName().equals(Map.class.getSimpleName())){
                    f.setAccessible(true);
                    dataMap.add((Map<?, ?>) f.get(null));
                }
            }
     
            if (dataMap.get(2).containsKey(id)){
                dataMap.get(0).remove(name);
                dataMap.get(2).remove(id);
            }
     
            Method method = EntityTypes.class.getDeclaredMethod("a", Class.class, String.class, int.class);
            method.setAccessible(true);
            method.invoke(null, customClass, name, id);
     
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
}
