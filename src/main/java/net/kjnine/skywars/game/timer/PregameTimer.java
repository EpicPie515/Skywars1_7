package net.kjnine.skywars.game.timer;

import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

import net.kjnine.skywars.Messages;
import net.kjnine.skywars.SkywarsPlugin;
import net.kjnine.skywars.game.GameState;
import net.kjnine.skywars.game.SkywarsGame;

public class PregameTimer extends BukkitRunnable {

	private SkywarsGame game;
	private SkywarsPlugin plugin;
	
	public PregameTimer(SkywarsGame game) {
		this.game = game;
		this.plugin = SkywarsPlugin.getInstance();
	}
	
	// Pregame timer uses tick timer so lag will halt it, rather than using millis time and skipping around in lag.
	public long millisecsRemaining = 60000;
	
	@Override
	public void run() {
		if(game.getPlayers().size() < (game.getMaxPlayers() / 2)) {
			millisecsRemaining = 60000;
			game.gameState = GameState.WAITING_FOR_PLAYERS;
			cancel();

			game.getPlayers().stream().map(u -> plugin.getServer().getPlayer(u)).forEach(p -> {
				game.getScoreboardController().updateSecTimer(p);
			});
			game.getSpectators().stream().map(u -> plugin.getServer().getPlayer(u)).forEach(p -> {
				game.getScoreboardController().updateSecTimer(p);
			});
			return;
		} 
		int tq = (int) Math.floor((game.getMaxPlayers()) * 0.75f);
		if(game.getPlayers().size() >= (tq) && millisecsRemaining > 10000)
			millisecsRemaining = 10000;
		
		if(millisecsRemaining <= 0) {
			game.messaging().announceWithSound(Sound.ENDERDRAGON_GROWL, 1f, 1f, Messages.GAME_STARTED);
			millisecsRemaining = 60000;
			cancel();
			game.startFullGame();
			return;
		}
		
		if(millisecsRemaining % 10000 == 0) game.messaging().announceWithSound(
				Sound.WOOD_CLICK, 1f, 1f, Messages.GAME_STARTING, (millisecsRemaining / 1000));
		else if(millisecsRemaining <= 5000 && millisecsRemaining % 1000 == 0) game.messaging().announceWithSound(
				Sound.WOOD_CLICK, 1f, 1f, Messages.GAME_STARTING, (millisecsRemaining / 1000));
		
		game.getPlayers().stream().map(u -> plugin.getServer().getPlayer(u)).forEach(p -> {
			game.getScoreboardController().updateSecTimer(p);
		});
		game.getSpectators().stream().map(u -> plugin.getServer().getPlayer(u)).forEach(p -> {
			game.getScoreboardController().updateSecTimer(p);
		});
		
		millisecsRemaining -= 100;
	}
	
}
