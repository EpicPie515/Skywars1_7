package net.kjnine.skywars;

public class Messages {

	public static final String PLAYER_LEAVE = "&7%s&e left.";
	public static final String PLAYER_JOIN = "&7%s&e joined.";
	public static final String GAME_STARTING = "&eStarting in &c%d&e seconds...";
	public static final String GAME_STARTED = "&eGame has begun, Good Luck!";

	
	public static final String WIN = "&7&m                                                \n\n"
			+ "&r                    &2&lWinner\n"
			+ "&r%s\n\n" // Remember to add dynamic center-spacing in code
			+ "&r                  &2&lTop Kills\n"
			+ "%s"
			+ "%s"
			+ "%s\n"
			+ "&7&m                                                ";
	
	public static final String WIN_TOPKILLER = "&r&a%d. &r%s&f - &a%d\n";
	
	public static final String DRAW = "&7&m                                                \n\n"
			+ "&r                     &d&lDraw\n"
			+ "&r                  &fNo Winner\n\n"
			+ "&r                  &2&lTop Kills\n"
			+ "%s"
			+ "%s"
			+ "%s\n"
			+ "&7&m                                                ";
	
	public static final String DEATH_OTHER = "&7%s&e died.";
	public static final String DEATH_FALL = "&7%s&e fell to their death.";
	public static final String DEATH_EXPLODE = "&7%s&e blew up.";
	public static final String DEATH_VOID = "&7%s&e fell into the void.";
	public static final String DEATH_FIRE = "&7%s&e burned to death.";
	public static final String DEATH_MAGIC = "&7%s&e was killed by magic.";
	public static final String DEATH_SMITE = "&7%s&e was struck by lightning.";
	public static final String DEATH_STARVE = "&7%s&e starved to death.";
	public static final String DEATH_THORNS = "&7%s&e was pricked to death.";
	public static final String DEATH_BLOCK = "&7%s&e was squashed to death.";

	public static final String KILL_OTHER = "&7%s&e was killed by &7%s&e.";
	public static final String KILL_SHOT = "&7%s&e was shot by &7%s&e.";
	public static final String KILL_VOID = "&7%s&e was thrown into the void by &7%s&e.";
	public static final String KILL_FALL = "&7%s&e was knocked off a cliff by &7%s&e.";
	public static final String KILL_FIRE = "&7%s&e burned to death while fighting &7%s&e.";
	public static final String KILL_EXPLODE = "&7%s&e was blown up by &7%s&e.";

	public static final String KICK_NOGAMES = "&cCouldn't connect to game, please try again momentarily.";
	public static final String KICK_GAMEEND = "&cGame has ended.";
	public static final String KICKBUNGEE_GAMEEND = "&cGame has ended, connecting you to the lobby...";

	public static final String CHEST_REFILLED = "&eChests have been refilled!";
	public static final String DRAGON_SPAWN = "&e%s &ahas spawned.";
	public static final String GAME_END_WARNING = "&cGame will end in &b%d&c %s.";

	public static final String KILLBOOST_SELECT = "&eSelected Killboost: &7%s";
	
	public static final String TIME_MINUTE = "minute";
	public static final String TIME_MINUTES = "minutes";
	public static final String TIME_SECONDS = "seconds";
	public static final String TIME_SECOND = "second";

	public static final String SB_HEADER = "&2&lSKYWARS";
	public static final String SB_LINEPREFIX = "&7&m            ";
	public static final String SB_LINE = "&7&m    %s";
	public static final String SB_MAINTIMER = "&7&f";
	public static final String SB_SECTIMER = "&7&r&f";
	public static final String SB_SECTIMER_SUFFIX = "&f: %s";
	public static final String SB_SECTIMERWAITING = "&fWaiting for";
	public static final String SB_SECTIMERWAITING_SUFFIX = "&f players...";
	public static final String SB_MAINTIMER_SUFFIX = "&f: %02d:%02d";
	public static final String SB_SECTIMERSTART = "&c&lStarting";
	public static final String SB_TIMERREFILL = "&6&lChest Refill";
	public static final String SB_TIMERREFILL2 = "&3&lFinal Refill";
	public static final String SB_TIMERDRAGON = "&d&lDragon";
	public static final String SB_TIMERGAMEEND = "&4&lGame End";
	public static final String SB_SECTIMERKILLBOOST = "&e&lKillboost";
	public static final String SB_TIMERWIN = "&2&lWinner";
	public static final String SB_TIMERDRAW = "&d&lGame Over";

	public static final String SB_PLAYERS = "&aPlayers: &f";
	public static final String SB_KILLS = "&aKills: &f";
	public static final String SB_PLAYERS_SUFFIX = "%d";
	public static final String SB_KILLS_SUFFIX = "%d";
	public static final String SB_MAP = "&bMap: &f";

	public static final String SB_IP = "&2keisu.us";
	public static final String SB_IP_PREFIX = "      &l   ";
	public static final String SB_TIMESTAMP = "&7%1$tm/%1$td/%1$ty";
	public static final String SB_NUMLINE = "&8&r&7&f";
	public static final String SB_GAMENUM = " &8[%s-%d]";

	public static final String ADMIN_PREFIX = "&2[SWAdmin] &a";
	public static final String ADMIN_NOCHEAT = "%s&cYou must be in Cheat mode to use that.";
	public static final String ADMIN_KILLBOOST = "%sApplied Killboost to &e[%s&e]";
	
}
