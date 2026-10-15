package com.example.donuthud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Saved HUD layout (position, size, visibility). Stored in config/donuthud.json. */
public class HudConfig {
    public double statsX = 6, statsY = 6, statsScale = 1.0;
    /** auctionX < 0 means "centered horizontally". */
    public double auctionX = -1, auctionY = 6, auctionScale = 1.0;
    public boolean statsVisible = true, auctionVisible = true;

    private static HudConfig inst;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("donuthud.json");
    }

    public static HudConfig get() {
        if (inst == null) inst = load();
        return inst;
    }

    private static HudConfig load() {
        try {
            Path f = file();
            if (Files.exists(f)) {
                HudConfig c = GSON.fromJson(Files.readString(f), HudConfig.class);
                if (c != null) return c;
            }
        } catch (Exception ignored) { }
        return new HudConfig();
    }

    public static void save() {
        try {
            Files.writeString(file(), GSON.toJson(get()));
        } catch (Exception ignored) { }
    }

    public double auctionPosX(int screenWidth, double panelWidth) {
        return auctionX < 0 ? (screenWidth - panelWidth * auctionScale) / 2.0 : auctionX;
    }

    public void resetLayout() {
        statsX = 6; statsY = 6; statsScale = 1.0;
        auctionX = -1; auctionY = 6; auctionScale = 1.0;
    }
}
