package net.kjnine.skywars.scoreboard;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import net.kjnine.skywars.Messages;
import net.kjnine.skywars.SkywarsPlugin;
import net.kjnine.skywars.game.GameState;
import net.kjnine.skywars.game.SkywarsGame;

public class ScoreboardController {
	
	private SkywarsPlugin plugin;
	private ScoreboardManager sbm;
	private SkywarsGame game;
	
	private Map<Player, Scoreboard> sbs;
	
	public ScoreboardController(SkywarsPlugin plugin, SkywarsGame game) {
		this.plugin = plugin;
		this.sbm = plugin.getServer().getScoreboardManager();
		this.game = game;
		this.sbs = new HashMap<>();
	}
	
	public void updateMainTimer(Player p) {
		if(p == null) return;
		Scoreboard sb = sbs.get(p);
		if(sb == null) sb = updateScoreboard(p);
		Team mainTimerTeam = sb.getTeam("maintimer");
		if(mainTimerTeam == null) mainTimerTeam = sb.registerNewTeam("maintimer");
		setMainTimer(mainTimerTeam);
		
		p.setScoreboard(sb);
	}
	
	public void updateSecTimer(Player p) {
		if(p == null) return;
		Scoreboard sb = sbs.get(p);
		if(sb == null) sb = updateScoreboard(p);
		Team secTimerTeam = sb.getTeam("sectimer");
		if(secTimerTeam == null) secTimerTeam = sb.registerNewTeam("sectimer");
		setSecTimer(secTimerTeam, p);
		
		p.setScoreboard(sb);
	}
	
	public void updatePlayersLeft(Player p) {
		if(p == null) return;
		Scoreboard sb = sbs.get(p);
		if(sb == null) sb = updateScoreboard(p);
		
		// Teams for name coloring
		Team enemy = sb.getTeam("enemy");
		if(enemy == null) enemy = sb.registerNewTeam("enemy");
		enemy.setPrefix(ChatColor.RED.toString());
		Team self = sb.getTeam("aself");
		if(self == null) self = sb.registerNewTeam("aself");
		self.setPrefix(ChatColor.GREEN.toString());
		Team spec = sb.getTeam("yspec");
		if(spec == null) spec = sb.registerNewTeam("yspec");
		spec.setPrefix(ChatColor.GRAY.toString());
		Team staffspec = sb.getTeam("zstaffspec");
		if(staffspec == null) staffspec = sb.registerNewTeam("zstaffspec");
		staffspec.setPrefix(ChatColor.GRAY.toString());
		staffspec.setSuffix(ChatColor.GOLD + "*");
		
		for(UUID u : game.getPlayers()) {
			Player tp = plugin.getServer().getPlayer(u);
			if(!tp.getUniqueId().equals(p.getUniqueId()))
				enemy.addEntry(ChatColor.stripColor(tp.getDisplayName()));
		}
		for(UUID u : game.getSpectators()) {
			Player tp = plugin.getServer().getPlayer(u);
			if(p.hasPermission("skywars.staff") && plugin.getGameManager().isStaffMode(tp))
				staffspec.addEntry(ChatColor.stripColor(tp.getDisplayName()));
			else if(!tp.getUniqueId().equals(p.getUniqueId()))
				spec.addEntry(ChatColor.stripColor(tp.getDisplayName()));
		}
		
		if(game.isAlive(p))
			self.addEntry(ChatColor.stripColor(p.getDisplayName()));
		else spec.addEntry(ChatColor.stripColor(p.getDisplayName()));
		
		Team playersTeam = sb.getTeam("players");
		if(playersTeam == null) playersTeam = sb.registerNewTeam("players");
		playersTeam.setSuffix(c(Messages.SB_PLAYERS_SUFFIX, game.getPlayers().size()));
		
		Team killsTeam = sb.getTeam("kills");
		if(killsTeam == null) killsTeam = sb.registerNewTeam("kills");
		killsTeam.setSuffix(c(Messages.SB_KILLS_SUFFIX, game.kills.getOrDefault(p.getUniqueId(), 0)));
		
		p.setScoreboard(sb);
	}
	
	public Scoreboard updateScoreboard(Player p) {
		if(p == null) return null;
		if(!game.isAlive(p) && !game.isSpectator(p)) return null;
		Scoreboard sb = sbs.get(p);
		if(sb == null) sb = sbm.getNewScoreboard();
		
		// Teams for name coloring
		Team enemy = sb.getTeam("enemy");
		if(enemy == null) enemy = sb.registerNewTeam("enemy");
		enemy.setPrefix(ChatColor.RED.toString());
		Team self = sb.getTeam("aself");
		if(self == null) self = sb.registerNewTeam("aself");
		self.setPrefix(ChatColor.GREEN.toString());
		Team spec = sb.getTeam("yspec");
		if(spec == null) spec = sb.registerNewTeam("yspec");
		spec.setPrefix(ChatColor.GRAY.toString());
		Team staffspec = sb.getTeam("zstaffspec");
		if(staffspec == null) staffspec = sb.registerNewTeam("zstaffspec");
		staffspec.setPrefix(ChatColor.GRAY.toString());
		staffspec.setSuffix(ChatColor.GOLD + "*");
		
		for(UUID u : game.getPlayers()) {
			Player tp = plugin.getServer().getPlayer(u);
			if(!tp.getUniqueId().equals(p.getUniqueId()))
				enemy.addEntry(ChatColor.stripColor(tp.getDisplayName()));
		}
		for(UUID u : game.getSpectators()) {
			Player tp = plugin.getServer().getPlayer(u);
			if(p.hasPermission("skywars.staff") && plugin.getGameManager().isStaffMode(tp))
				staffspec.addEntry(ChatColor.stripColor(tp.getDisplayName()));
			else if(!tp.getUniqueId().equals(p.getUniqueId()))
				spec.addEntry(ChatColor.stripColor(tp.getDisplayName()));
		}
		
		if(game.isAlive(p))
			self.addEntry(ChatColor.stripColor(p.getDisplayName()));
		else spec.addEntry(ChatColor.stripColor(p.getDisplayName()));
		
		// Side-bar
		Objective sidebar = sb.getObjective("Skywars");
		if(sidebar == null) sidebar = sb.registerNewObjective("Skywars", "dummy");
		
		sidebar.setDisplayName(c(Messages.SB_HEADER));
		sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);

		GameState s = game.gameState;
		
		sidebar.getScore(c(Messages.SB_LINE, "&r")).setScore(15);
		sidebar.getScore(c("&r&r")).setScore(14);
		
		if(s.isIngame() || s.isPostgame()) sidebar.getScore(c(Messages.SB_MAINTIMER)).setScore(13);
		sidebar.getScore(c(Messages.SB_SECTIMER)).setScore(12);
		sidebar.getScore(c("&r&r&r")).setScore(11);
		sidebar.getScore(c(Messages.SB_PLAYERS)).setScore(10);
		if(s.isIngame() || s.isPostgame()) sidebar.getScore(c("&r&r&r&r")).setScore(9);
		if(s.isIngame() || s.isPostgame()) sidebar.getScore(c(Messages.SB_KILLS)).setScore(8);
		
		sidebar.getScore(c("&r&r&r&r&r")).setScore(7);
		sidebar.getScore(c("&r&r&r&r&r&r")).setScore(6);
		
		sidebar.getScore(c(Messages.SB_MAP)).setScore(5);
		Date date = new Date();
		sidebar.getScore(c(Messages.SB_NUMLINE)).setScore(4);

		sidebar.getScore(c(Messages.SB_LINE, "&r&r")).setScore(3);
		sidebar.getScore(c(Messages.SB_IP)).setScore(2);
		
		// Teams for side-bar
		Team lineTeam = sb.getTeam("line");
		if(lineTeam == null) lineTeam = sb.registerNewTeam("line");
		lineTeam.setPrefix(c(Messages.SB_LINEPREFIX));
		lineTeam.setSuffix(c(Messages.SB_LINEPREFIX));
		lineTeam.addEntry(c(Messages.SB_LINE, "&r"));
		lineTeam.addEntry(c(Messages.SB_LINE, "&r&r"));

		Team ipTeam = sb.getTeam("ip");
		if(ipTeam == null) ipTeam = sb.registerNewTeam("ip");
		ipTeam.setPrefix(c(Messages.SB_IP_PREFIX));
		ipTeam.addEntry(c(Messages.SB_IP));
		
		Team mainTimerTeam = sb.getTeam("maintimer");
		if(mainTimerTeam == null) mainTimerTeam = sb.registerNewTeam("maintimer");
		mainTimerTeam.addEntry(c(Messages.SB_MAINTIMER));
		setMainTimer(mainTimerTeam);
		
		Team secTimerTeam = sb.getTeam("sectimer");
		if(secTimerTeam == null) secTimerTeam = sb.registerNewTeam("sectimer");
		secTimerTeam.addEntry(c(Messages.SB_SECTIMER));
		setSecTimer(secTimerTeam, p);
		
		Team playersTeam = sb.getTeam("players");
		if(playersTeam == null) playersTeam = sb.registerNewTeam("players");
		playersTeam.setSuffix(c(Messages.SB_PLAYERS_SUFFIX, game.getPlayers().size()));
		playersTeam.addEntry(c(Messages.SB_PLAYERS));
		
		Team killsTeam = sb.getTeam("kills");
		if(killsTeam == null) killsTeam = sb.registerNewTeam("kills");
		killsTeam.setSuffix(c(Messages.SB_KILLS_SUFFIX, game.kills.getOrDefault(p.getUniqueId(), 0)));
		killsTeam.addEntry(c(Messages.SB_KILLS));
		
		Team mapTeam = sb.getTeam("map");
		if(mapTeam == null) mapTeam = sb.registerNewTeam("map");
		String mapname = game.getMapName();
		mapname = mapname.substring(0, 1).toUpperCase() + mapname.substring(1);
		mapTeam.setSuffix(mapname);
		mapTeam.addEntry(c(Messages.SB_MAP));

		Team timeTeam = sb.getTeam("time");
		if(timeTeam == null) timeTeam = sb.registerNewTeam("time");
		String sn = plugin.getConfig().getString("redis.server-name");
		if(sn == null) sn = plugin.getServer().getServerName();
		timeTeam.setSuffix(c(Messages.SB_GAMENUM, sn.substring(sn.indexOf('_')+1), game.getGameNumber()));
		timeTeam.setPrefix(c(Messages.SB_TIMESTAMP, date));
		timeTeam.addEntry(c(Messages.SB_NUMLINE));
		
		sbs.put(p, sb);
		p.setScoreboard(sb);
		return sb;
	}

	private void setMainTimer(Team mainTimerTeam) {
		if(game.gameState.isPregame()) {
			mainTimerTeam.setPrefix("");
			mainTimerTeam.setSuffix("");
			return;
		}
		long secs = (game.gameTimer.milRem - (game.gameTimer.milRem % 1000)) / 1000L;
		switch(game.nextPhase) {
		case 0:
			mainTimerTeam.setPrefix("");
			mainTimerTeam.setSuffix("");
			break;
		case 1:
			mainTimerTeam.setPrefix(c(Messages.SB_TIMERREFILL));
			mainTimerTeam.setSuffix(c(Messages.SB_MAINTIMER_SUFFIX, m(secs), s(secs)));
			break;
		case 2:
			mainTimerTeam.setPrefix(c(Messages.SB_TIMERREFILL2));
			mainTimerTeam.setSuffix(c(Messages.SB_MAINTIMER_SUFFIX, m(secs), s(secs)));
			break;
		case 3:
			mainTimerTeam.setPrefix(c(Messages.SB_TIMERDRAGON));
			mainTimerTeam.setSuffix(c(Messages.SB_MAINTIMER_SUFFIX, m(secs), s(secs)));
			break;
		case 4:
			mainTimerTeam.setPrefix(c(Messages.SB_TIMERGAMEEND));
			mainTimerTeam.setSuffix(c(Messages.SB_MAINTIMER_SUFFIX, m(secs), s(secs)));
			break;
		case 9:
			mainTimerTeam.setPrefix(c(Messages.SB_TIMERDRAW));
			mainTimerTeam.setSuffix("");
			break;
		case 10:
			mainTimerTeam.setPrefix(c(Messages.SB_TIMERWIN));
			mainTimerTeam.setSuffix("");
			break;
		default:
			mainTimerTeam.setPrefix("");
			mainTimerTeam.setSuffix("");
		}
	}

	private void setSecTimer(Team secTimerTeam, Player p) {
		if(p == null) return;
		if(game.gameState.isPregame()) {
			if(game.gameState == GameState.STARTING) {
				secTimerTeam.setPrefix(c(Messages.SB_SECTIMERSTART));
				DecimalFormat df;
				double t = game.pregameTimer.millisecsRemaining / 1000.0;
				if(t < 0) t = 0;
				if(t >= 10.0) df = new DecimalFormat("00");
				else df = new DecimalFormat("0.0");
				
				secTimerTeam.setSuffix(c(Messages.SB_SECTIMER_SUFFIX, df.format(t)));
			} else {
				secTimerTeam.setPrefix(c(Messages.SB_SECTIMERWAITING));
				secTimerTeam.setSuffix(c(Messages.SB_SECTIMERWAITING_SUFFIX));
			}
			return;
		}
		if(game.nextPhase >= 9) {
			secTimerTeam.setPrefix("");
			if(game.nextPhase == 10) secTimerTeam.setSuffix(game.winnerName);
			else secTimerTeam.setSuffix("");
			return;
		}
		if(game.boostTimes.containsKey(p.getUniqueId()) && game.boostTimes.get(p.getUniqueId()) > 0) {
			secTimerTeam.setPrefix(c(Messages.SB_SECTIMERKILLBOOST));
			DecimalFormat df;
			double t = game.boostTimes.get(p.getUniqueId()) / 1000.0;
			if(t < 0) t = 0;
			if(t >= 10.0) df = new DecimalFormat("00");
			else df = new DecimalFormat("0.0");
			
			secTimerTeam.setSuffix(c(Messages.SB_SECTIMER_SUFFIX, df.format(t)));
			return;
		}
		secTimerTeam.setPrefix("");
		secTimerTeam.setSuffix("");
	}

	private long m(long secs) {
		return (secs - (secs % 60L)) / 60L;
	}
	
	private long s(long secs) {
		return (secs % 60L);
	}

	private String c(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
	}
	
	private String c(String s, Object... objects) {
		return ChatColor.translateAlternateColorCodes('&', String.format(s, objects));
	}
	
}
