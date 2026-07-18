package akumiyuukii.mods;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bridge to a local AI HTTP server.
 *
 * Everything here runs on a dedicated daemon thread pool with connect/request timeouts, so the
 * Minecraft render thread is NEVER blocked (previously a wrong URL would freeze the game for a
 * while, and some responses could throw on the render thread). All network work is off-thread and
 * every failure is turned into a friendly message instead of an exception that could crash MC.
 */
public class FabricAIBridge {
    public static String API_URL = "http://localhost:3000/api/components/chat/v1";

    /** Dedicated daemon pool so HTTP work never touches the render thread or blocks shutdown. */
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "AkumiYuukii-AI");
        t.setDaemon(true);
        return t;
    });

    /** Single shared client with a short connect timeout so bad URLs fail fast instead of hanging. */
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .executor(EXECUTOR)
            .build();

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    public static CompletableFuture<String> askAI(String prompt, String model, int maxTokens) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("prompt", prompt);
        requestBody.addProperty("model", model);
        requestBody.addProperty("max_tokens", maxTokens);

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(requestBody.toString()))
                    .build();
        } catch (Exception ex) {
            return CompletableFuture.completedFuture("URL không hợp lệ: " + ex.getMessage());
        }

        return CLIENT.sendAsync(request, BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        if (response.statusCode() == 200) {
                            JsonElement parsed = JsonParser.parseString(response.body());
                            if (parsed.isJsonObject()) {
                                JsonObject json = parsed.getAsJsonObject();
                                // Accept a few common field names so different servers just work.
                                for (String key : new String[]{"response", "text", "content", "message", "output"}) {
                                    if (json.has(key) && !json.get(key).isJsonNull()) {
                                        return json.get(key).getAsString();
                                    }
                                }
                            }
                            // No known field: return the raw body so the user still sees something.
                            return response.body();
                        }
                        return "Lỗi máy chủ: HTTP " + response.statusCode();
                    } catch (Exception ex) {
                        return "Lỗi đọc phản hồi: " + ex.getMessage();
                    }
                })
                .exceptionally(ex -> "Không thể kết nối local server: " + rootMessage(ex));
    }

    public static CompletableFuture<String> testConnection(String url) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString("{\"prompt\":\"test\",\"model\":\"verity\",\"max_tokens\":1}"))
                    .build();
        } catch (Exception ex) {
            return CompletableFuture.completedFuture("LỖI URL: " + ex.getMessage()
                    + ". Fix: nhập URL đầy đủ, ví dụ http://localhost:3000/api/components/chat/v1");
        }

        return CLIENT.sendAsync(request, BodyHandlers.ofString())
                .thenApply(response -> {
                    int code = response.statusCode();
                    if (code == 200) {
                        return "OK: Kết nối thành công (HTTP 200)";
                    } else if (code == 404) {
                        return "LỖI 404: URL không tồn tại. Fix: Kiểm tra lại đường dẫn API, ví dụ: http://localhost:3000/api/components/chat/v1";
                    } else if (code == 403) {
                        return "LỖI 403: Bị từ chối truy cập. Fix: Thêm API key hoặc token vào header.";
                    } else if (code == 500) {
                        return "LỖI 500: Server lỗi nội bộ. Fix: Khởi động lại server AI.";
                    } else {
                        return "LỖI HTTP " + code + ". Fix: Kiểm tra URL và server.";
                    }
                })
                .exceptionally(ex -> "LỖI KẾT NỐI: " + rootMessage(ex)
                        + ". Fix: Đảm bảo server đang chạy trên URL đã nhập.");
    }

    public static CompletableFuture<String> listModels(String baseUrl) {
        String modelsUrl = baseUrl.replace("/api/components/chat/v1", "") + "/api/models";
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(modelsUrl))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();
        } catch (Exception ex) {
            return CompletableFuture.completedFuture("ERROR:" + ex.getMessage());
        }

        return CLIENT.sendAsync(request, BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return response.body();
                    }
                    return "ERROR:" + response.statusCode();
                })
                .exceptionally(ex -> "ERROR:" + rootMessage(ex));
    }

    /** Unwraps CompletionException so the user sees the real cause, never a null message. */
    private static String rootMessage(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String msg = cause.getMessage();
        return msg != null ? msg : cause.getClass().getSimpleName();
    }
}
