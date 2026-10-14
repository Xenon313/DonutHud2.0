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
import java.util.Map;

public class DonutHudClient implements ClientModInitializer {
    private static final StatsTracker stats = new StatsTracker();
    private static final BidTracker bids = new BidTracker();
    private static boolean visible = true, auctionVisible = true;
    private static int corner = 0; // 0=TL 1=TR 2=BL 3=BR
    private int tick = 0;

    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("donuthud", "main"));

    private static KeyBinding key(String id, int code) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyBinding(id, InputUtil.Type.KEYSYM, code, CATEGORY));
    }

    @Override
    public void onInitializeClient() {
        KeyBinding toggle = key("key.donuthud.toggle", GLFW.GLFW_KEY_H);
        KeyBinding reset = key("key.donuthud.reset", GLFW.GLFW_KEY_J);
        KeyBinding cornerKey = key("key.donuthud.corner", GLFW.GLFW_KEY_K);
        KeyBinding auctionToggle = key("key.donuthud.auction", GLFW.GLFW_KEY_L);
        KeyBinding auctionClear = key("key.donuthud.auction_clear", GLFW.GLFW_KEY_O);

        // Watch chat for bid announcements
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (bids.onChat(message.getString())) {
                MinecraftClient mc = MinecraftClient.getInstance();
                msg(mc, "New top bid: §a" + bids.lastTopBidder + " §f- $" + StatsTracker.fmt(bids.lastTopAmount));
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (toggle.wasPressed()) { visible = !visible; msg(mc, "Stats HUD " + (visible ? "on" : "off")); }
            while (reset.wasPressed()) { stats.reset(); msg(mc, "Session stats reset"); }
            while (cornerKey.wasPressed()) corner = (corner + 1) % 4;
            while (auctionToggle.wasPressed()) { auctionVisible = !auctionVisible; msg(mc, "Auction panel " + (auctionVisible ? "on" : "off")); }
            while (auctionClear.wasPressed()) { bids.clear(); msg(mc, "Auction bids cleared"); }

            if (++tick >= 20) { tick = 0; stats.update(mc); }
        });

        HudElementRegistry.addLast(Identifier.of("donuthud", "overlay"), (ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.options.hudHidden) return;
            var font = mc.textRenderer;
            int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight(), pad = 4;

            // ---- Stats panel ----
            if (visible) {
                String[] lines = {
                        "§6Money: §f" + StatsTracker.fmt(stats.money) + " §7(" + sign(stats.moneyGain()) + ")",
                        "§6  /hr: §f" + StatsTracker.fmt(stats.perHour(stats.moneyGain())),
                        "§bShards: §f" + StatsTracker.fmt(stats.shards) + " §7(" + sign(stats.shardsGain()) + ")",
                        "§b  /hr: §f" + StatsTracker.fmt(stats.perHour(stats.shardsGain())),
                        "§7Session: " + stats.sessionTime()
                };
                int w = 0;
                for (String l : lines) w = Math.max(w, font.getWidth(l));
                int h = lines.length * 10;
                int x = (corner % 2 == 0) ? pad : sw - w - pad * 3;
                int y = (corner < 2) ? pad : sh - h - pad * 3;
                ctx.fill(x - 3, y - 3, x + w + 3, y + h + 1, 0x90000000);
                for (int i = 0; i < lines.length; i++) ctx.drawTextWithShadow(font, lines[i], x, y + i * 10, 0xFFFFFFFF);
            }

            // ---- Auction leaderboard (right side, vertically centered) ----
            if (auctionVisible) {
                List<Map.Entry<String, Double>> top = bids.top(5);
                String[] medals = {"§6#1 ", "§7#2 ", "§c#3 ", "§f#4 ", "§f#5 "};
                int rows = Math.max(top.size(), 1) + 1;
                String title = "§e§lTop Bidders";
                int w = font.getWidth(title);
                String[] lines = new String[top.size()];
                for (int i = 0; i < top.size(); i++) {
                    lines[i] = medals[i] + "§f" + top.get(i).getKey() + " §a$" + StatsTracker.fmt(top.get(i).getValue());
                    w = Math.max(w, font.getWidth(lines[i]));
                }
                String empty = "§7Waiting for bids...";
                if (top.isEmpty()) w = Math.max(w, font.getWidth(empty));
                int h = rows * 10;
                int x = sw - w - pad * 3;
                int y = sh / 2 - h / 2;
                ctx.fill(x - 3, y - 3, x + w + 3, y + h + 1, 0x90000000);
                ctx.drawTextWithShadow(font, title, x, y, 0xFFFFFFFF);
                if (top.isEmpty()) ctx.drawTextWithShadow(font, empty, x, y + 10, 0xFFFFFFFF);
                for (int i = 0; i < lines.length; i++) ctx.drawTextWithShadow(font, lines[i], x, y + (i + 1) * 10, 0xFFFFFFFF);
            }
        });
    }

    private static String sign(double v) { return (v >= 0 ? "+" : "-") + StatsTracker.fmt(Math.abs(v)); }

    private static void msg(MinecraftClient mc, String s) {
        if (mc.player != null) mc.player.sendMessage(Text.literal("§e[DonutHUD] §f" + s), true);
    }
}
