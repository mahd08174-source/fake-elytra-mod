package com.fake;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Tiny JSON config at .minecraft/config/fakemod.json */
public final class FakeConfig {
    public static class Data {
        /** DonutSMP API key (get it with /api on the server, or use /fakemod key <key>). */
        public String apiKey = "";
        /** Used when there is no API key or the lookup fails. */
        public long elytraFallback = 240_000_000L;
        public long netheriteIngotFallback = 5_000_000L;
    }

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("fakemod.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile Data data = new Data();

    private FakeConfig() {}

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                Data d = GSON.fromJson(Files.readString(FILE), Data.class);
                if (d != null) data = d;
            } else {
                save();
            }
        } catch (Exception e) {
            System.err.println("[fakemod] could not read config: " + e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(data));
        } catch (Exception e) {
            System.err.println("[fakemod] could not write config: " + e);
        }
    }

    public static String apiKey() {
        return data.apiKey == null ? "" : data.apiKey.trim();
    }

    public static void setApiKey(String key) {
        data.apiKey = key == null ? "" : key.trim();
        save();
    }

    public static long elytraFallback() {
        return data.elytraFallback;
    }

    public static long netheriteIngotFallback() {
        return data.netheriteIngotFallback;
    }
}
