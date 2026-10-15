package com.example.donuthud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class DonutHudClient implements ClientModInitializer {
    public static final StatsTracker stats = new StatsTracker();
    private int tick = 0;

    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("donuthud", "main"));

    private static KeyBinding key(String id, int code) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyBinding(id, InputUtil.Type.KEYSYM, code, CATEGORY));
    }

    @Override
    public void onInitializeClient() {
        KeyBinding toggleStats = key("key.donuthud.toggle", GLFW.GLFW_KEY_H);
        KeyBinding reset = key("key.donuthud.reset", GLFW.GLFW_KEY_J);
        KeyBinding toggleAuction = key("key.donuthud.auction", GLFW.GLFW_KEY_L);
        KeyBinding newAuction = key("key.donuthud.new_auction", GLFW.GLFW_KEY_P);
        KeyBinding editHud = key("key.donuthud.edit", GLFW.GLFW_KEY_U);
        KeyBinding clearAuctions = key("key.donuthud.auction_clear", GLFW.GLFW_KEY_O);
        KeyBinding debug = key("key.donuthud.debug", GLFW.GLFW_KEY_G);

        // Watch chat for bid announcements
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            Auction a = AuctionManager.onChat(message.getString());
            if (a != null) {
                msg(MinecraftClient.getInstance(), "New top bid on " + a.name + ": §a" + a.lastBidder
                        + " §f$" + StatsTracker.fmt(a.lastAmount));
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            HudConfig c = HudConfig.get();
            while (toggleStats.wasPressed()) { c.statsVisible = !c.statsVisible; HudConfig.save(); msg(mc, "Stats HUD " + (c.statsVisible ? "on" : "off")); }
            while (toggleAuction.wasPressed()) { c.auctionVisible = !c.auctionVisible; HudConfig.save(); msg(mc, "Auction panel " + (c.auctionVisible ? "on" : "off")); }
            while (reset.wasPressed()) { stats.reset(); msg(mc, "Session stats reset"); }
            while (clearAuctions.wasPressed()) { AuctionManager.clear(); msg(mc, "Auctions cleared"); }
            while (newAuction.wasPressed()) { if (mc.player != null && mc.currentScreen == null) mc.setScreen(new AuctionScreen()); }
            while (editHud.wasPressed()) { if (mc.player != null && mc.currentScreen == null) mc.setScreen(new HudEditScreen()); }
            while (debug.wasPressed()) {
                if (mc.player == null) continue;
                List<StatsTracker.Row> rows = stats.rows(mc);
                mc.player.sendMessage(Text.literal("§e[DonutHUD] scoreboard lines: " + rows.size()), false);
                for (StatsTracker.Row r : rows)
                    mc.player.sendMessage(Text.literal("§7[sb] §f" + r.text + " §8(score " + r.score + ")"), false);
            }

            if (++tick >= 20) { // once per second
                tick = 0;
                stats.update(mc);
                AuctionManager.tick(mc);
            }
        });

        HudElementRegistry.addLast(Identifier.of("donuthud", "overlay"), (ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.options.hudHidden) return;
            if (mc.currentScreen instanceof HudEditScreen) return; // the editor draws them itself
            var font = mc.textRenderer;
            HudConfig c = HudConfig.get();
            int sw = ctx.getScaledWindowWidth();

            if (c.statsVisible) {
                HudPanels.withTransform(ctx, (float) c.statsX, (float) c.statsY, (float) c.statsScale,
                        () -> HudPanels.drawStats(ctx, font, stats));
            }
            if (c.auctionVisible) {
                List<Auction> list = AuctionManager.visible();
                int w = HudPanels.auctionWidth(font, list);
                float x = (float) c.auctionPosX(sw, w);
                HudPanels.withTransform(ctx, x, (float) c.auctionY, (float) c.auctionScale,
                        () -> HudPanels.drawAuction(ctx, font, list, w));
            }
        });
    }

    public static void msg(MinecraftClient mc, String s) {
        if (mc.player != null) mc.player.sendMessage(Text.literal("§e[DonutHUD] §f" + s), true);
    }
}
