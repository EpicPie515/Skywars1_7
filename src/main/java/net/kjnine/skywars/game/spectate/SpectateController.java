package net.kjnine.skywars.game.spectate;

import org.bukkit.entity.Player;

import net.kjnine.skywars.GameManager;
import net.kjnine.skywars.game.SkywarsGame;

public class SpectateController {

	private SkywarsGame game;
	private GameManager gm;
	
	public SpectateController(SkywarsGame game, GameManager gm) {
		super();
		this.game = game;
		this.gm = gm;
	}

	/**
	 * @throws IllegalStateException if player is not added as spectator.
	 */
	public void enable(Player p) {
		if(!game.isSpectator(p)) throw new IllegalStateException("Player must be marked as spectator before enabling Spectate Mode.");
		
	}
	
}
