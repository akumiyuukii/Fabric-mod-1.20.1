package akumiyuukii.mods;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server sends each confirmed hit's actual damage + element to the client, which accumulates it
 * into a combo (HSR-style) for the HUD overlay.
 */
public class DamageCounter {
    public static final ResourceLocation DAMAGE_PACKET_ID = new ResourceLocation(AkumiYuukiiMods.MOD_ID, "damage_packet");

    private static float totalDamage = 0;
    private static int comboCount = 0;
    private static int elementOrdinal = 0;
    private static long lastHitTime = 0;
    // Client-side: timestamp of the latest single hit, used for the number "pop" animation.
    private static long lastPopTime = 0;
    private static float lastHitDamage = 0;

    private static final long DISPLAY_DURATION_MS = 5000; // disappears 5s after the last hit
    private static final long FADE_START_MS = 4000;       // fade over the final 1s

    /** Client-side: accumulate a hit into the current combo. */
    public static void addDamage(float damage, int element) {
        long now = System.currentTimeMillis();
        if (now - lastHitTime > DISPLAY_DURATION_MS) {
            totalDamage = 0;
            comboCount = 0;
        }
        totalDamage += damage;
        comboCount++;
        elementOrdinal = element;
        lastHitDamage = damage;
        lastHitTime = now;
        lastPopTime = now;
    }

    public static float getTotalDamage() {
        long now = System.currentTimeMillis();
        if (now - lastHitTime > DISPLAY_DURATION_MS) {
            totalDamage = 0;
            comboCount = 0;
            return 0;
        }
        return totalDamage;
    }

    public static int getComboCount() {
        getTotalDamage(); // trigger expiry
        return comboCount;
    }

    public static int getElementOrdinal() {
        return elementOrdinal;
    }

    public static float getLastHitDamage() {
        return lastHitDamage;
    }

    public static long getLastPopTime() {
        return lastPopTime;
    }

    public static long getLastHitTime() {
        return lastHitTime;
    }

    public static long getDisplayDurationMs() {
        return DISPLAY_DURATION_MS;
    }

    public static float getAlpha() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastHitTime;
        if (elapsed >= DISPLAY_DURATION_MS) return 0;
        if (elapsed > FADE_START_MS) {
            return 1.0f - (float)(elapsed - FADE_START_MS) / (float)(DISPLAY_DURATION_MS - FADE_START_MS);
        }
        return 1.0f;
    }

    /** Server -> client. */
    public static void sendDamagePacket(ServerPlayer player, float damage, int elementOrdinal) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeFloat(damage);
        buf.writeInt(elementOrdinal);
        ServerPlayNetworking.send(player, DAMAGE_PACKET_ID, buf);
    }
}
