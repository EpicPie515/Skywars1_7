package net.kjnine.skywars.game.loot;

import java.util.Random;

import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;

public interface LootTable {

	public void fillIsland(Random rand, int phase, Chest[] chests, BrewingStand[] brewingStands);
	
	public void fillCenterChest(Random rand, int phase, Chest chest);
	public void fillCenterBrewer(Random rand, int phase, BrewingStand brewingStand);
	
}
