package com.example.donuthud;

import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class Auction {
    public final ItemStack stack;
    public final String name;
    public final double minBid;
    public final long endMs;
    public final Map<String, Double> bids = new HashMap<>();
    public String lastBidder;
    public double lastAmount;
    public boolean announced = false;

    public Auction(ItemStack stack, double minBid, long endMs) {
        this.stack = stack;
        this.name = stack.getName().getString();
        this.minBid = minBid;
        this.endMs = endMs;
    }

    public boolean active() { return System.currentTimeMillis() < endMs; }

    public long remainingSec() { return Math.max(0, (endMs - System.currentTimeMillis()) / 1000); }

    public Map.Entry<String, Double> top() {
        Map.Entry<String, Double> best = null;
        for (Map.Entry<String, Double> e : bids.entrySet())
            if (best == null || e.getValue() > best.getValue()) best = e;
        return best;
    }
}
