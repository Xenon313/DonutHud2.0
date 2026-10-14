package com.example.donuthud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads money/shards from the sidebar scoreboard and tracks session gains. */
public class StatsTracker {
    // Tweak these if the server's scoreboard wording changes.
    private static final Pattern MONEY = Pattern.compile("(?i)money\\D{0,5}\\$?\\s*([\\d.,]+)\\s*([KMBT]?)");
    private static final Pattern SHARDS = Pattern.compile("(?i)shards?\\D{0,5}([\\d.,]+)\\s*([KMBT]?)");

    public double money = -1, shards = -1;
    private double startMoney = -1, startShards = -1;
    private long startTime = System.currentTimeMillis();

    public void reset() {
        startMoney = money;
        startShards = shards;
        startTime = System.currentTimeMillis();
    }

    public void update(MinecraftClient mc) {
        if (mc.world == null) return;
        Scoreboard sb = mc.world.getScoreboard();
        ScoreboardObjective obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (obj == null) return;

        for (ScoreboardEntry entry : sb.getScoreboardEntries(obj)) {
            Team team = sb.getScoreHolderTeam(entry.owner());
            String line = Team.decorateName(team, entry.name()).getString()
                    .replaceAll("§.", "").trim();

            Matcher m = MONEY.matcher(line);
            if (m.find()) {
                money = parse(m.group(1), m.group(2));
                if (startMoney < 0) startMoney = money;
            }
            Matcher s = SHARDS.matcher(line);
            if (s.find()) {
                shards = parse(s.group(1), s.group(2));
                if (startShards < 0) startShards = shards;
            }
        }
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
