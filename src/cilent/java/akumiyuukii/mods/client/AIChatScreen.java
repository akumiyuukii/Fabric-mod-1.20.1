package akumiyuukii.mods.client;

import akumiyuukii.mods.FabricAIBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

public class AIChatScreen extends Screen {
    private EditBox inputField;
    private Button sendButton;
    private final List<String> chatHistory = new ArrayList<>();
    private boolean isLoading = false;
    private static final int HISTORY_START_Y = 30;
    private static final int LINE_HEIGHT = 12;

    protected AIChatScreen() {
        super(Component.literal("AI Chat"));
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;

        inputField = new EditBox(this.font, centerX - 150, this.height - 40, 300, 20, Component.literal("Chat với AI..."));
        inputField.setMaxLength(256);
        inputField.setFocused(true);
        addRenderableWidget(inputField);

        sendButton = Button.builder(
                Component.literal("Gửi"),
                btn -> sendMessage()
        ).bounds(centerX + 155, this.height - 40, 40, 20).build();
        addRenderableWidget(sendButton);

        // Settings button
        addRenderableWidget(Button.builder(
                Component.literal("Settings"),
                btn -> Minecraft.getInstance().setScreen(new AISettingsScreen())
        ).bounds(centerX - 150, this.height - 65, 100, 20).build());

        if (chatHistory.isEmpty()) {
            chatHistory.add("§a=== AI Chat ===");
            chatHistory.add("§7Nhấn Settings để cấu hình API và model.");
            chatHistory.add("§7Model hiện tại: " + AISettingsScreen.model);
        }
    }

    private void sendMessage() {
        String message = inputField.getValue().trim();
        if (message.isEmpty() || isLoading) return;

        chatHistory.add("§eBạn: " + message);
        inputField.setValue("");
        isLoading = true;
        chatHistory.add("§7AI đang trả lời...");

        FabricAIBridge.askAI(message, AISettingsScreen.model, 500).thenAccept(response -> {
            Minecraft.getInstance().execute(() -> {
                chatHistory.remove(chatHistory.size() - 1);
                chatHistory.add("§bAI: " + response);
                isLoading = false;
            });
        });
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            sendMessage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, Component.literal("=== AI Chat ==="), this.width / 2, 10, 0x55FF55);

        int y = HISTORY_START_Y;
        int maxLines = (this.height - HISTORY_START_Y - 80) / LINE_HEIGHT;
        int startIndex = Math.max(0, chatHistory.size() - maxLines);

        for (int i = startIndex; i < chatHistory.size(); i++) {
            String line = chatHistory.get(i);
            MutableComponent component = Component.literal("");
            String[] parts = line.split("§");
            boolean first = true;
            for (String part : parts) {
                if (first) {
                    component.append(Component.literal(part));
                    first = false;
                } else if (part.length() >= 1) {
                    char colorCode = part.charAt(0);
                    int color = parseColorCode(colorCode);
                    String text = part.substring(1);
                    component.append(Component.literal(text).setStyle(Style.EMPTY.withColor(color)));
                }
            }
            guiGraphics.drawString(this.font, component, 10, y, 0xFFFFFF, false);
            y += LINE_HEIGHT;
        }

        guiGraphics.drawString(this.font, Component.literal("API: " + AISettingsScreen.apiUrl), 10, this.height - 15, 0x888888);
    }

    private int parseColorCode(char code) {
        return switch (code) {
            case '0' -> 0x000000;
            case '1' -> 0x0000AA;
            case '2' -> 0x00AA00;
            case '3' -> 0x00AAAA;
            case '4' -> 0xAA0000;
            case '5' -> 0xAA00AA;
            case '6' -> 0xFFAA00;
            case '7' -> 0xAAAAAA;
            case '8' -> 0x555555;
            case '9' -> 0x5555FF;
            case 'a' -> 0x55FF55;
            case 'b' -> 0x55FFFF;
            case 'c' -> 0xFF5555;
            case 'd' -> 0xFF55FF;
            case 'e' -> 0xFFFF55;
            case 'f' -> 0xFFFFFF;
            default -> 0xFFFFFF;
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}