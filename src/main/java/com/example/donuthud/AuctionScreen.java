package com.example.donuthud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/** Pick an item from your inventory, set a minimum bid and a timer, and start tracking it on the HUD. */
public class AuctionScreen extends Screen {
    private int selected = -1;
    private TextFieldWidget minField, minutesField;
    private final int[] slotX = new int[36], slotY = new int[36];
    private int gx, y0;

    public AuctionScreen() { super(Text.literal("New Auction")); }

    private void addSlot(int slot, int x, int y) {
        slotX[slot] = x;
        slotY[slot] = y;
        addDrawableChild(ButtonWidget.builder(Text.empty(), b -> selected = slot)
                .dimensions(x, y, 20, 20).build());
    }

    @Override
    protected void init() {
        gx = (width - 180) / 2;
        int gy = 44;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) addSlot(9 + row * 9 + col, gx + col * 20, gy + row * 20);
        for (int col = 0; col < 9; col++) addSlot(col, gx + col * 20, gy + 3 * 20 + 6);

        y0 = gy + 3 * 20 + 6 + 20 + 14;

        minField = new TextFieldWidget(textRenderer, gx, y0 + 12, 86, 20, Text.literal("Min bid"));
        minField.setMaxLength(12);
        minField.setText("100k");
        addDrawableChild(minField);

        minutesField = new TextFieldWidget(textRenderer, gx + 94, y0 + 12, 86, 20, Text.literal("Minutes"));
        minutesField.setMaxLength(6);
        minutesField.setText("5");
        addDrawableChild(minutesField);

        String[] labels = {"1m", "5m", "15m", "1h"};
        String[] vals = {"1", "5", "15", "60"};
        for (int i = 0; i < 4; i++) {
            final String v = vals[i];
            addDrawableChild(ButtonWidget.builder(Text.literal(labels[i]), b -> minutesField.setText(v))
                    .dimensions(gx + i * 46, y0 + 38, 42, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Start auction"), b -> start())
                .dimensions(gx, y0 + 64, 180, 20).build());
    }

    private void start() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (selected < 0 || mc.player.getInventory().getStack(selected).isEmpty()) {
            DonutHudClient.msg(mc, "Pick an item first");
            return;
        }
        double min = StatsTracker.parseAmount(minField.getText());
        double mins;
        try { mins = Double.parseDouble(minutesField.getText().trim()); } catch (Exception e) { mins = -1; }
        if (min < 0 || mins <= 0) {
            DonutHudClient.msg(mc, "Check the min bid (e.g. 250k) and minutes");
            return;
        }
        ItemStack stack = mc.player.getInventory().getStack(selected).copy();
        AuctionManager.add(new Auction(stack, min, System.currentTimeMillis() + (long) (mins * 60_000)));
        DonutHudClient.msg(mc, "Auction started: " + stack.getName().getString());
        close();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        var player = client.player;
        ctx.drawTextWithShadow(textRenderer, "§e§lNew Auction §7- click an item", gx, 28, HudPanels.WHITE);

        if (player != null) {
            for (int slot = 0; slot < 36; slot++) {
                ItemStack s = player.getInventory().getStack(slot);
                if (!s.isEmpty()) ctx.drawItem(s, slotX[slot] + 2, slotY[slot] + 2);
            }
            if (selected >= 0) {
                int x = slotX[selected], y = slotY[selected], col = 0xFF55FF55;
                ctx.fill(x, y, x + 20, y + 1, col);
                ctx.fill(x, y + 19, x + 20, y + 20, col);
                ctx.fill(x, y, x + 1, y + 20, col);
                ctx.fill(x + 19, y, x + 20, y + 20, col);
                ItemStack s = player.getInventory().getStack(selected);
                if (!s.isEmpty()) {
                    ctx.drawTextWithShadow(textRenderer, "§aSelected: §f" + s.getName().getString(), gx, y0 - 8, HudPanels.WHITE);
                }
            }
        }
        ctx.drawTextWithShadow(textRenderer, "§7Min bid", gx, y0 + 2, HudPanels.WHITE);
        ctx.drawTextWithShadow(textRenderer, "§7Minutes", gx + 94, y0 + 2, HudPanels.WHITE);
    }

    @Override
    public boolean shouldPause() { return false; }
}
