package akumiyuukii.mods;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerFragments {
    private static final String NBT_KEY = "AkumiYuukiiFragments";
    private static final ConcurrentHashMap<UUID, PlayerFragments> CACHE = new ConcurrentHashMap<>();

    private int fragments = 0;

    public PlayerFragments() {}

    public static PlayerFragments get(Player player) {
        return CACHE.computeIfAbsent(player.getUUID(), k -> new PlayerFragments());
    }

    public static void loadFromNbt(UUID uuid, CompoundTag tag) {
        PlayerFragments fragments = new PlayerFragments();
        if (tag.contains(NBT_KEY)) {
            fragments.fragments = tag.getInt(NBT_KEY);
        }
        CACHE.put(uuid, fragments);
    }

    public static CompoundTag saveToNbt(UUID uuid) {
        PlayerFragments fragments = CACHE.get(uuid);
        if (fragments == null) return new CompoundTag();
        CompoundTag tag = new CompoundTag();
        tag.putInt(NBT_KEY, fragments.fragments);
        return tag;
    }

    public static void remove(UUID uuid) {
        CACHE.remove(uuid);
    }

    public void addFragment(int amount) {
        this.fragments += amount;
    }

    public boolean spendFragments(int amount) {
        if (fragments >= amount) {
            fragments -= amount;
            return true;
        }
        return false;
    }

    public int getFragments() {
        return fragments;
    }

    public int getPoints() {
        return fragments / 10;
    }

    public int getRemainingFragments() {
        return fragments % 10;
    }

    public boolean convertToPoint() {
        if (fragments >= 10) {
            fragments -= 10;
            return true;
        }
        return false;
    }
}