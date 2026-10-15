package com.example.donuthud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Keeps your auctions and matches bid messages from chat to them. Purely local - sends nothing to the server. */
public class AuctionManager {
    // Edit these to match the server's real bid messages. Group 1 = player, 2 = amount, 3 = K/M/B/T suffix.
    private static final Pattern[] PATTERNS = {
            Pattern.compile("(?i)(\\w{3,16}) (?:placed a bid of|bid|bids|raised the bid to|is now the highest bidder with)\\s*\\$?\\s*([\\d.,]+)\\s*([KMBT]?)"),
            Pattern.compile("(?i)new (?:top |highest )?bid:?\\s*(\\w{3,16})\\D{0,10}\\$?\\s*([\\d.,]+)\\s*([KMBT]?)"),
    };

    private static final List<Auction> auctions = new ArrayList<>();
    private static List<Auction> sample;

    public static void add(Auction a) { auctions.add(a); }
    public static void clear() { auctions.clear(); }

    /** Newest 4 auctions, oldest first. */
    public static List<Auction> visible() {
        int from = Math.max(0, auctions.size() - 4);
        return new ArrayList<>(auctions.subList(from, auctions.size()));
    }

    /** Fake auction used so the layout editor has something to show. */
    public static List<Auction> visibleOrSample() {
        List<Auction> v = visible();
        if (!v.isEmpty()) return v;
        if (sample == null) {
            Auction a = new Auction(new ItemStack(Items.DIAMOND), 100_000, System.currentTimeMillis() + 5 * 60_000L);
            a.bids.put("Steve", 250_000.0);
            sample = List.of(a);
        }
        return sample;
    }

    /** Returns the auction that got a new highest bid, or null. */
    public static Auction onChat(String raw) {
        String line = raw.replaceAll("§.", "").trim();
        for (Pattern p : PATTERNS) {
            Matcher m = p.matcher(line);
            if (!m.find()) continue;
            double amount = StatsTracker.parse(m.group(2), m.group(3));
            if (amount < 0) continue;
            String player = m.group(1);

            String low = line.toLowerCase();
            Auction target = null, newestActive = null;
            for (int i = auctions.size() - 1; i >= 0; i--) {
                Auction a = auctions.get(i);
                if (!a.active()) continue;
                if (newestActive == null) newestActive = a;
                if (low.contains(a.name.toLowerCase())) { target = a; break; }
            }
            if (target == null) target = newestActive;
            if (target == null || amount < target.minBid) return null;

            Map.Entry<String, Double> before = target.top();
            double prev = before == null ? 0 : before.getValue();
            target.bids.merge(player, amount, Math::max);
            if (amount > prev) {
                target.lastBidder = player;
                target.lastAmount = amount;
                return target;
            }
            return null;
        }
        return null;
    }

    /** Announces finished auctions and clears old ones. Call about once a second. */
    public static void tick(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        for (Auction a : auctions) {
            if (!a.active() && !a.announced) {
                a.announced = true;
                Map.Entry<String, Double> top = a.top();
                DonutHudClient.msg(mc, top == null
                        ? "Auction ended: " + a.name + " - no bids"
                        : "Auction ended: " + a.name + " - winner §a" + top.getKey() + " §f$" + StatsTracker.fmt(top.getValue()));
            }
        }
        auctions.removeIf(a -> now - a.endMs > 120_000);
    }
}
