package id.eventmuharram.boss;

import id.eventmuharram.EventMuharram;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.jetbrains.annotations.NotNull;
import java.util.Map;
import java.util.UUID;

public class RaksasaCommand implements CommandExecutor {

    private final EventMuharram plugin;
    private final RaksasaManager manager;

    public RaksasaCommand(EventMuharram plugin, RaksasaManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("eventmuharram.admin")) {
            sender.sendMessage(ChatColor.RED + "✖ Kamu tidak punya izin untuk ini.");
            return true;
        }

        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "spawn" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "✖ Hanya player yang bisa spawn boss.");
                    return true;
                }
                Wither boss = manager.spawnBoss(player.getLocation());
                if (boss != null)
                    sender.sendMessage(ChatColor.GOLD + "✦ Raksasa Jahiliyah berhasil dipanggil!");
                else
                    sender.sendMessage(ChatColor.RED + "✖ Gagal spawn boss.");
            }
            case "kill" -> {
                int count = manager.getBossCount();
                if (count == 0) { sender.sendMessage(ChatColor.YELLOW + "⚠ Tidak ada boss aktif."); return true; }
                manager.killAllBoss();
                sender.sendMessage(ChatColor.GREEN + "✔ " + count + " boss telah dimusnahkan.");
            }
            case "info" -> {
                Map<UUID, BossData> bosses = manager.getActiveBosses();
                sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "=== STATUS RAKSASA JAHILIYAH ===");
                if (bosses.isEmpty()) {
                    sender.sendMessage(ChatColor.YELLOW + "  Tidak ada boss aktif.");
                } else {
                    int idx = 1;
                    for (Map.Entry<UUID, BossData> entry : bosses.entrySet()) {
                        Wither w = entry.getValue().getWither();
                        if (!w.isValid() || w.isDead()) continue;
                        double maxHp = w.getAttribute(Attribute.MAX_HEALTH) != null
                                ? w.getAttribute(Attribute.MAX_HEALTH).getValue() : 1000.0;
                        double pct = (w.getHealth() / maxHp) * 100.0;
                        sender.sendMessage(ChatColor.RED + "  [" + idx + "] " +
                                ChatColor.WHITE + "HP: " + ChatColor.YELLOW +
                                String.format("%.0f / %.0f (%.1f%%)", w.getHealth(), maxHp, pct) +
                                " " + getPhaseLabel(pct));
                        sender.sendMessage(ChatColor.GRAY + "      Lokasi: " +
                                ChatColor.WHITE + "X=" + w.getLocation().getBlockX() +
                                " Y=" + w.getLocation().getBlockY() +
                                " Z=" + w.getLocation().getBlockZ() +
                                " [" + w.getWorld().getName() + "]");
                        sender.sendMessage(ChatColor.GRAY + "      Stomp: " +
                                ChatColor.WHITE + entry.getValue().getStompCount() + "x");
                        idx++;
                    }
                }
                sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "================================");
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private String getPhaseLabel(double pct) {
        if (pct > 75) return ChatColor.GREEN + "[Normal]";
        if (pct > 50) return ChatColor.YELLOW + "[Marah]";
        if (pct > 25) return ChatColor.LIGHT_PURPLE + "[Mengamuk]";
        return ChatColor.WHITE + "" + ChatColor.BOLD + "[HAMPIR MATI]";
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "=== EventMuharram ===");
        sender.sendMessage(ChatColor.RED + "  /raksasa spawn " + ChatColor.GRAY + "- Panggil boss di lokasi kamu");
        sender.sendMessage(ChatColor.RED + "  /raksasa kill  " + ChatColor.GRAY + "- Hapus semua boss aktif");
        sender.sendMessage(ChatColor.RED + "  /raksasa info  " + ChatColor.GRAY + "- Status boss aktif");
        sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "====================");
    }
}
