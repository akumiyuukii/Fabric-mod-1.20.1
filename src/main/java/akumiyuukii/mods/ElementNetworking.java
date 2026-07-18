package akumiyuukii.mods;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Networking for the element system.
 *  - SYNC_ID:   server -> client, current element + whether the player has chosen yet.
 *  - SELECT_ID: client -> server, the player's chosen element ordinal.
 */
public class ElementNetworking {
    public static final ResourceLocation SYNC_ID = new ResourceLocation(AkumiYuukiiMods.MOD_ID, "element_sync");
    public static final ResourceLocation SELECT_ID = new ResourceLocation(AkumiYuukiiMods.MOD_ID, "element_select");

    /** Server -> client: push the authoritative element state. */
    public static void sync(ServerPlayer player, PlayerStats stats) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeInt(stats.getElement().ordinal());
        buf.writeBoolean(stats.hasChosenElement());
        buf.writeInt(stats.getPoints());
        ServerPlayNetworking.send(player, SYNC_ID, buf);
    }
}
