package net.kjnine.skywars.game;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.inventivetalent.itembuilder.ItemBuilder;

import net.kjnine.skywars.SkywarsPlugin;

public enum Killboost {
	
	RESISTANCE(p -> p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 7, 1)), Material.IRON_INGOT, "&9Resistance", "Resistance II", "7 seconds", true),
	REGEN(p -> {
		if(p.hasPotionEffect(PotionEffectType.REGENERATION)
				&& p.getActivePotionEffects().stream()
				.filter(e -> e.getType().equals(PotionEffectType.REGENERATION))
				.filter(a -> a.getAmplifier() < 2).count() > 0) 
			p.removePotionEffect(PotionEffectType.REGENERATION); 
		p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 7, 1));
	}, Material.SPECKLED_MELON, "&dRegeneration", "Regeneration II", "7 seconds"),
	ABSORPTION(p -> p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 7, 2)), Material.GOLD_INGOT, "&6Absorption", "Absorption III &7(&e\u2764\u2764\u2764\u2764\u2764\u2764&7)", "7 seconds"),
	STRENGTH(p -> p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 7, 0)), Material.BLAZE_POWDER, "&cStrength", "Strength I", "7 seconds");
	
	private Consumer<Player> applyFunc;
	private Material mat;
	private boolean unlockDefault = false;
	private String effName, effStr, durStr;
	Killboost(Consumer<Player> applyFunc, Material mat, String effName, String effStr, String durStr) {
		this.applyFunc = applyFunc;
		this.mat = mat;
		this.effName = effName;
		this.effStr = effStr;
		this.durStr = durStr;
	}
	Killboost(Consumer<Player> applyFunc, Material mat, String effName, String effStr, String durStr, boolean unlock) {
		this(applyFunc, mat, effName, effStr, durStr);
		this.unlockDefault = unlock;
	}
	
	public void apply(Player p) {
		applyFunc.accept(p);
	}
	
	public String getFormattedName() {
		return Messaging.colorcode(effName);
	}
	
	public ItemStack getItem(SkywarsGame g, UUID u) {
		boolean acc = hasAccess(u);
		// player online, check selected, etc.
		Player p = SkywarsPlugin.getInstance().getServer().getPlayer(u);
		boolean sel = false;
		Killboost kb;
		if(p != null && p.hasMetadata("killboost"))
			kb = Killboost.valueOf(p.getMetadata("killboost").get(0).asString());
		else
			kb = Killboost.RESISTANCE;
		sel = (kb.equals(this));
		ItemBuilder ib = new ItemBuilder(acc ? mat : Material.STAINED_GLASS_PANE).buildMeta()
		.withDisplayName(Messaging.colorcode(effName))
		.withLore(
				Messaging.colorcode("&7Receive &e" + effStr), 
				Messaging.colorcode("&7for &e" + durStr + "&7 upon"), 
				Messaging.colorcode("&7killing a player."),
				" ", 
				" ",
				Messaging.colorcode(sel ? "&aSelected" : (acc ? ("&eClick to Select") : ("&cLocked"))))
		.item();
		if(!acc) ib = ib.withDurability(11);
		return ib.build();
	}
	
	private boolean hasAccess(UUID u) {
		if(unlockDefault) return true;
		Player p = SkywarsPlugin.getInstance().getServer().getPlayer(u);
		if(p != null) {
			if(p.hasMetadata("killboosts_unlocked")) {
				String unlocked = p.getMetadata("killboosts_unlocked").get(0).asString();
				Set<Killboost> kbs = Arrays.asList(unlocked.split(";")).stream().distinct().map(str -> Killboost.valueOf(str)).collect(Collectors.toSet());
				if(kbs.contains(this)) return true;
			}
		}
		return false;
	}
	
	public static boolean hasAccess(UUID u, Killboost kb) {
		return kb.hasAccess(u);
	}
	
}
