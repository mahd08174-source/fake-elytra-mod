package com.fake;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Latest per-unit sale price for every item, from the same data that powers
 * donutsmp.stacksail.com/item-prices. No API key needed. Fetched in the
 * background and refreshed every 5 minutes; never blocks the game thread.
 */
public final class AhPrices {
    private static final String URL = "https://api.donutsmp.jpsoftware.nl/api/items?source=api";
    private static final long TTL_MS = 5 * 60 * 1000L;
    private static final long RETRY_MS = 30 * 1000L;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final Map<String, Long> PRICE = new ConcurrentHashMap<>();
    private static final AtomicBoolean IN_FLIGHT = new AtomicBoolean(false);
    private static volatile long lastTry = 0L;
    private static volatile String status = "not fetched";

    private AhPrices() {}

    /** @param item item id without namespace, e.g. "elytra" or "netherite_ingot". Returns null if unknown yet. */
    public static Long get(String item) {
        long now = System.currentTimeMillis();
        long wait = PRICE.isEmpty() ? RETRY_MS : TTL_MS;
        if (now - lastTry > wait && IN_FLIGHT.compareAndSet(false, true)) {
            lastTry = now;
            CompletableFuture.runAsync(AhPrices::fetch)
                    .whenComplete((r, e) -> IN_FLIGHT.set(false));
        }
        return PRICE.get(item);
    }

    public static void reset() {
        PRICE.clear();
        lastTry = 0L;
        status = "not fetched";
    }

    public static String describe(String item) {
        Long p = PRICE.get(item);
        return item + ": " + (p == null ? "no price yet" : FakeModClient.fmt(p)) + " [" + status + "]";
    }

    private static void fetch() {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(URL))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                status = "HTTP " + res.statusCode();
                return;
            }

            JsonArray arr = JsonParser.parseString(res.body()).getAsJsonArray();
            int n = 0;
            for (JsonElement el : arr) {
                try {
                    JsonObject o = el.getAsJsonObject();
                    String id = o.get("item_id").getAsString();
                    if (id.startsWith("minecraft:")) id = id.substring("minecraft:".length());
                    double price = o.get("latest_price").getAsDouble();
                    int count = o.has("latest_count") ? Math.max(1, o.get("latest_count").getAsInt()) : 1;
                    long unit = Math.round(price / count);
                    if (unit > 0) {
                        PRICE.put(id, unit);
                        n++;
                    }
                } catch (Exception ignored) {
                    // skip malformed entry
                }
            }
            status = "HTTP " + res.statusCode() + " ok, " + n + " items";
        } catch (Exception e) {
            status = "error: " + e.getClass().getSimpleName() + " " + e.getMessage();
        }
    }
}
