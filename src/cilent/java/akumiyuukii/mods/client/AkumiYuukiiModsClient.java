package akumiyuukii.mods.client;

import akumiyuukii.mods.client.AISettingsScreen;
import akumiyuukii.mods.client.entity.AkumiMobRenderer;
import akumiyuukii.mods.entity.ModEntities;
import akumiyuukii.mods.BonusStackSync;
import akumiyuukii.mods.DamageCounter;
import akumiyuukii.mods.ElementNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class AkumiYuukiiModsClient implements ClientModInitializer {
    public static KeyMapping openStatsKey;
    public static KeyMapping openAIKey;

    @Override
    public void onInitializeClient() {
        openStatsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.akumiyuukiimods.open_stats",
                GLFW.GLFW_KEY_G,
                "category.akumiyuukiimods"
        ));

        openAIKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.akumiyuukiimods.open_ai_chat",
                GLFW.GLFW_KEY_H,
                "category.akumiyuukiimods"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openStatsKey.consumeClick()) {
                if (client.player != null) {
                    Minecraft.getInstance().setScreen(new StatScreen(client.player));
                }
            }
            while (openAIKey.consumeClick()) {
                if (client.player != null) {
                    Minecraft.getInstance().setScreen(new AIChatScreen());
                }
            }
        });

        // Register the humanoid renderer for every custom mob.
        for (var type : ModEntities.TYPES.values()) {
            EntityRendererRegistry.register(type, AkumiMobRenderer::new);
        }

        // Register damage HUD overlay
        HudRenderCallback.EVENT.register(new DamageHudOverlay());

        // Register packet receiver for damage counter from server
        ClientPlayNetworking.registerGlobalReceiver(DamageCounter.DAMAGE_PACKET_ID,
                (client, handler, buf, responseSender) -> {
                    float damage = buf.readFloat();
                    int element = buf.readInt();
                    client.execute(() -> {
                        DamageCounter.addDamage(damage, element);
                    });
                });

        // Receive bonus-damage stack info for the info tab.
        ClientPlayNetworking.registerGlobalReceiver(BonusStackSync.PACKET_ID,
                (client, handler, buf, responseSender) -> {
                    int stacks = buf.readInt();
                    int maxStacks = buf.readInt();
                    int level = buf.readInt();
                    double weaponPercent = buf.readDouble();
                    double statPercent = buf.readDouble();
                    double perStack = buf.readDouble();
                    client.execute(() -> BonusStackClient.update(
                            stacks, maxStacks, level, weaponPercent, statPercent, perStack));
                });

        // Receive element state; force the selection screen on first join.
        ClientPlayNetworking.registerGlobalReceiver(ElementNetworking.SYNC_ID,
                (client, handler, buf, responseSender) -> {
                    int ordinal = buf.readInt();
                    boolean hasChosen = buf.readBoolean();
                    int points = buf.readInt();
                    client.execute(() -> {
                        ElementClient.update(ordinal, hasChosen, points);
                        if (!hasChosen && client.player != null) {
                            client.setScreen(new ElementSelectScreen(true));
                        }
                    });
                });
    }
}



