package akumiyuukii.mods.client;

import akumiyuukii.mods.PlayerStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class StatScreen extends Screen {
    private final Player player;
    private PlayerStats stats;

    protected StatScreen(Player player) {
        super(Component.literal("Player Stats"));
        this.player = player;
        this.stats = PlayerStats.get(player);
    }

    @Override
    protected void init() {
        super.init();
        stats = PlayerStats.get(player);
        refreshButtons();
    }

    private void refreshButtons() {
        this.clearWidgets();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int startX = centerX - 100;
        int startY = centerY - 80;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 24;

        // HP button
        addRenderableWidget(Button.builder(
                Component.literal(String.format("HP: %.0f (Lv.%d) [Cost: %d]", stats.getMaxHp(), stats.getHpLevel(), stats.getHpCost())),
                btn -> {
                    if (stats.upgradeHp(player)) {
                        refreshButtons();
                    }
                }
        ).bounds(startX, startY, buttonWidth, buttonHeight).build());

        // Attack button
        addRenderableWidget(Button.builder(
                Component.literal(String.format("Attack: %.1f (Lv.%d) [Cost: %d]", stats.getAttackDamage(), stats.getAttackLevel(), stats.getAttackCost())),
                btn -> {
                    if (stats.upgradeAttack(player)) {
                        refreshButtons();
                    }
                }
        ).bounds(startX, startY + spacing, buttonWidth, buttonHeight).build());

        // Defense button
        addRenderableWidget(Button.builder(
                Component.literal(String.format("Defense: %.0f (Lv.%d) [Cost: %d]", stats.getDefense(), stats.getDefenseLevel(), stats.getDefenseCost())),
                btn -> {
                    if (stats.upgradeDefense(player)) {
                        refreshButtons();
                    }
                }
        ).bounds(startX, startY + spacing * 2, buttonWidth, buttonHeight).build());

        // Reset stats button (refunds all spent points, costs 5 points)
        addRenderableWidget(Button.builder(
                Component.literal("Reset stats (tốn 5đ)"),
                btn -> {
                    if (stats.resetStats()) {
                        refreshButtons();
                    }
                }
        ).bounds(startX, startY + spacing * 3 + 6, buttonWidth, buttonHeight).build());

        // Bonus damage stack info tab
        addRenderableWidget(Button.builder(
                Component.literal("Tầng sát thương »"),
                btn -> this.minecraft.setScreen(new StackInfoScreen(this))
        ).bounds(startX, startY + spacing * 4 + 6, buttonWidth, buttonHeight).build());

        // Element selection / re-choose (costs 100 points)
        addRenderableWidget(Button.builder(
                Component.literal("Nguyên tố: " + ElementClient.element.displayName + " (đổi: 100đ)"),
                btn -> this.minecraft.setScreen(new ElementSelectScreen(false))
        ).bounds(startX, startY + spacing * 5 + 6, buttonWidth, buttonHeight).build());

        // Close button
        addRenderableWidget(Button.builder(
                Component.literal("Close"),
                btn -> this.onClose()
        ).bounds(startX, startY + spacing * 6 + 6, buttonWidth, buttonHeight).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Draw panel background
        guiGraphics.fill(centerX - 120, centerY - 100, centerX + 120, centerY + 118, 0xCC000000);

        // Draw title
        guiGraphics.drawCenteredString(this.font, Component.literal("=== Player Stats ==="), centerX, centerY - 90, 0xFFFFFF);

        // Draw points
        guiGraphics.drawCenteredString(this.font, Component.literal("Points: " + stats.getPoints()), centerX, centerY - 70, 0xFFFF55);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}