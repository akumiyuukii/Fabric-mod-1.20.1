package akumiyuukii.mods;

/**
 * Static configuration for the "Tăng sát thương kèm theo" enchantment.
 *
 * The enchantment deals real bonus damage each hit and builds up "bonus damage stacks"
 * that are stored in the player's own data (see {@link PlayerStats}), NOT as a MobEffect.
 *
 * Bonus damage per hit =
 *     weaponPercent * (sword/tool attack damage)
 *   + statPercent   * (player stat attack damage)
 *   + (stackCount * perStackBonus) * (sword damage + stat damage)
 *
 * From level 5 onward, every extra level (via /give with a higher level) adds +1% to all
 * three percentages.
 */
public final class BonusDamageConfig {
    private BonusDamageConfig() {}

    public static final int MAX_STACKS_I = 5;
    public static final int MAX_STACKS_II = 7;
    public static final int MAX_STACKS_III = 10;
    public static final int MAX_STACKS_IV = 15;
    public static final int MAX_STACKS_V = 20;

    public static final double PER_STACK_I = 0.02;
    public static final double PER_STACK_II = 0.03;
    public static final double PER_STACK_III = 0.04;
    public static final double PER_STACK_IV = 0.045;
    public static final double PER_STACK_V = 0.05;

    /** Stacks and duration reset 5 minutes after the last hit. */
    public static final int DURATION_TICKS = 5 * 60 * 20; // 5 minutes in ticks

    public static int getMaxStacks(int enchantmentLevel) {
        int base = switch (enchantmentLevel) {
            case 1 -> MAX_STACKS_I;
            case 2 -> MAX_STACKS_II;
            case 3 -> MAX_STACKS_III;
            case 4 -> MAX_STACKS_IV;
            default -> MAX_STACKS_V;
        };
        // Levels beyond 5: keep the level-5 cap (only percentages scale, per spec).
        return base;
    }

    public static double getPerStackBonus(int enchantmentLevel) {
        double base = switch (enchantmentLevel) {
            case 1 -> PER_STACK_I;
            case 2 -> PER_STACK_II;
            case 3 -> PER_STACK_III;
            case 4 -> PER_STACK_IV;
            default -> PER_STACK_V;
        };
        if (enchantmentLevel > 5) {
            base += (enchantmentLevel - 5) * 0.01;
        }
        return base;
    }

    public static double getWeaponDamagePercent(int enchantmentLevel) {
        double base = switch (enchantmentLevel) {
            case 1 -> 0.05;
            case 2 -> 0.07;
            case 3 -> 0.10;
            case 4 -> 0.12;
            default -> 0.15;
        };
        if (enchantmentLevel > 5) {
            base += (enchantmentLevel - 5) * 0.01;
        }
        return base;
    }

    public static double getStatDamagePercent(int enchantmentLevel) {
        double base = switch (enchantmentLevel) {
            case 1 -> 0.10;
            case 2 -> 0.12;
            case 3 -> 0.17;
            case 4 -> 0.20;
            default -> 0.25;
        };
        if (enchantmentLevel > 5) {
            base += (enchantmentLevel - 5) * 0.01;
        }
        return base;
    }
}
