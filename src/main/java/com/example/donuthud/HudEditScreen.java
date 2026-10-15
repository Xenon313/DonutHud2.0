package com.example.donuthud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Drag a panel to move it, drag its corner square to resize it. Esc saves and closes. */
public class HudEditScreen extends Screen {
    private enum Target { NONE, STATS, AUCTION }

    private Target target = Target.NONE;
    private boolean resizing = false;
    private double offX, offY;
    private boolean wasDown = false;

    public HudEditScreen() { super(Text.literal("Edit HUD")); }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset layout"), b -> {
            HudConfig.get().resetLayout();
            HudConfig.save();
        }).dimensions(width / 2 - 50, height - 28, 100, 20).build());
    }

    private static boolean in(double mx, double my, double x, double y, double w, double h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        HudConfig c = HudConfig.get();
        TextRenderer f = textRenderer;
        StatsTracker stats = DonutHudClient.stats;
        List<Auction> list = AuctionManager.visibleOrSample();

        int[] st = HudPanels.statsSize(f, stats);
        int aw = HudPanels.auctionWidth(f, list), ah = HudPanels.auctionHeight(list);

        double sX = c.statsX, sY = c.statsY, sW = st[0] * c.statsScale, sH = st[1] * c.statsScale;
        double aX = c.auctionPosX(width, aw), aY = c.auctionY, aW = aw * c.auctionScale, aH = ah * c.auctionScale;

        // ---- mouse (polled, so it does not depend on Screen method signatures) ----
        long win = client.getWindow().getHandle();
        boolean down = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (down && !wasDown) {
            if (c.statsVisible && in(mx, my, sX + sW - 6, sY + sH - 6, 12, 12)) { target = Target.STATS; resizing = true; }
            else if (c.auctionVisible && in(mx, my, aX + aW - 6, aY + aH - 6, 12, 12)) { target = Target.AUCTION; resizing = true; }
            else if (c.statsVisible && in(mx, my, sX, sY, sW, sH)) { target = Target.STATS; resizing = false; offX = mx - sX; offY = my - sY; }
            else if (c.auctionVisible && in(mx, my, aX, aY, aW, aH)) { target = Target.AUCTION; resizing = false; offX = mx - aX; offY = my - aY; }
        } else if (down && target != Target.NONE) {
            if (target == Target.STATS) {
                if (resizing) c.statsScale = clamp((mx - sX) / st[0], 0.5, 4.0);
                else {
                    c.statsX = clamp(mx - offX, 0, Math.max(0, width - sW));
                    c.statsY = clamp(my - offY, 0, Math.max(0, height - sH));
                }
            } else {
                if (resizing) c.auctionScale = clamp((mx - aX) / aw, 0.5, 4.0);
                else {
                    double nx = clamp(mx - offX, 0, Math.max(0, width - aW));
                    // snap back to "centered" when close to the middle
                    c.auctionX = Math.abs(nx + aW / 2 - width / 2.0) < 6 ? -1 : nx;
                    c.auctionY = clamp(my - offY, 0, Math.max(0, height - aH));
                }
            }
        } else if (!down && wasDown && target != Target.NONE) {
            target = Target.NONE;
            HudConfig.save();
        }
        wasDown = down;

        // ---- draw panels (recompute positions after any change this frame) ----
        sX = c.statsX; sY = c.statsY; sW = st[0] * c.statsScale; sH = st[1] * c.statsScale;
        aX = c.auctionPosX(width, aw); aY = c.auctionY; aW = aw * c.auctionScale; aH = ah * c.auctionScale;

        if (c.statsVisible) {
            HudPanels.withTransform(ctx, (float) sX, (float) sY, (float) c.statsScale, () -> HudPanels.drawStats(ctx, f, stats));
            box(ctx, sX, sY, sW, sH);
        }
        if (c.auctionVisible) {
            HudPanels.withTransform(ctx, (float) aX, (float) aY, (float) c.auctionScale, () -> HudPanels.drawAuction(ctx, f, list, aw));
            box(ctx, aX, aY, aW, aH);
        }

        ctx.drawTextWithShadow(f, "§eDrag a panel to move it  §7|  §edrag the white square to resize  §7|  §eEsc to save",
                width / 2 - 170, height - 46, HudPanels.WHITE);
    }

    private void box(DrawContext ctx, double x, double y, double w, double h) {
        int x1 = (int) x, y1 = (int) y, x2 = (int) (x + w), y2 = (int) (y + h);
        int col = 0xFFFFFF55;
        ctx.fill(x1 - 1, y1 - 1, x2 + 1, y1, col);
        ctx.fill(x1 - 1, y2, x2 + 1, y2 + 1, col);
        ctx.fill(x1 - 1, y1, x1, y2, col);
        ctx.fill(x2, y1, x2 + 1, y2, col);
        ctx.fill(x2 - 4, y2 - 4, x2 + 3, y2 + 3, 0xFFFFFFFF); // resize handle
    }

    @Override
    public void removed() { HudConfig.save(); }

    @Override
    public boolean shouldPause() { return false; }
}
