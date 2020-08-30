package net.kjnine.skywars.game.listeners;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.kjnine.skywars.GameManager;
import net.kjnine.skywars.Messages;
import net.kjnine.skywars.SkywarsPlugin;
import net.kjnine.skywars.game.SkywarsGame;

public class GameListener implements Listener {

	private GameManager gm;
	private SkywarsGame game;
	
	public GameListener(GameManager gm, SkywarsGame game) {
		this.gm = gm;
		this.game = game;
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onPlayerQuit(PlayerQuitEvent e) {
		e.setQuitMessage(null);
		if(game.isSpectator(e.getPlayer())) game.removeSpectator(e.getPlayer());
		if(!game.getPlayers().contains(e.getPlayer().getUniqueId())) return;
		if(!game.gameState.isIngame()) return;
		String color = e.getPlayer().hasMetadata("rankcolor") ? e.getPlayer().getMetadata("rankcolor").get(0).asString() : ChatColor.GRAY.toString();
		game.messaging().announce(Messages.DEATH_OTHER, color + e.getPlayer().getDisplayName());
		playerDeath(e.getPlayer());
		game.removeSpectator(e.getPlayer());
	}
	
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent e) {
		if(e.getEntity() instanceof EnderDragon) e.setCancelled(true);
		if(!(e.getEntity() instanceof Player)) return;
		Player p = (Player) e.getEntity();
		if(game.isSpectator(p)) {
			e.setCancelled(true);
			if(e.getCause().equals(DamageCause.VOID)) p.teleport(p.getWorld().getSpawnLocation());
			return;
		}
		if(!game.getPlayers().contains(p.getUniqueId())) return;
		if(!game.gameState.isIngame()) {
			e.setCancelled(true);
			return;
		}
		
		String color = p.hasMetadata("rankcolor") ? p.getMetadata("rankcolor").get(0).asString() : ChatColor.GRAY.toString();
		
		if(p.getHealth() - e.getFinalDamage() <= 0 || e.getCause().equals(DamageCause.VOID)) {
			if(p.hasMetadata("lasthit")) {
				UUID u = UUID.fromString(p.getMetadata("lasthit").get(0).asString());
				Player kp = p.getServer().getPlayer(u);
				if(kp != null) {
					String kco = kp.hasMetadata("rankcolor") ? kp.getMetadata("rankcolor").get(0).asString() : ChatColor.GRAY.toString();
					switch(e.getCause()) {
					case FIRE:
					case FIRE_TICK:
					case LAVA:
						playerKill(kp);
						game.messaging().announce(Messages.KILL_FIRE, color+p.getDisplayName(), kco+kp.getDisplayName());
						break;
					case BLOCK_EXPLOSION:
						playerKill(kp);
						game.messaging().announce(Messages.KILL_EXPLODE, color+p.getDisplayName(), kco+kp.getDisplayName());
						break;
					case FALL:
						playerKill(kp);
						game.messaging().announce(Messages.KILL_FALL, color+p.getDisplayName(), kco+kp.getDisplayName());
						break;
					case VOID:
						playerKill(kp);
						game.messaging().announce(Messages.KILL_VOID, color+p.getDisplayName(), kco+kp.getDisplayName());
						break;
					case LIGHTNING:
					case CONTACT:
					case THORNS:
					case DROWNING:
					case MELTING:
					case FALLING_BLOCK:
					case SUFFOCATION:
					case CUSTOM:
					case SUICIDE:
					case MAGIC:
					case POISON:
					case WITHER:
					case STARVATION:
						playerKill(kp);
						game.messaging().announce(Messages.KILL_OTHER, color+p.getDisplayName(), kco+kp.getDisplayName());
						break;
					case PROJECTILE:
					case ENTITY_ATTACK:
					case ENTITY_EXPLOSION:
						EntityDamageByEntityEvent edbe = (EntityDamageByEntityEvent) e;
						if(edbe.getDamager() instanceof Projectile) {
							Projectile proj = (Projectile) edbe.getDamager();
							if(!(proj.getShooter() instanceof Player)) {
								if(proj.getShooter() instanceof LivingEntity) {
									if(((LivingEntity)proj.getShooter()).getCustomName() != null) {
										game.messaging().announce(Messages.KILL_SHOT, color+p.getDisplayName(), ChatColor.YELLOW + ((LivingEntity)proj.getShooter()).getCustomName());
									} else 
										game.messaging().announce(Messages.KILL_SHOT, color+p.getDisplayName(), ChatColor.YELLOW + proj.getShooter().getClass().getSimpleName().replace("Craft", ""));
								} else
									game.messaging().announce(Messages.KILL_SHOT, color+p.getDisplayName(), ChatColor.YELLOW + proj.getShooter().getClass().getSimpleName().replace("Craft", ""));
							} else {
								Player shooter = (Player) proj.getShooter();
								String co = shooter.hasMetadata("rankcolor") ? shooter.getMetadata("rankcolor").get(0).asString() : ChatColor.GRAY.toString();
								playerKill(shooter);
								game.messaging().announce(Messages.KILL_SHOT, color+p.getDisplayName(), co+shooter.getDisplayName());
							}
						} else if(edbe.getDamager() instanceof TNTPrimed) {
							TNTPrimed tnt = (TNTPrimed) edbe.getDamager();
							if(tnt.getSource() != null && tnt.getSource() instanceof Player) {
								Player killer = (Player) tnt.getSource();
								String co = killer.hasMetadata("rankcolor") ? killer.getMetadata("rankcolor").get(0).asString() : ChatColor.GRAY.toString();
								playerKill(killer);
								game.messaging().announce(Messages.KILL_EXPLODE, color+p.getDisplayName(), co+killer.getDisplayName());
							} else {
								playerKill(kp);
								game.messaging().announce(Messages.KILL_EXPLODE, color+p.getDisplayName(), kco+kp.getDisplayName());
							}
						} else {
							if(!(edbe.getDamager() instanceof Player)) {
								if(edbe.getDamager() instanceof LivingEntity) {
									if(((LivingEntity)edbe.getDamager()).getCustomName() != null) {
										game.messaging().announce(Messages.KILL_OTHER, color+p.getDisplayName(), ChatColor.YELLOW + ((LivingEntity)edbe.getDamager()).getCustomName());
									} else 
										game.messaging().announce(Messages.KILL_OTHER, color+p.getDisplayName(), ChatColor.YELLOW + edbe.getDamager().getClass().getSimpleName().replace("Craft", ""));
								} else
									game.messaging().announce(Messages.KILL_OTHER, color+p.getDisplayName(), ChatColor.YELLOW + edbe.getDamager().getClass().getSimpleName().replace("Craft", ""));
							} else {
								Player killer = (Player) edbe.getDamager();
								String co = killer.hasMetadata("rankcolor") ? killer.getMetadata("rankcolor").get(0).asString() : ChatColor.GRAY.toString();
								playerKill(killer);
								game.messaging().announce(Messages.KILL_OTHER, color+p.getDisplayName(), co+killer.getDisplayName());
							}
						}
						break;
					}
					e.setCancelled(true);
					e.setDamage(0);
					p.setHealth(p.getMaxHealth());
					playerDeath(p);
					return;
				}
			}
			switch(e.getCause()) {
			case FIRE:
			case FIRE_TICK:
			case LAVA:
				game.messaging().announce(Messages.DEATH_FIRE, color+p.getDisplayName());
				break;
			case BLOCK_EXPLOSION:
				game.messaging().announce(Messages.DEATH_EXPLODE, color+p.getDisplayName());
				break;
			case CONTACT:
			case THORNS:
				game.messaging().announce(Messages.DEATH_THORNS, color+p.getDisplayName());
				break;
			case DROWNING:
			case MELTING:
			case FALLING_BLOCK:
			case SUFFOCATION:
				game.messaging().announce(Messages.DEATH_BLOCK, color+p.getDisplayName());
				break;
			case CUSTOM:
			case SUICIDE:
				game.messaging().announce(Messages.DEATH_OTHER, color+p.getDisplayName());
				break;
			case FALL:
				game.messaging().announce(Messages.DEATH_FALL, color+p.getDisplayName());
				break;
			case VOID:
				game.messaging().announce(Messages.DEATH_VOID, color+p.getDisplayName());
				break;
			case LIGHTNING:
				game.messaging().announce(Messages.DEATH_SMITE, color+p.getDisplayName());
				break;
			case MAGIC:
			case POISON:
			case WITHER:
				game.messaging().announce(Messages.DEATH_MAGIC, color+p.getDisplayName());
				break;
			case STARVATION:
				game.messaging().announce(Messages.DEATH_STARVE, color+p.getDisplayName());
				break;
			case PROJECTILE:
			case ENTITY_ATTACK:
			case ENTITY_EXPLOSION:
				EntityDamageByEntityEvent edbe = (EntityDamageByEntityEvent) e;
				if(edbe.getDamager() instanceof Projectile) {
					Projectile proj = (Projectile) edbe.getDamager();
					if(!(proj.getShooter() instanceof Player)) {
						if(proj.getShooter() instanceof LivingEntity) {
							if(((LivingEntity)proj.getShooter()).getCustomName() != null) {
								game.messaging().announce(Messages.KILL_SHOT, color+p.getDisplayName(), ChatColor.YELLOW + ((LivingEntity)proj.getShooter()).getCustomName());
							} else 
								game.messaging().announce(Messages.KILL_SHOT, color+p.getDisplayName(), ChatColor.YELLOW + proj.getShooter().getClass().getSimpleName().replace("Craft", ""));
						} else
							game.messaging().announce(Messages.KILL_SHOT, color+p.getDisplayName(), ChatColor.YELLOW + proj.getShooter().getClass().getSimpleName().replace("Craft", ""));
					} else {
						Player shooter = (Player) proj.getShooter();
						String co = shooter.hasMetadata("rankcolor") ? shooter.getMetadata("rankcolor").get(0).asString() : ChatColor.GRAY.toString();
						playerKill(shooter);
						game.messaging().announce(Messages.KILL_SHOT, color+p.getDisplayName(), co+shooter.getDisplayName());
					}
				} else if(edbe.getDamager() instanceof TNTPrimed) {
					TNTPrimed tnt = (TNTPrimed) edbe.getDamager();
					if(tnt.getSource() != null && tnt.getSource() instanceof Player) {
						Player killer = (Player) tnt.getSource();
						String co = killer.hasMetadata("rankcolor") ? killer.getMetadata("rankcolor").get(0).asString() : ChatColor.GRAY.toString();
						playerKill(killer);
						game.messaging().announce(Messages.KILL_EXPLODE, color+p.getDisplayName(), co+killer.getDisplayName());
					} else {
						game.messaging().announce(Messages.DEATH_EXPLODE, color+p.getDisplayName());
					}
				} else {
					if(!(edbe.getDamager() instanceof Player)) {
						if(edbe.getDamager() instanceof LivingEntity) {
							if(((LivingEntity)edbe.getDamager()).getCustomName() != null) {
								game.messaging().announce(Messages.KILL_OTHER, color+p.getDisplayName(), ChatColor.YELLOW + ((LivingEntity)edbe.getDamager()).getCustomName());
							} else 
								game.messaging().announce(Messages.KILL_OTHER, color+p.getDisplayName(), ChatColor.YELLOW + edbe.getDamager().getClass().getSimpleName().replace("Craft", ""));
						} else
							game.messaging().announce(Messages.KILL_OTHER, color+p.getDisplayName(), ChatColor.YELLOW + edbe.getDamager().getClass().getSimpleName().replace("Craft", ""));
					} else {
						Player killer = (Player) edbe.getDamager();
						String co = killer.hasMetadata("rankcolor") ? killer.getMetadata("rankcolor").get(0).asString() : ChatColor.GRAY.toString();
						playerKill(killer);
						game.messaging().announce(Messages.KILL_OTHER, color+p.getDisplayName(), co+killer.getDisplayName());
					}
				}
				break;
			}
			e.setCancelled(true);
			e.setDamage(0);
			p.setHealth(p.getMaxHealth());
			playerDeath(p);
		} else {
			if(e.getCause().equals(DamageCause.PROJECTILE) || e.getCause().equals(DamageCause.ENTITY_ATTACK) || e.getCause().equals(DamageCause.ENTITY_EXPLOSION)) {
				EntityDamageByEntityEvent edbe = (EntityDamageByEntityEvent) e;
				if(edbe.getDamager() instanceof Projectile) {
					Projectile proj = (Projectile) edbe.getDamager();
					if(proj.getShooter() instanceof Player) {
						Player shooter = (Player) proj.getShooter();
						if(!shooter.getUniqueId().equals(p.getUniqueId()))
							p.setMetadata("lasthit", new FixedMetadataValue(SkywarsPlugin.getInstance(), shooter.getUniqueId().toString()));
					}
				} else if(edbe.getDamager() instanceof TNTPrimed) {
					TNTPrimed tnt = (TNTPrimed) edbe.getDamager();
					if(tnt.getSource() != null && tnt.getSource() instanceof Player) {
						Player bomber = (Player) tnt.getSource();
						if(!bomber.getUniqueId().equals(p.getUniqueId()))
							p.setMetadata("lasthit", new FixedMetadataValue(SkywarsPlugin.getInstance(), bomber.getUniqueId().toString()));
					}
				} else if(edbe.getDamager() instanceof Player) {
					Player attacker = (Player) edbe.getDamager();
					if(!attacker.getUniqueId().equals(p.getUniqueId()))
						p.setMetadata("lasthit", new FixedMetadataValue(SkywarsPlugin.getInstance(), attacker.getUniqueId().toString()));
				}
			}
		}
	}
	
	/**
	 * Returns whether the death was accepted. (Cancel event if its false, they didnt die)
	 */
	private boolean playerDeath(Player p) {
		if(!game.gameState.isIngame()) return false;
		
		Location loc = p.getLocation();
		
		game.removeFromGame(p);
		game.addSpectator(p);
		
		p.setHealth(p.getMaxHealth());
		
		if(game.getPlayers().size() == 0) {
			game.win(p);
			return false;
		}
		
		gm.getStats().addDeath(p);
		
		if(game.getPlayers().size() == 1) {
			p.getWorld().strikeLightningEffect(loc);
			Player winner = p.getServer().getPlayer(game.getPlayers().stream().findFirst().get());
			game.win(winner);
		}
		
		return true;
	}
	
	private void playerKill(Player p ) {
		if(!game.gameState.isIngame()) return;
		
		game.kills.put(p.getUniqueId(), game.kills.getOrDefault(p.getUniqueId(), 0)+1);
		gm.getStats().addKill(p);
		game.applyKillboost(p);
		
		p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1f, 1f);
	}
	
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if(!(e.getEntity() instanceof Player)) return;
		if(!(e.getDamager() instanceof Player)) return;
		Player p = (Player) e.getEntity();
		Player d = (Player) e.getDamager();
		p.setMetadata("lasthit", new FixedMetadataValue(SkywarsPlugin.getInstance(), d.getUniqueId().toString()));
	}
	
	@EventHandler
	public void onPickup(PlayerPickupItemEvent e) {
		if(e.getPlayer() == null) System.out.println("WHAT TE FUCK");
		if(e.getPlayer().getWorld() == null) return;
		if(e.getPlayer().getWorld().getName().equals(game.getGameWorld().getName())) {
			if(!game.isAlive(e.getPlayer()) || !game.gameState.isIngame()) {
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onThrow(ProjectileLaunchEvent e) {
		if(e.getEntity() instanceof ThrownPotion) {
			ThrownPotion pot = (ThrownPotion) e.getEntity();
			if(pot.getEffects().stream().filter(eff -> eff.getType().equals(PotionEffectType.HARM)).count() > 0) {
				if(pot.getShooter() != null && (pot.getShooter() instanceof LivingEntity)) {
					pot.setVelocity(((LivingEntity)pot.getShooter()).getEyeLocation().getDirection().multiply(4));
				}
			}
		}
	}
	
	@EventHandler
	public void onPotion(PotionSplashEvent e) {
		for(LivingEntity le : e.getAffectedEntities()) {
			if(le instanceof Player) {
				ThrownPotion proj = e.getPotion();
				if(proj.getShooter() instanceof Player) {
					Player shooter = (Player) proj.getShooter();
					if(shooter.getUniqueId().equals(((Player)le).getUniqueId())) continue;
					boolean harmful = false;
					for(PotionEffect eff : proj.getEffects()) {
						if(eff.getType().equals(PotionEffectType.POISON)
								|| eff.getType().equals(PotionEffectType.HARM)
								|| eff.getType().equals(PotionEffectType.WITHER)) {
							harmful = true;
							break;
						}
					}
					if(harmful)
						((Player)le).setMetadata("lasthit", new FixedMetadataValue(SkywarsPlugin.getInstance(), shooter.getUniqueId().toString()));
				}
			}
		}
	}
	
	
}
