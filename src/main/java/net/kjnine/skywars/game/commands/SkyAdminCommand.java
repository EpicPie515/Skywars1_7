package net.kjnine.skywars.game.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import net.haoshoku.nick.NickPlugin;
import net.haoshoku.nick.api.NickAPI;
import net.kjnine.skywars.GameManager;
import net.kjnine.skywars.Messages;
import net.kjnine.skywars.SkywarsPlugin;
import net.kjnine.skywars.game.Messaging;
import net.kjnine.skywars.game.SkywarsGame;

public class SkyAdminCommand implements CommandExecutor{

	private GameManager gm;
	private SkywarsPlugin pl;
	
	public SkyAdminCommand(SkywarsPlugin plugin, GameManager gm) {
		this.gm = gm;
		this.pl = plugin;
	}
	
	@SuppressWarnings("unused")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(command.getName().equals("skyadmin")) {
			if(!sender.hasPermission("skywars.admin")) {
				sender.sendMessage(ChatColor.RED + "You do not have permission to perform this command. If you believe this is in error, contact an administrator.");
				return true;
			}
			if(args.length == 0) {
				sender.sendMessage("Use /skyadmin <command> [param]");
				return true;
			}
			String sub = args[0].toLowerCase();
			if(sub.equals("killboost")) {
				if(args.length == 1) {
					if(!(sender instanceof Player)) return true;
					Player p = (Player) sender;
					SkywarsGame game = gm.getGameForPlayer(p);
					if(game == null) { 
						p.sendMessage("You are not in a game.");
						return true;
					} if(!game.isAlive(p)) {
						p.sendMessage("You are not alive.");
						return true;
					} if(/*TODO !gm.isInteractiveMode(p)*/false) {
						game.messaging().send(p, Messages.ADMIN_NOCHEAT, Messages.ADMIN_PREFIX);
						return true;
					}
					game.messaging().send(p, Messages.ADMIN_KILLBOOST, Messages.ADMIN_PREFIX, p.getDisplayName());
					game.applyKillboost(p);
					return true;
				}
				Player t = pl.getServer().getPlayer(args[1]);
				if(t == null) {
					sender.sendMessage("Player not found");
					return true;
				}
				SkywarsGame game = gm.getGameForPlayer(t);
				if(game == null) { 
					sender.sendMessage(t.getDisplayName() + " is not in a game.");
					return true;
				} if(!game.isAlive(t)) {
					sender.sendMessage(t.getDisplayName() + " is not alive.");
					return true;
				} if(sender instanceof Player) {
					Player p = (Player) sender;
					if(/*TODO !gm.isInteractiveMode(p)*/false) {
						game.messaging().send(p, Messages.ADMIN_NOCHEAT, Messages.ADMIN_PREFIX);
						return true;
					}
					game.messaging().send(p, Messages.ADMIN_KILLBOOST, Messages.ADMIN_PREFIX, t.getDisplayName());
					game.applyKillboost(t);
					return true;
				}
				game.messaging().sendConsole(Messages.ADMIN_KILLBOOST, Messages.ADMIN_PREFIX, t.getDisplayName());
				game.applyKillboost(t);
				return true;
			} else if(sub.equals("nick")) {
				if(!(sender instanceof Player)) return true;
				Player p = (Player) sender;
				String prefix = "";
				String color = ChatColor.GRAY.toString();
				String nick = p.getDisplayName();
				if(p.hasMetadata("rankcolor"))
					color = p.getMetadata("rankcolor").get(0).asString();
				if(p.hasMetadata("rankprefix"))
					prefix = p.getMetadata("rankprefix").get(0).asString();
				if(p.hasMetadata("nickname"))
					nick = p.getMetadata("nickname").get(0).asString();
				NickAPI api = NickPlugin.getPlugin().getAPI();
				api.nick(p, nick);
				api.setSkin(p, nick);
				api.setGameProfileName(p, nick);
				api.refreshPlayer(p);
				p.setDisplayName(nick);
				p.setMetadata("rankprefix", new FixedMetadataValue(pl, Messaging.colorcode(prefix) + " "));
				p.setMetadata("rankcolor", new FixedMetadataValue(pl, color));
				p.setMetadata("nickname", new FixedMetadataValue(pl, nick));
			} else if(sub.equals("d")) {
				if(!(sender instanceof Player)) return true;
				Player p = (Player) sender;
				NickAPI api = NickPlugin.getPlugin().getAPI();
				api.nick(p, "GodHimself");
				api.setSkin(p, "GodHimself");
				api.setGameProfileName(p, "GodHimself");
				api.refreshPlayer(p);
			}
			
			return true;
		}
		return false;
	}
	
}
