package akumiyuukii.mods;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStats {
    private static final String NBT_KEY = "AkumiYuukiiStats";
    private static final ConcurrentHashMap<UUID, PlayerStats> CACHE = new ConcurrentHashMap<>();

    private int hpLevel = 0;
    private int attackLevel = 0;
    private int defenseLevel = 0;
    private int points = 0;

    // Bonus damage stacks (from the "Tăng sát thương kèm theo" enchantment).
    // Stored in player data, not a MobEffect. Expire 5 minutes after the last hit.
    // While active, they buff ALL damage the player deals (not just the enchanted weapon).
    private int bonusStacks = 0;
    private long bonusStacksExpireTick = 0;
    private int bonusStackLevel = 0; // enchant level that produced the current stacks

    // Chosen damage element (HSR-style). All damage the player deals is of this element.
    private int elementOrdinal = 0;       // defaults to PHYSICAL
    private boolean hasChosenElement = false;

    // Base values
    public static final double BASE_HP = 20.0;
    public static final double BASE_ATTACK = 1.0;
    public static final double BASE_DEFENSE = 0.0;

    // Increment per level
    public static final double HP_PER_LEVEL = 2.0;
    public static final double ATTACK_PER_LEVEL = 0.5;
    public static final double DEFENSE_PER_LEVEL = 1.0;

    // Cost per level (increases with each level)
    public static final int BASE_POINT_COST = 1;

    public PlayerStats() {}

    public static PlayerStats get(Player player) {
        return CACHE.computeIfAbsent(player.getUUID(), k -> new PlayerStats());
    }

    public static void loadFromNbt(UUID uuid, CompoundTag tag) {
        PlayerStats stats = new PlayerStats();
        if (tag.contains(NBT_KEY)) {
            CompoundTag data = tag.getCompound(NBT_KEY);
            stats.hpLevel = data.getInt("hpLevel");
            stats.attackLevel = data.getInt("attackLevel");
            stats.defenseLevel = data.getInt("defenseLevel");
            stats.points = data.getInt("points");
            stats.bonusStacks = data.getInt("bonusStacks");
            stats.bonusStacksExpireTick = data.getLong("bonusStacksExpireTick");
            stats.bonusStackLevel = data.getInt("bonusStackLevel");
            stats.elementOrdinal = data.getInt("elementOrdinal");
            stats.hasChosenElement = data.getBoolean("hasChosenElement");
        }
        CACHE.put(uuid, stats);
    }

    public static CompoundTag saveToNbt(UUID uuid) {
        PlayerStats stats = CACHE.get(uuid);
        if (stats == null) return new CompoundTag();
        CompoundTag tag = new CompoundTag();
        CompoundTag data = new CompoundTag();
        data.putInt("hpLevel", stats.hpLevel);
        data.putInt("attackLevel", stats.attackLevel);
        data.putInt("defenseLevel", stats.defenseLevel);
        data.putInt("points", stats.points);
        data.putInt("bonusStacks", stats.bonusStacks);
        data.putLong("bonusStacksExpireTick", stats.bonusStacksExpireTick);
        data.putInt("bonusStackLevel", stats.bonusStackLevel);
        data.putInt("elementOrdinal", stats.elementOrdinal);
        data.putBoolean("hasChosenElement", stats.hasChosenElement);
        tag.put(NBT_KEY, data);
        return tag;
    }

    public static void remove(UUID uuid) {
        CACHE.remove(uuid);
    }

    public int getPointCost(int level) {
        return BASE_POINT_COST + level;
    }

    public boolean upgradeHp(Player player) {
        int cost = getPointCost(hpLevel);
        if (points >= cost) {
            points -= cost;
            hpLevel++;
            return true;
        }
        return false;
    }

    public boolean upgradeAttack(Player player) {
        int cost = getPointCost(attackLevel);
        if (points >= cost) {
            points -= cost;
            attackLevel++;
            return true;
        }
        return false;
    }

    public boolean upgradeDefense(Player player) {
        int cost = getPointCost(defenseLevel);
        if (points >= cost) {
            points -= cost;
            defenseLevel++;
            return true;
        }
        return false;
    }

    public void addPoints(int amount) {
        this.points += amount;
    }

    /** Total points that were spent to reach the given level (sum of per-level costs). */
    private int refundForLevel(int level) {
        int total = 0;
        for (int i = 0; i < level; i++) {
            total += getPointCost(i);
        }
        return total;
    }

    /**
     * Refunds all points spent on stats and resets HP/Attack/Defense to level 0.
     * Costs 5 points to perform. Returns true on success (needs >= 5 points).
     */
    public boolean resetStats() {
        if (points < 5) return false;
        int refund = refundForLevel(hpLevel) + refundForLevel(attackLevel) + refundForLevel(defenseLevel);
        hpLevel = 0;
        attackLevel = 0;
        defenseLevel = 0;
        points += refund;
        points -= 5;
        return true;
    }

    // ===== Element =====

    public Element getElement() {
        return Element.byOrdinal(elementOrdinal);
    }

    public void setElement(Element element) {
        this.elementOrdinal = element.ordinal();
    }

    public boolean hasChosenElement() {
        return hasChosenElement;
    }

    public void markElementChosen() {
        this.hasChosenElement = true;
    }

    // ===== Bonus damage stacks =====

    /**
     * Current number of bonus-damage stacks, accounting for expiry.
     * @param currentTick the current game time in ticks (level.getGameTime())
     */
    public int getBonusStacks(long currentTick) {
        if (currentTick >= bonusStacksExpireTick) {
            bonusStacks = 0;
            bonusStackLevel = 0;
        }
        return bonusStacks;
    }

    /** Enchant level that produced the current (non-expired) stacks; 0 if none. */
    public int getBonusStackLevel(long currentTick) {
        getBonusStacks(currentTick); // trigger expiry cleanup
        return bonusStackLevel;
    }

    /**
     * The multiplier applied to ALL damage the player deals while stacks are active.
     * = 1 + stacks * perStackBonus(level). Returns 1.0 when there are no active stacks.
     */
    public double getBonusStackMultiplier(long currentTick) {
        int stacks = getBonusStacks(currentTick);
        if (stacks <= 0 || bonusStackLevel <= 0) return 1.0;
        return 1.0 + stacks * BonusDamageConfig.getPerStackBonus(bonusStackLevel);
    }

    /**
     * Adds one bonus-damage stack (capped at maxStacks) and refreshes the 5-minute timer.
     * Called on every hit with the enchantment equipped; records the enchant level so the
     * stacks keep buffing all damage even after the weapon is swapped out.
     * @return the new stack count after adding.
     */
    public int addBonusStack(int maxStacks, int enchantLevel, long currentTick) {
        int current = getBonusStacks(currentTick);
        bonusStacks = Math.min(current + 1, maxStacks);
        bonusStacksExpireTick = currentTick + BonusDamageConfig.DURATION_TICKS;
        bonusStackLevel = enchantLevel;
        return bonusStacks;
    }

    // Getters
    public int getHpLevel() { return hpLevel; }
    public int getAttackLevel() { return attackLevel; }
    public int getDefenseLevel() { return defenseLevel; }
    public int getPoints() { return points; }

    // Calculated values
    public double getMaxHp() { return BASE_HP + hpLevel * HP_PER_LEVEL; }
    public double getAttackDamage() { return BASE_ATTACK + attackLevel * ATTACK_PER_LEVEL; }
    public double getDefense() { return BASE_DEFENSE + defenseLevel * DEFENSE_PER_LEVEL; }

    public int getHpCost() { return getPointCost(hpLevel); }
    public int getAttackCost() { return getPointCost(attackLevel); }
    public int getDefenseCost() { return getPointCost(defenseLevel); }
}