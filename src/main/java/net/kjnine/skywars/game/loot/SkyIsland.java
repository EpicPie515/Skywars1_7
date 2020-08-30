package net.kjnine.skywars.game.loot;

import org.bukkit.Location;
import org.bukkit.block.Block;

public class SkyIsland {
	
	private Block[] chests;
	private Block[] brewingStands;
	private Location spawnPoint;
	
	public SkyIsland(Block[] chests, Block[] brewingStands, Location spawnPoint) {
		super();
		this.chests = chests;
		this.brewingStands = brewingStands;
		this.spawnPoint = spawnPoint;
	}

	public Block[] getChests() {
		return chests;
	}

	public Block[] getBrewingStands() {
		return brewingStands;
	}

	public Location getSpawnPoint() {
		return spawnPoint;
	}
}
