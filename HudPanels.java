package com.example.donuthud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.List;
import java.util.Map;

/** Drawing code for the two HUD panels. Everything is drawn at (0,0); callers move and scale it. */
public class HudPanels {
    public static final int WHITE = 0xFFFFFFFF;
    private static final int BG = 0x90000000;
    private static final int ROW = 24;

    public static void withTransform(DrawContext ctx, float x, float y, float scale, Runnable r) {
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(scale, scale);
        r.run();
        m.popMatrix();
    }

    // ---------------- Stats panel ----------------

    private static String sign(double v) { return (v >= 0 ? "+" : "-") + StatsTracker.fmt(Math.abs(v)); }

    public static String[] statsLines(StatsTracker s) {
        String mc = s.moneyGain() >= 0 ? "§a" : "§c";
        String sc = s.shardsGain() >= 0 ? "§a" : "§c";
        return new String[]{
                "§6Money: " + mc + StatsTracker.fmt(s.money) + " §7(" + mc + sign(s.moneyGain()) + "§7)",
                "§6  Money/hr: " + mc + sign(s.perHour(s.moneyGain())),
                "§bShards: " + sc + StatsTracker.fmt(s.shards) + " §7(" + sc + sign(s.shardsGain()) + "§7)",
                "§b  Shards/hr: " + sc + sign(s.perHour(s.shardsGain())),
                "§7Session: " + s.sessionTime()
        };
    }

    public static int[] statsSize(TextRenderer f, StatsTracker s) {
        String[] lines = statsLines(s);
        int w = 0;
        for (String l : lines) w = Math.max(w, f.getWidth(l));
        return new int[]{w + 8, lines.length * 10 + 6};
    }

    public static void drawStats(DrawContext ctx, TextRenderer f, StatsTracker s) {
        int[] sz = statsSize(f, s);
        ctx.fill(0, 0, sz[0], sz[1], BG);
        String[] lines = statsLines(s);
        for (int i = 0; i < lines.length; i++) ctx.drawTextWithShadow(f, lines[i], 4, 4 + i * 10, WHITE);
    }

    // ---------------- Auction panel ----------------

    private static String timeStr(Auction a) {
        if (!a.active()) return "§cENDED";
        long s = a.remainingSec();
        String t = String.format("%d:%02d", s / 60, s % 60);
        return (s < 30 ? "§c" : "§b") + t;
    }

    private static String line1(Auction a) {
        int n = a.stack.getCount();
        return "§f" + a.name + (n > 1 ? " x" + n : "") + "  " + timeStr(a);
    }

    private static String line2(Auction a) {
        Map.Entry<String, Double> top = a.top();
        return "§7Min §e$" + StatsTracker.fmt(a.minBid) + " §7Top "
                + (top == null ? "§8none" : "§a" + top.getKey() + " $" + StatsTracker.fmt(top.getValue()));
    }

    public static int auctionWidth(TextRenderer f, List<Auction> list) {
        int w = 170;
        for (Auction a : list) w = Math.max(w, 30 + Math.max(f.getWidth(line1(a)), f.getWidth(line2(a))));
        return w;
    }

    public static int auctionHeight(List<Auction> list) {
        return 16 + Math.max(list.size(), 1) * ROW;
    }

    public static void drawAuction(DrawContext ctx, TextRenderer f, List<Auction> list, int w) {
        ctx.fill(0, 0, w, auctionHeight(list), BG);
        ctx.drawTextWithShadow(f, "§e§lAuctions", 4, 4, WHITE);
        if (list.isEmpty()) {
            ctx.drawTextWithShadow(f, "§7No auctions - press P to start one", 4, 20, WHITE);
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            Auction a = list.get(i);
            int y = 16 + i * ROW;
            ctx.drawItem(a.stack, 4, y + 2);
            ctx.drawTextWithShadow(f, line1(a), 26, y + 2, WHITE);
            ctx.drawTextWithShadow(f, line2(a), 26, y + 13, WHITE);
        }
    }
}
