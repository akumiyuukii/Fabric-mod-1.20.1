package akumiyuukii.mods.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Info tab for the "Tăng sát thương kèm theo" enchantment: shows the current stack count,
 * the enchant level, the per-stack bonus, the cap, and a description.
 */
public class StackInfoScreen extends Screen {

    private final Screen parent;

    public StackInfoScreen(Screen parent) {
        super(Component.literal("Tăng sát thương - Số tầng"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        addRenderableWidget(Button.builder(
                Component.literal("Đóng"),
                btn -> this.onClose()
        ).bounds(centerX - 60, centerY + 95, 120, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int left = centerX - 150;
        int right = centerX + 150;
        g.fill(left, centerY - 110, right, centerY + 106, 0xCC000000);

        int level = BonusStackClient.level;
        int stacks = BonusStackClient.currentStacks();
        int maxStacks = BonusStackClient.maxStacks;
        double perStack = BonusStackClient.perStack;
        double weaponPercent = BonusStackClient.weaponPercent;
        double statPercent = BonusStackClient.statPercent;

        String roman = switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> level > 5 ? "V+" + (level - 5) : "-";
        };

        int y = centerY - 100;
        g.drawCenteredString(this.font, Component.literal("=== Tăng sát thương kèm theo ==="), centerX, y, 0x55FF55);
        y += 20;

        if (level <= 0) {
            g.drawCenteredString(this.font,
                    Component.literal("Chưa đánh với vũ khí có enchant này."), centerX, y + 20, 0xAAAAAA);
            g.drawCenteredString(this.font,
                    Component.literal("Cầm vũ khí có enchant và đánh 1 phát để xem."), centerX, y + 34, 0x888888);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }

        // Enchant level
        g.drawString(this.font, Component.literal("Cấp enchant: " + roman + " (Lv." + level + ")"), left + 12, y, 0xFFFF55);
        y += 16;

        // Stack count with a visual bar
        g.drawString(this.font, Component.literal("Số tầng: " + stacks + " / " + maxStacks), left + 12, y, 0xFFFFFF);
        y += 12;
        int barLeft = left + 12;
        int barRight = right - 12;
        int barW = barRight - barLeft;
        g.fill(barLeft, y, barRight, y + 8, 0xFF333333);
        if (maxStacks > 0 && stacks > 0) {
            int filled = (int) (barW * ((double) stacks / maxStacks));
            g.fill(barLeft, y, barLeft + filled, y + 8, 0xFFFF4444);
        }
        y += 18;

        // Time remaining before the stacks expire.
        long remainMs = stacks > 0 ? BonusStackClient.remainingMs() : 0;
        long totalSec = remainMs / 1000;
        String timeStr = stacks > 0
                ? String.format("%d:%02d", totalSec / 60, totalSec % 60)
                : "--:--";
        int timeColor = remainMs < 30_000 && stacks > 0 ? 0xFFFF5555 : 0xFF55FF55;
        g.drawString(this.font, Component.literal("Thời gian còn lại: " + timeStr), left + 12, y, timeColor);
        y += 16;

        // Per-stack bonus + total from stacks
        double stackTotal = stacks * perStack;
        g.drawString(this.font, Component.literal(
                String.format("Mỗi tầng: +%.1f%% sát thương", perStack * 100)), left + 12, y, 0xCCCCCC);
        y += 14;
        g.drawString(this.font, Component.literal(
                String.format("Tổng từ tầng: +%.1f%%", stackTotal * 100)), left + 12, y, 0xCCCCCC);
        y += 18;

        // Base bonus percentages
        g.drawString(this.font, Component.literal(
                String.format("Sát thương kiếm: +%.0f%%", weaponPercent * 100)), left + 12, y, 0x88CCFF);
        y += 14;
        g.drawString(this.font, Component.literal(
                String.format("Sát thương từ stat: +%.0f%%", statPercent * 100)), left + 12, y, 0x88CCFF);
        y += 18;

        // Description
        g.drawString(this.font, Component.literal("Mô tả:"), left + 12, y, 0xFFAA00);
        y += 12;
        g.drawString(this.font, Component.literal(
                "Mỗi đòn đánh cộng thêm sát thương và +1 tầng."), left + 12, y, 0x999999);
        y += 11;
        g.drawString(this.font, Component.literal(
                "Tầng reset sau 5 phút không đánh."), left + 12, y, 0x999999);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
