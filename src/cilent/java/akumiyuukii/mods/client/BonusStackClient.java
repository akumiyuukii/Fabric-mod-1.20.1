package akumiyuukii.mods.client;

/**
 * Client-side holder for the latest bonus-damage stack info received from the server.
 * Populated by the BonusStackSync packet receiver; read by StackInfoScreen.
 */
public class BonusStackClient {
    public static int stacks = 0;
    public static int maxStacks = 0;
    public static int level = 0;
    public static double weaponPercent = 0;
    public static double statPercent = 0;
    public static double perStack = 0;
    // Client time (ms) of the last update, used to fade out stacks after 5 minutes.
    public static long lastUpdateMs = 0;

    public static final long EXPIRE_MS = 5 * 60 * 1000L;

    public static void update(int stacks, int maxStacks, int level,
                              double weaponPercent, double statPercent, double perStack) {
        BonusStackClient.stacks = stacks;
        BonusStackClient.maxStacks = maxStacks;
        BonusStackClient.level = level;
        BonusStackClient.weaponPercent = weaponPercent;
        BonusStackClient.statPercent = statPercent;
        BonusStackClient.perStack = perStack;
        BonusStackClient.lastUpdateMs = System.currentTimeMillis();
    }

    /** Effective current stacks, accounting for the 5-minute expiry on the client. */
    public static int currentStacks() {
        if (System.currentTimeMillis() - lastUpdateMs > EXPIRE_MS) {
            return 0;
        }
        return stacks;
    }

    /** Milliseconds remaining before the stacks expire; 0 if already expired. */
    public static long remainingMs() {
        long left = EXPIRE_MS - (System.currentTimeMillis() - lastUpdateMs);
        return Math.max(0, left);
    }
}
