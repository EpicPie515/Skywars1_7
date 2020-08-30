package net.kjnine.skywars.game.cages;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import net.kjnine.skywars.SkywarsPlugin;
import net.minecraft.server.v1_7_R4.DamageSource;
import net.minecraft.server.v1_7_R4.EntityHorse;
import net.minecraft.server.v1_7_R4.World;

public class EntityCage extends EntityHorse {

	private CraftPlayer p;
	
	public EntityCage(World world) {
		super(world);
	}
	
	public EntityCage(Player p) {
		super(((CraftPlayer) p).getHandle().getWorld());
		this.p = ((CraftPlayer) p);
	}
	
	public void clearCage() {
		this.getBukkitEntity().setPassenger(null);
		dead = true;
	}
	
	@Override
	public void move(double arg0, double arg1, double arg2) {
		super.move(0, arg1, 0);
	}
	
	@Override
	public void makeSound(String s, float f, float f1) { }
	
	@Override
	public void w(int arg0) {
		this.bt = 0.0f;
	}
	
	@Override
	public boolean damageEntity(DamageSource arg0, float arg1) { return false; }
	
	public void enableCage(Location blockAboveSpawnPoint) {
		setInvisible(true);
		setVariant(-1);
		setTame(true);
		((Horse)getBukkitEntity()).setMaxHealth(2);
		setHealth(2f);
		Location loc = blockAboveSpawnPoint.clone();
		setLocation(loc.getX(), loc.getY(), loc.getZ(), 0, 0);
		world.addEntity(this, SpawnReason.CUSTOM);
		SkywarsPlugin.getInstance().getServer().getScheduler().runTaskLater(SkywarsPlugin.getInstance(), () -> {
			getBukkitEntity().setPassenger(p);
		}, 2L);
		
	}
	
	
	
}
