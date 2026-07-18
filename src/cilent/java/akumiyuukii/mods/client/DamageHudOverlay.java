package akumiyuukii.mods.client;

import akumiyuukii.mods.DamageCounter;
import akumiyuukii.mods.Element;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Honkai: Star Rail-style damage readout in the top-right corner:
 *  - element symbol + name header
 *  - large total damage number with a "pop" scale on each hit
 *  - combo counter with a decaying bar
 * Colour follows the player's element.
 */
public class DamageHudOverlay implements HudRenderCallback {

    private static final int MARGIN = 8;

    @Override
    public void onHudRender(GuiGraphics g, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float total = DamageCounter.getTotalDamage();
        float alpha = DamageCounter.getAlpha();
        if (total <= 0 || alpha <= 0) return;

        int combo = DamageCounter.getComboCount();
        Element element = Element.byOrdinal(DamageCounter.getElementOrdinal());
        int baseColor = element.color & 0x00FFFFFF;
        int a = (int) (alpha * 255) & 0xFF;

        int screenW = mc.getWindow().getGuiScaledWidth();

        // Layout — a touch smaller/tighter than before (box was 132x54, now 108x44).
        int boxW = 108;
        int boxH = 44;
        int boxRight = screenW - MARGIN;
        int boxLeft = boxRight - boxW;
        int boxTop = MARGIN;

        // Header text: element symbol + name
        String header = element.symbol + " " + element.displayName.toUpperCase();
        String totalStr = formatNumber(total);
        String comboStr = combo > 1 ? ("x" + combo + " COMBO") : "HIT";

        // Background panel — slightly darker for stronger contrast behind the bolder text.
        int panelA = (int) (alpha * 170) << 24;
        g.fill(boxLeft - 3, boxTop - 2, boxRight + 2, boxTop + boxH, panelA);
        // Left accent bar in element colour
        g.fill(boxLeft - 3, boxTop - 2, boxLeft - 1, boxTop + boxH, (a << 24) | baseColor);

        // Header (bold via double-draw).
        drawBold(g, mc, header, boxLeft + 2, boxTop + 1, (a << 24) | baseColor);

        // Big damage number with pop-scale animation.
        long sincePop = System.currentTimeMillis() - DamageCounter.getLastPopTime();
        float pop = 1.0f;
        if (sincePop < 180) {
            pop = 1.30f - 0.30f * (sincePop / 180.0f); // 1.30 -> 1.0
        }

        int numColor = (a << 24) | 0x00FFFFFF; // white number
        drawScaledBold(g, mc, totalStr, boxLeft + 2, boxTop + 13, pop, numColor);

        // Combo label (bold).
        drawBold(g, mc, comboStr, boxLeft + 2, boxTop + 33, (a << 24) | baseColor);

        // Combo decay bar (time remaining before reset).
        long elapsed = System.currentTimeMillis() - DamageCounter.getLastHitTime();
        float remaining = 1.0f - Math.min(1.0f, (float) elapsed / DamageCounter.getDisplayDurationMs());
        int barTop = boxTop + boxH - 3;
        int barLeft = boxLeft + 2;
        int barRight = boxRight - 2;
        int barW = barRight - barLeft;
        g.fill(barLeft, barTop, barRight, barTop + 2, (a << 24));
        g.fill(barLeft, barTop, barLeft + (int) (barW * remaining), barTop + 2, (a << 24) | baseColor);
    }

    /** Draws text with a shadow, then again offset by 1px to fake a bold weight. */
    private void drawBold(GuiGraphics g, Minecraft mc, String text, int x, int y, int color) {
        g.drawString(mc.font, Component.literal(text), x, y, color, true);
        g.drawString(mc.font, Component.literal(text), x + 1, y, color, false);
    }

    private void drawScaledBold(GuiGraphics g, Minecraft mc, String text, int x, int y, float scale, int color) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1.0f);
        RenderSystem.enableBlend();
        // Shadowed + 1px-offset second pass => bolder, crisper number.
        g.drawString(mc.font, Component.literal(text), 0, 0, color, true);
        g.drawString(mc.font, Component.literal(text), 1, 0, color, false);
        pose.popPose();
    }

    private static String formatNumber(float value) {
        long v = (long) value;
        // Thousands separators, HSR-like.
        return String.format("%,d", v);
    }
}
