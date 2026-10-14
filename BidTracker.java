package com.example.donuthud;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses auction bid messages from chat and keeps a leaderboard of top bidders. */
public class BidTracker {
    // Edit these to match the server's real bid messages. Group 1 = player, group 2 = amount, group 3 = K/M/B/T suffix.
    private static final Pattern[] PATTERNS = {
            Pattern.compile("(?i)(\\w{3,16}) (?:placed a bid of|bid|bids|raised the bid to|is now the highest bidder with)\\s*\\$?\\s*([\\d.,]+)\\s*([KMBT]?)"),
            Pattern.compile("(?i)new (?:top |highest )?bid:?\\s*(\\w{3,16})\\D{0,10}\\$?\\s*([\\d.,]+)\\s*([KMBT]?)"),
    };

    private final Map<String, Double> best = new HashMap<>();
    public String lastTopBidder = null;
    public double lastTopAmount = 0;

    /** Returns true if this line was a bid and a NEW highest bid was set. */
    public boolean onChat(String raw) {
        String line = raw.replaceAll("§.", "").trim();
        for (Pattern p : PATTERNS) {
            Matcher m = p.matcher(line);
            if (!m.find()) continue;
            double amount = StatsTracker.parse(m.group(2), m.group(3));
            if (amount < 0) continue;
            String player = m.group(1);
            best.merge(player, amount, Math::max);
            if (amount > lastTopAmount) {
                lastTopAmount = amount;
                lastTopBidder = player;
                return true;
            }
            return false;
        }
        return false;
    }

    public List<Map.Entry<String, Double>> top(int n) {
        List<Map.Entry<String, Double>> list = new ArrayList<>(best.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return list.subList(0, Math.min(n, list.size()));
    }

    public void clear() {
        best.clear();
        lastTopBidder = null;
        lastTopAmount = 0;
    }
}
