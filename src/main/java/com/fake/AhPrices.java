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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lowest per-unit Auction House price for an item, fetched in the background
 * from the DonutSMP API and cached for 5 minutes. Never blocks the game thread.
 */
public final class AhPrices {
    private static final String BASE = "https://api.donutsmp.net/v1";
    private static final long TTL_MS = 5 * 60 * 1000L;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final Map<String, Long> PRICE = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST_TRY = new ConcurrentHashMap<>();
    private static final Map<String, String> STATUS = new ConcurrentHashMap<>();
    private static final Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();

    private AhPrices() {}

    /** @param item item id without namespace, e.g. "elytra" or "netherite_ingot". Returns null if unknown yet. */
    public static Long get(String item) {
        if (!FakeConfig.apiKey().isEmpty()) {
            long now = System.currentTimeMillis();
            long last = LAST_TRY.getOrDefault(item, 0L);
            if (now - last > TTL_MS && IN_FLIGHT.add(item)) {
                LAST_TRY.put(item, now);
                CompletableFuture.runAsync(() -> fetch(item))
                        .whenComplete((r, e) -> IN_FLIGHT.remove(item));
            }
        }
        return PRICE.get(item);
    }

    public static void reset() {
        PRICE.clear();
        LAST_TRY.clear();
        STATUS.clear();
    }

    public static String describe(String item) {
        Long p = PRICE.get(item);
        return item + ": " + (p == null ? "no AH price yet" : FakeModClient.fmt(p))
                + " [" + STATUS.getOrDefault(item, "not fetched") + "]";
    }

    private static void fetch(String item) {
        try {
            String key = FakeConfig.apiKey();
            String body = "{\"search\":\"" + item + "\",\"sort\":\"lowest_price\"}";

            HttpRequest post = HttpRequest.newBuilder(URI.create(BASE + "/auction/list/1"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = HTTP.send(post, HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() == 404 || res.statusCode() == 405) {
                HttpRequest get = HttpRequest.newBuilder(
                                URI.create(BASE + "/auction/list/1?search=" + item + "&sort=lowest_price"))
                        .timeout(Duration.ofSeconds(10))
                        .header("Authorization", "Bearer " + key)
                        .header("Accept", "application/json")
                        .GET()
                        .build();
                res = HTTP.send(get, HttpResponse.BodyHandlers.ofString());
            }

            String status = "HTTP " + res.statusCode();
            if (res.statusCode() / 100 != 2) {
                STATUS.put(item, status);
                return;
            }

            Long lowest = lowest(res.body(), item);
            if (lowest != null) {
                PRICE.put(item, lowest);
                STATUS.put(item, status + " ok");
            } else {
                STATUS.put(item, status + ", no matching listings found");
            }
        } catch (Exception e) {
            STATUS.put(item, "error: " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
    }

    private static JsonArray findArray(JsonElement root) {
        if (root == null) return null;
        if (root.isJsonArray()) return root.getAsJsonArray();
        if (!root.isJsonObject()) return null;
        JsonObject o = root.getAsJsonObject();
        for (String k : new String[]{"result", "results", "data", "auctions", "listings", "items"}) {
            if (o.has(k)) {
                JsonArray a = findArray(o.get(k));
                if (a != null) return a;
            }
        }
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            if (e.getValue().isJsonArray()) return e.getValue().getAsJsonArray();
        }
        return null;
    }

    private static Long lowest(String json, String item) {
        JsonArray arr = findArray(JsonParser.parseString(json));
        if (arr == null) return null;

        double best = Double.MAX_VALUE;
        for (JsonElement el : arr) {
            try {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                if (!o.has("price")) continue;
                double price = o.get("price").getAsDouble();

                int count = 1;
                if (o.has("item") && o.get("item").isJsonObject()) {
                    JsonObject it = o.getAsJsonObject("item");
                    if (it.has("id")) {
                        String id = it.get("id").getAsString();
                        if (!id.equals(item) && !id.endsWith(":" + item)) continue;
                    }
                    if (it.has("count")) count = Math.max(1, it.get("count").getAsInt());
                }

                double unit = price / count;
                if (unit > 0 && unit < best) best = unit;
            } catch (Exception ignored) {
                // skip malformed entry
            }
        }
        return best == Double.MAX_VALUE ? null : Math.round(best);
    }
}
