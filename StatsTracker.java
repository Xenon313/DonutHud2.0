package com.example.donuthud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads money/shards from the sidebar scoreboard and tracks session gains. */
public class StatsTracker {
    private static final Pattern NUM = Pattern.compile("(\\d[\\d,]*(?:\\.\\d+)?)\\s*([KkMmBbTt])?(?![A-Za-z])");

    public static class Row {
        public final String text;
        public final int score;
        Row(String text, int score) { this.text = text; this.score = score; }
    }

    public double money = -1, shards = -1;
    private double startMoney = -1, startShards = -1;
    private long startTime = System.currentTimeMillis();

    public void reset() {
        startMoney = money;
        startShards = shards;
        startTime = System.currentTimeMillis();
    }

    /** Every line of the sidebar scoreboard (also used by the debug key). */
    public List<Row> rows(MinecraftClient mc) {
        List<Row> out = new ArrayList<>();
        if (mc.world == null) return out;
        Scoreboard sb = mc.world.getScoreboard();
        ScoreboardObjective obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (obj == null) return out;
        for (ScoreboardEntry entry : sb.getScoreboardEntries(obj)) {
            Team team = sb.getScoreHolderTeam(entry.owner());
            String line = Team.decorateName(team, entry.name()).getString()
                    .replaceAll("§.", "").trim();
            out.add(new Row(line, entry.value()));
        }
        return out;
    }

    public void update(MinecraftClient mc) {
        for (Row r : rows(mc)) {
            double m = find(r, "money");
            if (m == -2) m = find(r, "balance");
            if (m >= 0) {
                money = m;
                if (startMoney < 0) startMoney = money;
            }
            double s = find(r, "shard");
            if (s >= 0) {
                shards = s;
                if (startShards < 0) startShards = shards;
            }
        }
    }

    /** -2 = keyword not on this line, -1 = keyword but no number, otherwise the amount. */
    private static double find(Row r, String keyword) {
        int i = r.text.toLowerCase().indexOf(keyword);
        if (i < 0) return -2;
        Matcher m = NUM.matcher(r.text.substring(i + keyword.length()));
        if (m.find()) return parse(m.group(1), m.group(2) == null ? "" : m.group(2));
        return r.score > 0 ? r.score : -1; // fall back to the score number
    }

    public double moneyGain()  { return money  < 0 || startMoney  < 0 ? 0 : money  - startMoney; }
    public double shardsGain() { return shards < 0 || startShards < 0 ? 0 : shards - startShards; }

    public double perHour(double gain) {
        double hours = (System.currentTimeMillis() - startTime) / 3_600_000.0;
        return hours < 1.0 / 3600 ? 0 : gain / hours;
    }

    public String sessionTime() {
        long s = (System.currentTimeMillis() - startTime) / 1000;
        return String.format("%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
    }

    public static double parse(String num, String suffix) {
        double v;
        try { v = Double.parseDouble(num.replace(",", "")); }
        catch (NumberFormatException e) { return -1; }
        return switch (suffix.toUpperCase()) {
            case "K" -> v * 1e3;
            case "M" -> v * 1e6;
            case "B" -> v * 1e9;
            case "T" -> v * 1e12;
            default -> v;
        };
    }

    /** Parses user input like "250k", "1.5m", "$2,000". Returns -1 if invalid. */
    public static double parseAmount(String s) {
        Matcher m = Pattern.compile("(?i)^\\s*\\$?\\s*([\\d.,]+)\\s*([kmbt]?)\\s*$").matcher(s);
        if (!m.matches()) return -1;
        return parse(m.group(1), m.group(2));
    }

    public static String fmt(double v) {
        if (v < 0) return "?";
        double a = Math.abs(v);
        if (a >= 1e12) return String.format("%.2fT", v / 1e12);
        if (a >= 1e9)  return String.format("%.2fB", v / 1e9);
        if (a >= 1e6)  return String.format("%.2fM", v / 1e6);
        if (a >= 1e3)  return String.format("%.1fK", v / 1e3);
        return String.format("%.0f", v);
    }
}
