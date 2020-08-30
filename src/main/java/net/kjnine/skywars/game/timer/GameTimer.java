package net.kjnine.skywars.game.timer;

import java.util.Random;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

import net.kjnine.skywars.Messages;
import net.kjnine.skywars.SkywarsPlugin;
import net.kjnine.skywars.game.SkywarsGame;

public class GameTimer extends BukkitRunnable {

	private SkywarsGame game;
	private SkywarsPlugin plugin;
	
	public GameTimer(SkywarsPlugin plugin, SkywarsGame game) {
		this.game = game;
		this.plugin = plugin;
		milRem = 180000;
	}
	
	// game timer uses tick timer so lag will halt it, rather than using millis time and skipping around in lag.
	public long milRem = 180000;
	
	@Override
	public void run() {
		if(milRem <= 0) {
			milRem = game.execNextPhase();
			return;
		}
		
		if(game.nextPhase == 10 && milRem % 1000 == 0) {
	        game.getPlayers()
	        .stream()
	        .map(u -> plugin.getServer().getPlayer(u))
	        .forEach(p -> {
	        	Firework fw = (Firework) game.getGameWorld().spawn(p.getLocation().add(0, 1, 0), Firework.class);
	            FireworkMeta fwm = fw.getFireworkMeta();
	            fwm.setPower(2);
	            fwm.addEffects(randEffect(), randEffect(), randEffect());
	            fw.setFireworkMeta(fwm);
	            plugin.getServer().getScheduler().runTaskLater(plugin, () -> fw.detonate(), 2L);
	        });
		}
		
		
		if(game.nextPhase == 4) {
			if(milRem % 60000 == 0) {
				long min = (milRem - (milRem % 60000)) / 60000;
				if(min > 1) game.messaging().announceWithSound(Sound.NOTE_PIANO, 2f, 0.5f, Messages.GAME_END_WARNING, min, Messages.TIME_MINUTES);
				else game.messaging().announceWithSound(Sound.NOTE_PIANO, 2f, 0.5f, Messages.GAME_END_WARNING, min, Messages.TIME_MINUTE);
			} else if(milRem < 60000 && milRem % 10000 == 0) {
				long secs = (milRem - (milRem % 1000)) / 1000;
				game.messaging().announceWithSound(Sound.NOTE_PIANO, 2f, 0.5f, Messages.GAME_END_WARNING, secs, Messages.TIME_SECONDS);
			} else if(milRem < 10000 && milRem % 1000 == 0) {
				long secs = (milRem - (milRem % 1000)) / 1000;
				if(secs > 1) game.messaging().announceWithSound(Sound.NOTE_PIANO, 2f, 0.8f, Messages.GAME_END_WARNING, secs, Messages.TIME_SECONDS);
				else game.messaging().announceWithSound(Sound.NOTE_PIANO, 2f, 0.8f, Messages.GAME_END_WARNING, secs, Messages.TIME_SECOND);
			}
		}
		
		if(game.gameState.isIngame()) {
			game.getPlayers().forEach(u -> {
				if(game.boostTimes.containsKey(u)) {
					long t = game.boostTimes.get(u)-100;
					if(t > 0) game.boostTimes.put(u, t);
					else game.boostTimes.remove(u);
				}
				Player p = plugin.getServer().getPlayer(u);
				game.getScoreboardController().updateMainTimer(p);
				game.getScoreboardController().updateSecTimer(p);
			});
			game.getSpectators().stream().map(u -> plugin.getServer().getPlayer(u)).forEach(p -> {
				game.getScoreboardController().updateMainTimer(p);
				game.getScoreboardController().updateSecTimer(p);
			});
		}
		milRem -= 100;
	}
	
	public FireworkEffect randEffect() {
		Random rand = new Random();
		
		Color c1 = randColor(rand);
		Color c2 = randColor(rand);
		return FireworkEffect.builder()
				.flicker(rand.nextBoolean())
				.trail(rand.nextBoolean())
				.with(rand.nextBoolean() ? Type.BALL : Type.BURST)
				.withColor(c1)
				.withFade(c2)
				.build();
	}
	
	private Color randColor(Random rand) {
		switch(rand.nextInt(13)+1) {
		case 1: return Color.AQUA;
		case 2: return Color.BLUE;
		case 3: return Color.FUCHSIA;
		case 4: return Color.GREEN;
		case 5: return Color.LIME;
		case 6: return Color.MAROON;
		case 7: return Color.NAVY;
		case 8: return Color.OLIVE;
		case 9: return Color.ORANGE;
		case 10: return Color.PURPLE;
		case 11: return Color.RED;
		case 12: return Color.TEAL;
		case 13: return Color.YELLOW;
		default: return Color.WHITE;
		}
	}
	
}
