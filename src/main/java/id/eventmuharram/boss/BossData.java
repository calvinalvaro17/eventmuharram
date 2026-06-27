package id.eventmuharram.boss;

import org.bukkit.boss.BossBar;
import org.bukkit.entity.Wither;
import java.util.UUID;

public class BossData {

    private final Wither wither;
    private final BossBar bossBar;
    private UUID lastTarget;
    private int stompCount = 0;

    public BossData(Wither wither, BossBar bossBar) {
        this.wither = wither;
        this.bossBar = bossBar;
    }

    public Wither getWither() { return wither; }
    public BossBar getBossBar() { return bossBar; }
    public UUID getLastTarget() { return lastTarget; }
    public void setLastTarget(UUID uid) { this.lastTarget = uid; }
    public int getStompCount() { return stompCount; }
    public void incrementStomp() { stompCount++; }
}
