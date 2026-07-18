package akumiyuukii.mods.client;

import akumiyuukii.mods.Element;
import akumiyuukii.mods.ElementNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import io.netty.buffer.Unpooled;

/**
 * Element selection screen.
 *  - First time (forced): cannot be closed until an element is chosen.
 *  - Re-choosing later: allowed only if the player has >= 100 stat points (costs 100).
 */
public class ElementSelectScreen extends Screen {

    private final boolean forced;
    private Element hovered = null;

    public ElementSelectScreen(boolean forced) {
        super(Component.literal("Chọn nguyên tố"));
        this.forced = forced;
    }

    @Override
    protected void init() {
        super.init();
        Element[] elements = Element.values();
        int cols = 3;
        int cellW = 140;
        int cellH = 28;
        int gapX = 10;
        int gapY = 34;
        int totalW = cols * cellW + (cols - 1) * gapX;
        int startX = this.width / 2 - totalW / 2;
        int startY = this.height / 2 - 40;

        boolean canReChoose = ElementClient.hasChosen && ElementClient.points >= 100;

        for (int i = 0; i < elements.length; i++) {
            final Element e = elements[i];
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * (cellW + gapX);
            int y = startY + row * gapY;

            boolean enabled = !ElementClient.hasChosen || canReChoose;
            Button btn = Button.builder(
                    Component.literal(e.symbol + " " + e.displayName),
                    b -> choose(e)
            ).bounds(x, y, cellW, cellH).build();
            btn.active = enabled;
            addRenderableWidget(btn);
        }

        // Close button only when not forced.
        if (!forced) {
            addRenderableWidget(Button.builder(
                    Component.literal("Đóng"),
                    b -> this.onClose()
            ).bounds(this.width / 2 - 60, startY + 3 * gapY + 20, 120, 20).build());
        }
    }

    private void choose(Element e) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(e.ordinal());
        ClientPlayNetworking.send(ElementNetworking.SELECT_ID, buf);
        // Server will sync back; close after sending (unless forced first choice — the sync will
        // flip hasChosen and we can safely close here too).
        this.minecraft.setScreen(null);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        int cx = this.width / 2;
        g.drawCenteredString(this.font, Component.literal("=== CHỌN NGUYÊN TỐ ==="), cx, this.height / 2 - 80, 0xFFDD55);

        if (ElementClient.hasChosen) {
            if (ElementClient.points >= 100) {
                g.drawCenteredString(this.font,
                        Component.literal("Đổi nguyên tố sẽ tốn 100 điểm (bạn có " + ElementClient.points + ")"),
                        cx, this.height / 2 - 66, 0xFFAAAAAA);
            } else {
                g.drawCenteredString(this.font,
                        Component.literal("Cần 100 điểm để đổi (bạn có " + ElementClient.points + ")"),
                        cx, this.height / 2 - 66, 0xFFFF5555);
            }
        } else {
            g.drawCenteredString(this.font,
                    Component.literal("Chọn 1 nguyên tố để bắt đầu (không thể đổi khi chưa đủ 100 điểm)"),
                    cx, this.height / 2 - 66, 0xFFAAAAAA);
        }

        super.render(g, mouseX, mouseY, partialTick);

        // Perk description of the hovered element.
        Element hover = elementAt(mouseX, mouseY);
        if (hover != null) {
            int boxTop = this.height / 2 + 70;
            g.fill(cx - 180, boxTop, cx + 180, boxTop + 34, 0xCC000000);
            g.drawCenteredString(this.font, Component.literal(hover.symbol + " " + hover.displayName),
                    cx, boxTop + 4, hover.color);
            g.drawCenteredString(this.font, Component.literal(hover.perk), cx, boxTop + 18, 0xFFCCCCCC);
        }
    }

    private Element elementAt(int mouseX, int mouseY) {
        Element[] elements = Element.values();
        int cols = 3;
        int cellW = 140, cellH = 28, gapX = 10, gapY = 34;
        int totalW = cols * cellW + (cols - 1) * gapX;
        int startX = this.width / 2 - totalW / 2;
        int startY = this.height / 2 - 40;
        for (int i = 0; i < elements.length; i++) {
            int col = i % cols, row = i / cols;
            int x = startX + col * (cellW + gapX);
            int y = startY + row * gapY;
            if (mouseX >= x && mouseX <= x + cellW && mouseY >= y && mouseY <= y + cellH) {
                return elements[i];
            }
        }
        return null;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // Cannot escape the forced first-time choice.
        return !forced || ElementClient.hasChosen;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
