package akumiyuukii.mods.client;

import akumiyuukii.mods.FabricAIBridge;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class AISettingsScreen extends Screen {
    private EditBox apiUrlField;
    private EditBox modelField;
    public static String apiUrl = "http://localhost:3000/api/components/chat/v1";
    public static String model = "enter the model name here...";
    private static final Path CONFIG_PATH = Minecraft.getInstance().gameDirectory.toPath().resolve("config/akumiyuukiimods_ai.json");

    private final List<String> logLines = new ArrayList<>();
    private boolean connecting = false;
    private boolean scanning = false;
    private final List<String> availableModels = new ArrayList<>();
    private String connectStatus = "";

    // Button refs so we can disable + relabel them while a background request is running.
    private Button connectButton;
    private Button scanButton;

    protected AISettingsScreen() {
        super(Component.literal("AI Settings"));
    }

    // Layout constants shared by init() and render().
    private static final int API_LABEL_Y = 40;
    private static final int API_FIELD_Y = 52;
    private static final int MODEL_LABEL_Y = 82;
    private static final int MODEL_FIELD_Y = 94;
    private static final int BUTTON_ROW_Y = 124;
    private static final int STATUS_Y = 150;
    private static final int MODELS_Y = 166;

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;

        apiUrlField = new EditBox(this.font, centerX - 150, API_FIELD_Y, 300, 20, Component.literal("API URL"));
        apiUrlField.setMaxLength(256);
        apiUrlField.setValue(apiUrl);
        addRenderableWidget(apiUrlField);

        modelField = new EditBox(this.font, centerX - 150, MODEL_FIELD_Y, 300, 20, Component.literal("Model"));
        modelField.setMaxLength(64);
        modelField.setValue(model);
        addRenderableWidget(modelField);

        // Action buttons row
        connectButton = Button.builder(
                Component.literal(connecting ? "Đang..." : "Connect"),
                btn -> testConnection()
        ).bounds(centerX - 150, BUTTON_ROW_Y, 95, 20).build();
        connectButton.active = !connecting;
        addRenderableWidget(connectButton);

        scanButton = Button.builder(
                Component.literal(scanning ? "Đang..." : "Scan Models"),
                btn -> scanModels()
        ).bounds(centerX - 50, BUTTON_ROW_Y, 95, 20).build();
        scanButton.active = !scanning;
        addRenderableWidget(scanButton);

        addRenderableWidget(Button.builder(
                Component.literal("Save & Chat"),
                btn -> {
                    saveSettings();
                    Minecraft.getInstance().setScreen(new AIChatScreen());
                }
        ).bounds(centerX + 50, BUTTON_ROW_Y, 100, 20).build());

        // Model selection buttons (from a scan)
        for (int i = 0; i < availableModels.size() && i < 8; i++) {
            final String m = availableModels.get(i);
            addRenderableWidget(Button.builder(
                    Component.literal(m),
                    btn -> {
                        modelField.setValue(m);
                        model = m;
                        logLines.add("Đã chọn model: " + m);
                    }
            ).bounds(centerX - 150 + (i % 2) * 155, MODELS_Y + (i / 2) * 24, 150, 20).build());
        }
    }

    private void testConnection() {
        if (connecting) return;
        connecting = true;
        connectStatus = "Đang kết nối...";
        logLines.add("> Test connect: " + apiUrlField.getValue());
        if (connectButton != null) {
            connectButton.active = false;
            connectButton.setMessage(Component.literal("Đang..."));
        }
        FabricAIBridge.testConnection(apiUrlField.getValue().trim()).thenAccept(result -> {
            Minecraft.getInstance().execute(() -> {
                connecting = false;
                connectStatus = result;
                logLines.add(result);
                // Only touch widgets if this screen is still showing (avoids acting on a stale UI).
                if (Minecraft.getInstance().screen == this && connectButton != null) {
                    connectButton.active = true;
                    connectButton.setMessage(Component.literal("Connect"));
                }
            });
        });
    }

    private void scanModels() {
        if (scanning) return;
        scanning = true;
        logLines.add("> Quét models từ: " + apiUrlField.getValue());
        if (scanButton != null) {
            scanButton.active = false;
            scanButton.setMessage(Component.literal("Đang..."));
        }
        FabricAIBridge.listModels(apiUrlField.getValue().trim()).thenAccept(result -> {
            Minecraft.getInstance().execute(() -> {
                scanning = false;
                if (Minecraft.getInstance().screen != this) return; // screen closed mid-scan
                if (scanButton != null) {
                    scanButton.active = true;
                    scanButton.setMessage(Component.literal("Scan Models"));
                }
                if (result.startsWith("ERROR")) {
                    logLines.add("LỖI quét models: " + result);
                    logLines.add("Fix: Server có thể không hỗ trợ /api/models. Nhập model thủ công.");
                } else {
                    availableModels.clear();
                    try {
                        JsonElement el = JsonParser.parseString(result);
                        if (el.isJsonArray()) {
                            JsonArray arr = el.getAsJsonArray();
                            for (JsonElement e : arr) {
                                availableModels.add(e.getAsString());
                            }
                        } else if (el.isJsonObject() && el.getAsJsonObject().has("models")) {
                            JsonArray arr = el.getAsJsonObject().getAsJsonArray("models");
                            for (JsonElement e : arr) {
                                if (e.isJsonObject()) availableModels.add(e.getAsJsonObject().get("name").getAsString());
                                else availableModels.add(e.getAsString());
                            }
                        }
                    } catch (Exception ex) {
                        // Fallback: split by lines
                        for (String line : result.split("\\n")) {
                            line = line.trim().replace("\"", "");
                            if (!line.isEmpty() && !line.startsWith("{") && !line.startsWith("[")) {
                                availableModels.add(line);
                            }
                        }
                    }
                    logLines.add("Tìm thấy " + availableModels.size() + " models");
                    this.init(); // refresh to show model buttons
                }
            });
        });
    }

    private void saveSettings() {
        apiUrl = apiUrlField.getValue().trim().replaceAll("/$", "");
        model = modelField.getValue().trim();
        if (model.isEmpty()) model = "verity";
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            List<String> lines = new ArrayList<>();
            lines.add("{\"apiUrl\":\"" + apiUrl + "\",\"model\":\"" + model + "\"}");
            Files.write(CONFIG_PATH, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        FabricAIBridge.API_URL = apiUrl;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int left = this.width / 2 - 150;
        guiGraphics.drawCenteredString(this.font, Component.literal("=== AI Chat Settings ==="), this.width / 2, 18, 0x55FF55);
        guiGraphics.drawString(this.font, Component.literal("API URL (full endpoint):"), left, API_LABEL_Y, 0xAAAAAA);
        guiGraphics.drawString(this.font, Component.literal("Model:"), left, MODEL_LABEL_Y, 0xAAAAAA);
        guiGraphics.drawString(this.font, Component.literal("Status: " + connectStatus), left, STATUS_Y, 0x55FF55);

        // Log window (bottom of the screen, below the model buttons)
        int logY = this.height - 110;
        guiGraphics.drawString(this.font, Component.literal("=== Log ==="), 10, logY - 15, 0xFFAA00);
        int maxLog = Math.min(logLines.size(), 6);
        for (int i = 0; i < maxLog; i++) {
            guiGraphics.drawString(this.font, Component.literal(logLines.get(logLines.size() - maxLog + i)), 10, logY + i * 14, 0xCCCCCC);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static void loadSettings() {
        if (!Files.exists(CONFIG_PATH)) return;
        try {
            String content = Files.readString(CONFIG_PATH);
            if (content.contains("\"apiUrl\"")) {
                apiUrl = content.split("\"apiUrl\":\"")[1].split("\"")[0];
            }
            if (content.contains("\"model\"")) {
                model = content.split("\"model\":\"")[1].split("\"")[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
