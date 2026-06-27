package id.eventmuharram.boss;

import id.eventmuharram.EventMuharram;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

public class RaksasaManager {

    private final EventMuharram plugin;
    private final Map<UUID, BossData> activeBosses = new HashMap<>();

    public RaksasaManager(EventMuharram plugin) {
        this.plugin = plugin;
    }

    public Wither spawnBoss(Location loc) {
        World world = loc.getWorld();
        if (world == null) return null;

        Wither wither = (Wither) world.spawnEntity(loc, EntityType.WITHER);
        wither.setCustomName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "☠ Raksasa Jahiliyah ☠");
        wither.setCustomNameVisible(true);

        Objects.requireNonNull(wither.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(1000.0);
        wither.setHealth(1000.0);
        Objects.requireNonNull(wither.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.32);
        Objects.requireNonNull(wither.getAttribute(Attribute.ATTACK_DAMAGE)).setBaseValue(14.0);
        Objects.requireNonNull(wither.getAttribute(Attribute.FOLLOW_RANGE)).setBaseValue(60.0);
        Objects.requireNonNull(wither.getAttribute(Attribute.KNOCKBACK_RESISTANCE)).setBaseValue(1.0);
        wither.setRemoveWhenFarAway(false);

        BossBar bossBar = Bukkit.createBossBar(
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "☠ RAKSASA JAHILIYAH ☠",
                BarColor.RED, BarStyle.SEGMENTED_10);
        bossBar.setProgress(1.0);
        bossBar.setVisible(true);
        for (Player p : Bukkit.getOnlinePlayers()) bossBar.addPlayer(p);

        BossData data = new BossData(wither, bossBar);
        activeBosses.put(wither.getUniqueId(), data);

        world.strikeLightningEffect(loc);
        world.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 3.0f, 0.8f);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 3, 1, 0.5, 1, 0);
        world.spawnParticle(Particle.LARGE_SMOKE, loc, 50, 2, 1, 2, 0.1);

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "╔═══════════════════════════════╗");
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "  ☠  RAKSASA JAHILIYAH BANGKIT  ☠");
        Bukkit.broadcastMessage(ChatColor.RED + "  Makhluk kuno telah terjaga dari kegelapan!");
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "╚═══════════════════════════════╝");
        Bukkit.broadcastMessage("");

        startAI(wither, data);
        return wither;
    }

    private void startAI(Wither wither, BossData data) {
        new BukkitRunnable() {
            int tick = 0;
            int stompCooldown = 0;
            int rageCooldown = 0;
            int dashCooldown = 0;

            @Override
            public void run() {
                if (wither.isDead() || !wither.isValid()) { cancel(); return; }

                tick++;
                if (stompCooldown > 0) stompCooldown--;
                if (rageCooldown > 0) rageCooldown--;
                if (dashCooldown > 0) dashCooldown--;

                double maxHp = Objects.requireNonNull(wither.getAttribute(Attribute.MAX_HEALTH)).getValue();
                double progress = Math.max(0.0, Math.min(1.0, wither.getHealth() / maxHp));
                data.getBossBar().setProgress(progress);
                updateBarColor(data.getBossBar(), progress);

                Player target = findSmartTarget(wither, data);
                if (target == null) return;

                wither.setTarget(target);

                if (stompCooldown <= 0) {
                    double dist = wither.getLocation().distance(target.getLocation());
                    if (dist <= 10.0 || progress < 0.75) {
                        performStomp(wither, data);
                        stompCooldown = progress < 0.5 ? 80 : 160;
                    }
                }

                if (rageCooldown <= 0 && progress < 0.5) {
                    performRage(wither);
                    rageCooldown = 200;
                }

                if (dashCooldown <= 0 && progress < 0.3) {
                    performDash(wither, target);
                    dashCooldown = 100;
                }

                if (tick % 400 == 0) broadcastTaunt();
            }
        }.runTaskTimer(plugin, 20L, 1L);
    }

    private Player findSmartTarget(Wither wither, BossData data) {
        Player best = null;
        double bestScore = Double.MAX_VALUE;
        for (Player p : wither.getWorld().getPlayers()) {
            if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) continue;
            if (!p.isValid() || p.isDead()) continue;
            double dist = wither.getLocation().distance(p.getLocation());
            if (dist > 60.0) continue;
            double score = dist - (p.getMaxHealth() - p.getHealth()) * 2.0;
            if (score < bestScore) { bestScore = score; best = p; }
        }
        if (best != null) data.setLastTarget(best.getUniqueId());
        return best;
    }

    private void performStomp(Wither wither, BossData data) {
        Location loc = wither.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        world.playSound(loc, Sound.ENTITY_WITHER_BREAK_BLOCK, 2.5f, 0.4f);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
        world.playSound(loc, Sound.BLOCK_STONE_BREAK, 2.0f, 0.3f);

        for (int i = 0; i < 360; i += 15) {
            double rad = Math.toRadians(i);
            for (double r = 1.0; r <= 8.0; r += 1.5) {
                Location pLoc = loc.clone().add(Math.cos(rad) * r, 0.1, Math.sin(rad) * r);
                world.spawnParticle(Particle.EXPLOSION, pLoc, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.DUST, pLoc, 3, 0.2, 0.1, 0.2,
                        new Particle.DustOptions(Color.fromRGB(80, 0, 0), 1.5f));
            }
        }
        world.spawnParticle(Particle.LARGE_SMOKE, loc, 20, 1.5, 0.5, 1.5, 0.05);

        for (Player p : world.getPlayers()) {
            if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) continue;
            double dist = p.getLocation().distance(loc);
            if (dist > 12.0) continue;
            double force = Math.max(0.3, 1.8 - (dist / 12.0) * 1.5);
            Vector dir = p.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(force).setY(0.55);
            p.setVelocity(dir);
            double dmg = dist <= 4.0 ? 10.0 : (dist <= 8.0 ? 6.0 : 3.0);
            p.damage(dmg, wither);
            p.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "⚡ Raksasa Jahiliyah menghentak tanah!");
        }
        data.incrementStomp();
    }

    private void performRage(Wither wither) {
        Location loc = wither.getLocation();
        World world = wither.getWorld();
        world.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, 3.0f, 0.5f);
        world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.5f, 0.8f);
        world.strikeLightningEffect(loc);
        world.spawnParticle(Particle.WITCH, loc, 60, 2.0, 1.0, 2.0, 0.2);
        world.spawnParticle(Particle.LARGE_SMOKE, loc, 30, 1.5, 0.5, 1.5, 0.1);
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD +
                "☠ [RAKSASA JAHILIYAH] " + ChatColor.RED + "Amarahnya membara! Semua akan binasa!");
        for (Player p : world.getPlayers()) {
            if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) continue;
            double dist = p.getLocation().distance(loc);
            if (dist > 20.0) continue;
            Vector dir = p.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.8).setY(0.4);
            p.setVelocity(dir);
        }
    }

    private void performDash(Wither wither, Player target) {
        Location loc = wither.getLocation();
        Vector dir = target.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.5);
        dir.setY(Math.max(dir.getY(), 0.3));
        wither.setVelocity(dir);
        wither.getWorld().playSound(loc, Sound.ENTITY_WITHER_SHOOT, 2.0f, 1.5f);
        wither.getWorld().spawnParticle(Particle.SOUL, loc, 30, 1.0, 0.5, 1.0, 0.15);
    }

    private void updateBarColor(BossBar bar, double progress) {
        if (progress > 0.75) {
            bar.setColor(BarColor.RED);
            bar.setTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "☠ RAKSASA JAHILIYAH ☠");
        } else if (progress > 0.5) {
            bar.setColor(BarColor.YELLOW);
            bar.setTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "⚡ RAKSASA JAHILIYAH - MARAH ⚡");
        } else if (progress > 0.25) {
            bar.setColor(BarColor.PURPLE);
            bar.setTitle(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "☠ RAKSASA JAHILIYAH - MENGAMUK ☠");
        } else {
            bar.setColor(BarColor.WHITE);
            bar.setTitle(ChatColor.WHITE + "" + ChatColor.BOLD + "✦ RAKSASA JAHILIYAH - HAMPIR MATI ✦");
        }
    }

    private void broadcastTaunt() {
        String[] taunts = {
            "Kalian hanyalah debu di hadapan kegelapan abadi!",
            "Tidak ada yang bisa menghentikanku!",
            "Aku telah ada sebelum cahaya pertama!",
            "Perlawanan kalian hanya memperlambat kematian!",
            "Bergabunglah dengan kegelapan... atau hancur!"
        };
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "☠ [RAKSASA JAHILIYAH] " +
                ChatColor.RED + taunts[new Random().nextInt(taunts.length)]);
    }

    public void onBossDeath(UUID uid) {
        BossData data = activeBosses.remove(uid);
        if (data == null) return;
        data.getBossBar().removeAll();
        data.getBossBar().setVisible(false);
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "✦ RAKSASA JAHILIYAH TELAH DIKALAHKAN! ✦");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "  Kegelapan telah mundur, cahaya kembali bersinar!");
        Bukkit.broadcastMessage("");
    }

    public void addPlayerToBossBars(Player player) {
        for (BossData data : activeBosses.values()) data.getBossBar().addPlayer(player);
    }

    public void killAllBoss() {
        for (Map.Entry<UUID, BossData> entry : activeBosses.entrySet()) {
            entry.getValue().getBossBar().removeAll();
            for (World w : Bukkit.getWorlds())
                for (Entity e : w.getEntities())
                    if (e.getUniqueId().equals(entry.getKey())) e.remove();
        }
        activeBosses.clear();
    }

    public boolean isBoss(UUID uid) { return activeBosses.containsKey(uid); }
    public int getBossCount() { return activeBosses.size(); }
    public Map<UUID, BossData> getActiveBosses() { return activeBosses; }
}
