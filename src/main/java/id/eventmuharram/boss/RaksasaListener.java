package id.eventmuharram.boss;

import id.eventmuharram.EventMuharram;
import org.bukkit.ChatColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;

public class RaksasaListener implements Listener {

    private final EventMuharram plugin;
    private final RaksasaManager manager;

    public RaksasaListener(EventMuharram plugin, RaksasaManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        manager.addPlayerToBossBars(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Wither wither)) return;
        if (!manager.isBoss(wither.getUniqueId())) return;

        Location loc = wither.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_DEATH, 3.0f, 0.8f);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH, 3.0f, 0.6f);
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 5, 2, 1, 2, 0);
        loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 80, 3, 1.5, 3, 0.2);
        loc.getWorld().spawnParticle(Particle.SOUL, loc, 50, 2, 1, 2, 0.3);

        for (int i = 0; i < 3; i++) {
            final int delay = i * 10;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Firework fw = (Firework) loc.getWorld().spawnEntity(
                        loc.clone().add((Math.random() - 0.5) * 4, 0, (Math.random() - 0.5) * 4),
                        EntityType.FIREWORK_ROCKET);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .withColor(org.bukkit.Color.GOLD, org.bukkit.Color.RED)
                        .withFade(org.bukkit.Color.WHITE)
                        .with(FireworkEffect.Type.BURST)
                        .trail(true).flicker(true).build());
                meta.setPower(1);
                fw.setFireworkMeta(meta);
            }, delay);
        }

        event.getDrops().clear();
        loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.NETHER_STAR, 3));
        loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.DIAMOND, 5));
        event.setDroppedExp(500);

        manager.onBossDeath(wither.getUniqueId());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBossHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Wither wither)) return;
        if (!manager.isBoss(wither.getUniqueId())) return;

        Location loc = wither.getLocation();
        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 8, 0.5, 0.5, 0.5,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(180, 0, 0), 1.2f));

        if (event.getDamager() instanceof Player player) {
            if (player.getLocation().distance(loc) <= 3.0) {
                Vector knockback = player.getLocation().toVector()
                        .subtract(loc.toVector()).normalize().multiply(1.2).setY(0.5);
                player.setVelocity(knockback);
                player.sendMessage(ChatColor.RED + "⚡ Terlalu dekat! Kamu didorong mundur!");
            }
        }

        if (event.getDamager() instanceof Projectile proj) {
            if (proj.getShooter() instanceof Player shooter) wither.setTarget(shooter);
        }
    }

    @EventHandler
    public void onWitherSkull(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof WitherSkull skull)) return;
        if (!(skull.getShooter() instanceof Wither wither)) return;
        if (!manager.isBoss(wither.getUniqueId())) return;
        if (event.getHitEntity() instanceof Player player)
            player.sendMessage(ChatColor.DARK_RED + "☠ Terkena serangan Raksasa Jahiliyah!");
    }
}
