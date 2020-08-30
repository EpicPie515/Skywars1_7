package net.kjnine.skywars.game;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import net.kjnine.skywars.SkywarsPlugin;
import net.md_5.bungee.api.ChatColor;

public class Messaging {
	
	private SkywarsGame game;
	private Server server;
	
	public Messaging(SkywarsPlugin plugin, SkywarsGame game) {
		this.game = game;
		this.server = plugin.getServer();
	}
	
	/**
	 * Sends message to all players+spectators in game.
	 * Supports color codes and {@link String#format(String, Object...)} notation/arguments.
	 */
	public void announce(String messageFormat, Object... args) {
		List<String> messages = Arrays.asList(colorcode(String.format(messageFormat, args)).split("\n"));
		game.getPlayers().forEach(u -> messages.forEach(m -> server.getPlayer(u).sendMessage(m)));
		game.getSpectators().forEach(u -> messages.forEach(m -> server.getPlayer(u).sendMessage(m)));
		messages.forEach(m -> {
			server.getConsoleSender().sendMessage(colorcode("&3[Game-" + game.getGameNumber() + "(" + game.getMapName() + ")] &r") + m);
		});
	}
	
	public void sound(Sound sound, float vol, float pitch) {
		game.getPlayers().stream().map(u -> server.getPlayer(u)).forEach(p -> p.playSound(p.getLocation(), sound, vol, pitch));
		game.getSpectators().stream().map(u -> server.getPlayer(u)).forEach(p -> p.playSound(p.getLocation(), sound, vol, pitch));
	}
	
	public void announceWithSound(Sound sound, float vol, float pitch, String messageFormat, Object... args) {
		sound(sound, vol, pitch);
		announce(messageFormat, args);
	}
	
	public void soundalive(Sound sound, float vol, float pitch) {
		game.getPlayers().stream().map(u -> server.getPlayer(u)).forEach(p -> p.playSound(p.getLocation(), sound, vol, pitch));
	}
	
	public void announceAlive(String messageFormat, Object... args) {
		game.getPlayers().forEach(u -> server.getPlayer(u).sendMessage(colorcode(String.format(messageFormat, args))));
		server.getConsoleSender().sendMessage(colorcode("&3[Game-" + game.getGameNumber() + "(" + game.getMapName() + ")] &r" + String.format(messageFormat, args)));
	}
	
	public void announceSpec(String messageFormat, Object... args) {
		game.getSpectators().forEach(u -> server.getPlayer(u).sendMessage(colorcode(String.format(messageFormat, args))));
		server.getConsoleSender().sendMessage(colorcode("&3[Game-" + game.getGameNumber() + "(" + game.getMapName() + ")-&6SPEC&3] &r" + String.format(messageFormat, args)));
	}

	public void send(Player p, String messageFormat, Object... args) {
		p.sendMessage(colorcode(String.format(messageFormat, args)));
	}
	
	public void sendConsole(String messageFormat, Object... args) {
		server.getConsoleSender().sendMessage(colorcode(String.format(messageFormat, args)));
	}
	
	public String format(String messageFormat, Object... args) {
		return colorcode(String.format(messageFormat, args));
	}
	
	
	public static String colorcode(String message) {
		return ChatColor.translateAlternateColorCodes('&', message);
	}
	
}
