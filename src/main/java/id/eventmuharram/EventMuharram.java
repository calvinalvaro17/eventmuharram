package id.eventmuharram;

import id.eventmuharram.boss.RaksasaCommand;
import id.eventmuharram.boss.RaksasaListener;
import id.eventmuharram.boss.RaksasaManager;
import org.bukkit.plugin.java.JavaPlugin;

public class EventMuharram extends JavaPlugin {

    private RaksasaManager raksasaManager;

    @Override
    public void onEnable() {
        raksasaManager = new RaksasaManager(this);
        var cmd = getCommand("raksasa");
        if (cmd != null) cmd.setExecutor(new RaksasaCommand(this, raksasaManager));
        getServer().getPluginManager().registerEvents(new RaksasaListener(this, raksasaManager), this);
        getLogger().info("EventMuharram aktif - /raksasa spawn untuk memanggil boss.");
    }

    @Override
    public void onDisable() {
        if (raksasaManager != null) raksasaManager.killAllBoss();
    }
}
