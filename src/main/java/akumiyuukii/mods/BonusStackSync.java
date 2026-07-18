package akumiyuukii.mods;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sends the current bonus-damage stack info from server to client so the info tab
 * can display live values (stack count, cap, enchant level, and percentages).
 */
public class BonusStackSync {
    public static final ResourceLocation PACKET_ID = new ResourceLocation(AkumiYuukiiMods.MOD_ID, "bonus_stack_sync");

    public static void send(ServerPlayer player, int stacks, int maxStacks, int level,
                            double weaponPercent, double statPercent, double perStack) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeInt(stacks);
        buf.writeInt(maxStacks);
        buf.writeInt(level);
        buf.writeDouble(weaponPercent);
        buf.writeDouble(statPercent);
        buf.writeDouble(perStack);
        ServerPlayNetworking.send(player, PACKET_ID, buf);
    }
}
