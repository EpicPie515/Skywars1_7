package net.kjnine.skywars.game.loot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.inventivetalent.itembuilder.ItemBuilder;
import org.inventivetalent.itembuilder.PotionMetaBuilder;

import net.kjnine.skywars.SkywarsPlugin;
import net.md_5.bungee.api.ChatColor;

public class BasicLootTable implements LootTable {
	
	private SkywarsPlugin plugin;
	
	public BasicLootTable() {
		this.plugin = SkywarsPlugin.getInstance();
	}
	
	@Override
	public void fillIsland(Random rand, int phase, Chest[] chests, BrewingStand[] brewingStands) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			List<ItemStack> items = new ArrayList<>();
			List<ItemStack> potions = new ArrayList<>();
			
			if(phase == 0) {
				items.add(new ItemBuilder().withType(rand.nextBoolean() ? Material.DIAMOND_CHESTPLATE : Material.IRON_CHESTPLATE).build());
				int armorPiece = rand.nextInt(7) + 1;
				if(armorPiece - 4 >= 0) {
					items.add(new ItemBuilder().withType(rand.nextBoolean() ? Material.DIAMOND_BOOTS : Material.IRON_BOOTS).build());
					armorPiece -= 4;
				}
				if(armorPiece - 2 >= 0) {
					items.add(new ItemBuilder().withType(rand.nextBoolean() ? Material.DIAMOND_LEGGINGS : Material.IRON_LEGGINGS).build());
					armorPiece -= 2;
				}
				if(armorPiece > 0) {
					items.add(new ItemBuilder().withType(rand.nextBoolean() ? Material.DIAMOND_HELMET : Material.IRON_HELMET).build());
					armorPiece -= 1;
				}
				
				items.add(new ItemBuilder().withType(rand.nextBoolean() ? Material.IRON_SWORD : Material.DIAMOND_SWORD).build());
				items.add(new ItemBuilder().withType(Material.IRON_AXE).buildMeta().withEnchant(Enchantment.DIG_SPEED, 2, false).item().build());
				items.add(new ItemBuilder().withType(Material.DIAMOND_PICKAXE).build());
				
				int cntBlocks = rand.nextInt(3)+2;
				for(int i = 0; i < cntBlocks; i++)
					items.add(new ItemBuilder().withType(rand.nextBoolean() ? Material.WOOD : Material.STONE).withAmount((rand.nextInt(4)+2)*8).build());
				
				int cntProjs = rand.nextInt(5);
				for(int i = 0; i < cntProjs; i++) {
					if(rand.nextInt(10) > 1) items.add(new ItemBuilder().withType(rand.nextBoolean() ? Material.EGG : Material.SNOW_BALL).withAmount(16).build());
					else items.add(new ItemBuilder().withType(Material.FISHING_ROD).build());
				}
				

				int cntxp = rand.nextInt(3);
				for(int i = 0; i < cntxp; i++)
					items.add(new ItemBuilder().withType(Material.EXP_BOTTLE).withAmount(16).build());
				
				int wat = rand.nextInt(2)+1;
				for(int i = 0; i < wat; i++)
					items.add(new ItemBuilder().withType(Material.WATER_BUCKET).build());
				
				if(rand.nextInt(3) == 0) items.add(new ItemBuilder().withType(Material.ENCHANTMENT_TABLE).build());
				
				if(rand.nextInt(12) == 0) {
					items.add(new ItemBuilder().withType(Material.BOW).buildMeta().withEnchant(Enchantment.ARROW_DAMAGE, 3, false).item().build());
					items.add(new ItemBuilder().withType(Material.ARROW).withAmount((rand.nextInt(3)+2)*8).build());
				}
				if(rand.nextInt(6) == 0) {
					items.add(new ItemBuilder().withType(Material.BOW).buildMeta().withEnchant(Enchantment.ARROW_DAMAGE, 1, false).item().build());
					items.add(new ItemBuilder().withType(Material.ARROW).withAmount((rand.nextInt(3)+1)*8).build());
				}
				
				for(int i = 0; i < 3; i++)
					items.add(new ItemBuilder().withType(Material.COOKED_BEEF).withAmount(12).build());
				
				if(brewingStands.length > 0) {
					for(int i = 0; i < brewingStands.length; i++) {
						fillBrewingStand(rand, potions);
					}
				}
				
			} // TODO refills (phase 1+2)
			
			plugin.getServer().getScheduler().runTask(plugin, () -> {
				LinkedList<Chest> chestSet = new LinkedList<>();
				for(Chest c : chests) if(c.getBlock().getType().equals(Material.CHEST)) chestSet.add(c);
				LinkedList<BrewingStand> brewSet = new LinkedList<>();
				for(BrewingStand b : brewingStands) if(b.getBlock().getType().equals(Material.BREWING_STAND)) brewSet.add(b);
				
				if(chestSet.size() > 0) {
					Collections.shuffle(items);
					Iterator<ItemStack> it = items.iterator();
					int cind = 0;
					while(it.hasNext()) {
						ItemStack item = it.next();
						Inventory inv = chestSet.get(cind++).getBlockInventory();
						if(inv.firstEmpty() == -1) continue;
						int slot;
						do {
							slot = rand.nextInt(inv.getSize());
						} while(!(inv.getItem(slot) == null || inv.getItem(slot).getType().equals(Material.AIR)));
						
						inv.setItem(slot, item);
						
						if(cind >= chestSet.size()) cind = 0;
					}
				}
				if(brewSet.size() > 0) {
					Collections.shuffle(potions);
					Iterator<ItemStack> it = potions.iterator();
					int bind = 0;
					while(it.hasNext()) {
						ItemStack item = it.next();
						BrewerInventory inv = brewSet.get(bind++).getInventory();
						if(inv.firstEmpty() == -1) continue;
						List<Integer> slots = new ArrayList<>(Arrays.asList(0, 1, 2));
						Collections.shuffle(slots);
						for(int slot : slots) {
							if(inv.getItem(slot) == null || inv.getItem(slot).getType().equals(Material.AIR)) {
								inv.setItem(slot, item);
								break;
							}
						}
						
						if(bind >= brewSet.size()) bind = 0;
					}
				}
			});
		});
		
	}
	
	private void fillBrewingStand(Random rand, List<ItemStack> potions)  {
		ItemStack hite = new ItemBuilder().withType(Material.POTION).build();
		Potion hpot = new Potion(PotionType.SPEED, 2);
		hpot.apply(hite);
		potions.add(new ItemBuilder(hite)
				.withType(Material.POTION)
				.buildMeta(PotionMetaBuilder.class)
				.withMainEffect(PotionEffectType.SPEED)
				.withCustomEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 100, 1), true)
				.item().build());
		int p = rand.nextInt(3)+1;
		
		if(p - 2 >= 0) {
			if(rand.nextBoolean()) {
				boolean reg2 = rand.nextInt(3) == 0;
				ItemStack ite = new ItemBuilder().withType(Material.POTION).build();
				Potion pot = new Potion(PotionType.REGEN, reg2 ? 2 : 1);
				pot.apply(ite);
				potions.add(new ItemBuilder(ite)
						.withType(Material.POTION)
						.buildMeta(PotionMetaBuilder.class)
						.withMainEffect(PotionEffectType.REGENERATION)
						.withCustomEffect(new PotionEffect(
								PotionEffectType.REGENERATION, 
								20 * (reg2 ? 15 : 40),
								reg2 ? 1 : 0), true)
						.item().build());
			} else {
				ItemStack ite = new ItemBuilder().withType(Material.POTION).build();
				Potion pot = new Potion(PotionType.FIRE_RESISTANCE, 1);
				pot.apply(ite);
				potions.add(new ItemBuilder(ite)
						.withType(Material.POTION)
						.buildMeta(PotionMetaBuilder.class)
						.withMainEffect(PotionEffectType.FIRE_RESISTANCE)
						.withCustomEffect(new PotionEffect(
								PotionEffectType.FIRE_RESISTANCE, 
								20 * 80,
								0), true)
						.item().build());
			}
		}
		if(p > 0) {
			int debuff = rand.nextInt(5)+1;
			boolean l2 = rand.nextInt(6) == 0;
			if(debuff <= 2) {
				ItemStack ite = new ItemBuilder().withType(Material.POTION).build();
				Potion pot = new Potion(PotionType.POISON, l2 ? 2 : 1).splash();
				pot.apply(ite);
				potions.add(new ItemBuilder(ite)
						.buildMeta(PotionMetaBuilder.class)
						.withMainEffect(PotionEffectType.POISON)
						.withCustomEffect(new PotionEffect(
								PotionEffectType.POISON, 
								20 * (l2 ? 10 : 20),
								l2 ? 1 : 0), true)
						.item().build());
			} else if(debuff == 3) {
				ItemStack ite = new ItemBuilder().withType(Material.POTION).build();
				Potion pot = new Potion(PotionType.SLOWNESS, 1).splash();
				pot.apply(ite);
				potions.add(new ItemBuilder(ite)
						.buildMeta(PotionMetaBuilder.class)
						.withMainEffect(PotionEffectType.SLOW)
						.withCustomEffect(new PotionEffect(
								PotionEffectType.SLOW, 
								20 * (l2 ? 25 : 10),
								0), true)
						.item().build());
			} else if(debuff == 4) {
				ItemStack ite = new ItemBuilder().withType(Material.POTION).build();
				Potion pot = new Potion(PotionType.WEAKNESS, 1).splash();
				pot.apply(ite);
				potions.add(new ItemBuilder(ite)
						.buildMeta(PotionMetaBuilder.class)
						.withMainEffect(PotionEffectType.BLINDNESS)
						.withCustomEffect(new PotionEffect(
								PotionEffectType.BLINDNESS, 
								20 * (l2 ? 12 : 6),
								0), true)
						.item().build());
			} else if(debuff == 5) {
				ItemStack ite = new ItemBuilder().withType(Material.POTION).build();
				Potion pot = new Potion(PotionType.WEAKNESS, l2 ? 2 : 1).splash();
				pot.apply(ite);
				potions.add(new ItemBuilder(ite)
						.buildMeta(PotionMetaBuilder.class)
						.withMainEffect(PotionEffectType.WEAKNESS)
						.withCustomEffect(new PotionEffect(
								PotionEffectType.WEAKNESS, 
								20 * (l2 ? 15 : 25),
								l2 ? 1 : 0), true)
						.item().build());
			}
				
			
		}
	}

	@Override
	public void fillCenterChest(Random rand, int phase, Chest chest) {
		if(!chest.getBlock().getType().equals(Material.CHEST)) return;
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			List<ItemStack> items = new ArrayList<>();
			int icount = rand.nextInt(6)+2;
			for(int i = 0; i < icount; i++) {
				int p = rand.nextInt(19 + ((phase-1)*4))+1;
				switch(p) {
				case 1:
				case 2:
					items.add(new ItemBuilder(Material.SNOW_BALL).withAmount(64).build());
					break;
				case 3:
					items.add(new ItemBuilder(Material.FLINT_AND_STEEL).build());
				case 4:
					items.add(new ItemBuilder(Material.TNT).withAmount(12).build());
					break;
				case 19:
					items.add(new ItemBuilder(Material.EXP_BOTTLE).withAmount(64).build());
					break;
				case 20:
				case 24:
					items.add(new ItemBuilder(Material.GOLDEN_APPLE).withAmount(rand.nextInt(4)+3).build());
				case 5:
					items.add(new ItemBuilder(Material.GOLDEN_APPLE).withAmount(rand.nextInt(4)+3).build());
					break;
				case 6:
					items.add(new ItemBuilder(Material.DIAMOND_PICKAXE).buildMeta().withEnchant(Enchantment.DIG_SPEED, 4, true).item().build());
					break;
				case 7:
					items.add(new ItemBuilder(Material.DIAMOND_AXE).buildMeta().withEnchant(Enchantment.DIG_SPEED, 4, true).item().build());
					break;
				case 8:
					items.add(new ItemBuilder(Material.DIAMOND_AXE).buildMeta().withEnchant(Enchantment.DIG_SPEED, 4, true).item().build());
					break;
				case 9:
					items.add(new ItemBuilder(Material.LOG).withAmount(64).build());
					break;
				case 21:
				case 25:
				case 10:
					items.add(new ItemBuilder(Material.ENDER_PEARL).withAmount(5).build());
					break;
				case 11:
					items.add(new ItemBuilder(Material.DIAMOND_SWORD).buildMeta().withEnchant(Enchantment.FIRE_ASPECT, 1, true).item().build());
					break;
				case 12:
					items.add(new ItemBuilder(Material.BOW).buildMeta().withEnchant(Enchantment.ARROW_DAMAGE, 5, true).item().build());
				case 13:
					items.add(new ItemBuilder(Material.ARROW).withAmount(64).build());
					break;
				case 14:
					items.add(new ItemBuilder(Material.DIAMOND_BOOTS).buildMeta().withEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true).withEnchant(Enchantment.PROTECTION_FALL, 3, true).item().build());
					break;
				case 15:
					items.add(new ItemBuilder(Material.DIAMOND_CHESTPLATE).buildMeta().withEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true).item().build());
					break;
				case 16:
					items.add(new ItemBuilder(rand.nextInt(3) == 0 ? Material.DIAMOND_LEGGINGS : Material.DIAMOND_HELMET).buildMeta().withEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true).item().build());
					break;
				case 17:
					items.add(new ItemBuilder(Material.LAVA_BUCKET).build());
					break;
				case 18:
					items.add(new ItemBuilder(Material.DIAMOND_SWORD).buildMeta().withEnchant(Enchantment.FIRE_ASPECT, 1, true).withEnchant(Enchantment.DAMAGE_ALL, 3, true).item().build());
					break;
				case 27:
				case 22:
					items.add(new ItemBuilder(Material.BOW).buildMeta().withEnchant(Enchantment.ARROW_DAMAGE, 5, true).withEnchant(Enchantment.ARROW_FIRE, 1, true).item().build());
					items.add(new ItemBuilder(Material.ARROW).withAmount(64).build());
					break;
				case 23:
					items.add(new ItemBuilder(Material.DIAMOND_SWORD).buildMeta().withEnchant(Enchantment.FIRE_ASPECT, 2, true).withEnchant(Enchantment.DAMAGE_ALL, 4, true).item().build());
					break;
				case 26:
					items.add(new ItemBuilder(Material.FISHING_ROD).buildMeta().withEnchant(Enchantment.KNOCKBACK, 3, true).item().build());
					break;
				}
			}
			
			Collections.shuffle(items);
			plugin.getServer().getScheduler().runTask(plugin, () -> {
				Iterator<ItemStack> it = items.iterator();
				while(it.hasNext()) {
					ItemStack item = it.next();
					Inventory inv = chest.getInventory();
					if(inv.firstEmpty() == -1) break;
					int slot;
					do {
						slot = rand.nextInt(inv.getSize()); // brewing stands slot3 is ingredient
					} while(!(inv.getItem(slot) == null || inv.getItem(slot).getType().equals(Material.AIR)));
					
					inv.setItem(slot, item);
				}
			});
		});
	}

	@Override
	public void fillCenterBrewer(Random rand, int phase, BrewingStand brewingStand) {
		if(!brewingStand.getBlock().getType().equals(Material.BREWING_STAND)) return;
		
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			List<ItemStack> potions = new ArrayList<>();
			
			for(int i = 0; i < 3; i++) {
				ItemStack ite = new ItemBuilder().withType(Material.POTION).build();
				int p = rand.nextInt(5)+1;
				if(p == 1) {
					Potion pot = new Potion(PotionType.SPEED, 2);
					pot.apply(ite);
					// SPEED 3 pot
					potions.add(new ItemBuilder(ite)
							.withType(Material.POTION)
							.buildMeta(PotionMetaBuilder.class)
							.withMainEffect(PotionEffectType.SPEED)
							.withCustomEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 40, 2), true)
							.item().build());
				} else if(p == 2) {
					Potion pot = new Potion(PotionType.POISON, 2);
					pot.apply(ite);
					// Jump 2 pot
					potions.add(new ItemBuilder(ite)
							.withType(Material.POTION)
							.buildMeta(PotionMetaBuilder.class)
							.withMainEffect(PotionEffectType.JUMP)
							.withCustomEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 20, 1), true)
							.withDisplayName(ChatColor.RESET + "Potion of Leaping")
							.item().build());
				} else if(p == 3) {
					Potion pot = new Potion(PotionType.INSTANT_HEAL, 2);
					pot.apply(ite);
					// Health 3 pot
					potions.add(new ItemBuilder(ite)
							.withType(Material.POTION)
							.buildMeta(PotionMetaBuilder.class)
							.withMainEffect(PotionEffectType.HEAL)
							.withCustomEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 20 * 25, 1), true)
							.withCustomEffect(new PotionEffect(PotionEffectType.HEAL, 1, 2), true)
							.item().build());
				} else if(p == 4) {
					Potion pot = new Potion(PotionType.INSTANT_DAMAGE, 2).splash();
					pot.apply(ite);
					// Damage 2 pot
					potions.add(new ItemBuilder(ite)
							.withType(Material.POTION)
							.buildMeta(PotionMetaBuilder.class)
							.withMainEffect(PotionEffectType.HARM)
							.withCustomEffect(new PotionEffect(PotionEffectType.HARM, 1, 1), true)
							.item().build());
				} else if(p == 5) {
					Potion pot = new Potion(PotionType.FIRE_RESISTANCE, 1);
					pot.apply(ite);
					// Fire Res pot
					potions.add(new ItemBuilder(ite)
							.withType(Material.POTION)
							.buildMeta(PotionMetaBuilder.class)
							.withMainEffect(PotionEffectType.FIRE_RESISTANCE)
							.withCustomEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 6 * 60, 0), true)
							.item().build());
				}
			}
			
			Collections.shuffle(potions);
			plugin.getServer().getScheduler().runTask(plugin, () -> {
				Iterator<ItemStack> it = potions.iterator();
				while(it.hasNext()) {
					ItemStack item = it.next();
					BrewerInventory inv = brewingStand.getInventory();
					if(inv.firstEmpty() == -1) break;
					List<Integer> slots = new ArrayList<>(Arrays.asList(0, 1, 2));
					Collections.shuffle(slots);
					for(int slot : slots) {
						if(inv.getItem(slot) == null || inv.getItem(slot).getType().equals(Material.AIR)) {
							inv.setItem(slot, item);
							break;
						}
					}
				}
			});
		});
		
	}
	
}
