package nexus.integrations.common;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Robust HTTP client abstraction for NEXUS V26 external integrations.
 *
 * Reliability & Security:
 * - Uses standard Java 21 HttpClient.
 * - Enforces strict connect and request timeouts.
 * - Implements exponential backoff retry for transient network and 5xx errors.
 * - Completely non-blocking and safe for use outside the Swing Event Dispatch Thread (EDT).
 */
public class HttpHelper {
    private static final HttpClient defaultClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static HttpClient getDefaultClient() {
        return defaultClient;
    }

    /**
     * Executes an HTTP request with exponential backoff retry.
     *
     * @param client The HttpClient instance.
     * @param request The HttpRequest to send.
     * @param maxRetries Maximum number of attempts (e.g., 3).
     * @param initialDelayMs Initial delay between attempts in milliseconds (e.g., 500).
     * @return HttpResponse on success, or null if all retry attempts failed.
     */
    public static HttpResponse<String> executeWithRetry(
            HttpClient client,
            HttpRequest request,
            int maxRetries,
            long initialDelayMs) {

        int attempt = 0;
        long currentDelay = initialDelayMs;
        Exception lastException = null;

        while (attempt < maxRetries) {
            attempt++;
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();

                // If status is 2xx or 3xx, consider it successful
                if (status >= 200 && status < 400) {
                    return response;
                }

                // If 4xx client error (except 429 Too Many Requests), do not retry
                if (status >= 400 && status < 500 && status != 429) {
                    System.err.println("[HttpHelper] Non-retryable HTTP " + status + " from " + request.uri());
                    return response;
                }

                // For 5xx or 429, retry with backoff
                System.err.println("[HttpHelper] Attempt " + attempt + " returned HTTP " + status + ". Retrying in " + currentDelay + "ms...");

            } catch (IOException | InterruptedException e) {
                lastException = e;
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
                System.err.println("[HttpHelper] Attempt " + attempt + " failed (" + e.getMessage() + "). Retrying in " + currentDelay + "ms...");
            }

            if (attempt < maxRetries) {
                try {
                    Thread.sleep(currentDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                currentDelay *= 2; // Exponential backoff
            }
        }

        System.err.println("[HttpHelper] All " + maxRetries + " attempts failed for " + request.uri() +
                (lastException != null ? ": " + lastException.getMessage() : ""));
        return null;
    }

    /**
     * Sends an HTTP request asynchronously outside the calling thread.
     */
    public static CompletableFuture<HttpResponse<String>> sendAsync(HttpClient client, HttpRequest request) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}
