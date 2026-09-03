package dev.shadowsoffire.apothic_enchanting.table;

import javax.annotation.Nullable;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import dev.shadowsoffire.apothic_enchanting.Ench;
import dev.shadowsoffire.apothic_enchanting.payloads.SetRavenStatsPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.neoforged.neoforge.network.PacketDistributor;

public class RavenEnchantmentScreen extends ApothEnchantmentScreen {

    private static final int BAR_X = 59;
    private static final int BAR_W = 110;
    private static final int BAR_H = 5;
    private static final int ETERNA_BAR_Y = 75;
    private static final int QUANTA_BAR_Y = 85;
    private static final int ARCANA_BAR_Y = 95;
    private static final int HIT_PAD_Y = 3;

    private static final int BAR_U = 0;
    private static final int BAR_V_ETERNA = 197;
    private static final int BAR_V_QUANTA = 202;
    private static final int BAR_V_ARCANA = 207;

    private static final int HANDLE_W = 4;
    private static final int HANDLE_H = 7;
    private static final int HANDLE_U = 122;
    private static final int HANDLE_V_ETERNA = 197;
    private static final int HANDLE_V_QUANTA = 204;
    private static final int HANDLE_V_ARCANA = 211;

    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    private final RavenEnchantmentMenu ravenMenu;

    private int currentEterna;
    private int currentQuanta;
    private int currentArcana;
    @Nullable
    private DraggedStat draggingStat = null;

    /**
     * Updated whenever the player manipulates a stat bar. Indicates the stats need to be sent server-side.
     */
    private boolean dirty = false;

    public RavenEnchantmentScreen(EnchantmentMenu container, Inventory inv, Component title) {
        super(container, inv, title);
        this.ravenMenu = (RavenEnchantmentMenu) container;
        this.readMenuStats();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.currentEterna = Math.min(this.currentEterna, this.eternaMax());
        // Suppress the parent's lerp/decay so the bar fills track the player-set values instantly.
        // The parent already wrote smoothed values into these fields; clobber them.
        this.eterna = this.currentEterna;
        this.lastEterna = this.eterna;
        this.quanta = this.currentQuanta;
        this.lastQuanta = this.quanta;
        this.arcana = this.currentArcana;
        this.lastArcana = this.arcana;
        // Throttled live-drag commit: at most one packet per tick.
        if (this.dirty) {
            this.sendStats();
            this.dirty = false;
        }
        else {
            this.readMenuStats();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int eternaMax = this.eternaMax();
        if (this.isHovering(BAR_X, ETERNA_BAR_Y - HIT_PAD_Y, BAR_W, BAR_H + 2 * HIT_PAD_Y, mouseX, mouseY)) {
            this.draggingStat = DraggedStat.ETERNA;
            this.updateDraggedValue(mouseX, this.draggingStat, eternaMax);
            return true;
        }
        if (this.isHovering(BAR_X, QUANTA_BAR_Y - HIT_PAD_Y, BAR_W, BAR_H + 2 * HIT_PAD_Y, mouseX, mouseY)) {
            this.draggingStat = DraggedStat.QUANTA;
            this.updateDraggedValue(mouseX, this.draggingStat, 100);
            return true;
        }
        if (this.isHovering(BAR_X, ARCANA_BAR_Y - HIT_PAD_Y, BAR_W, BAR_H + 2 * HIT_PAD_Y, mouseX, mouseY)) {
            this.draggingStat = DraggedStat.ARCANA;
            this.updateDraggedValue(mouseX, this.draggingStat, 100);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (this.draggingStat != null) {
            int max = this.draggingStat == DraggedStat.ETERNA ? this.eternaMax() : 100;
            this.updateDraggedValue(mouseX, this.draggingStat, max);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.draggingStat != null) {
            this.draggingStat = null;
            // Flush any pending change so the final value is committed even if it landed between ticks.
            if (this.dirty) {
                this.sendStats();
                this.dirty = false;
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY) {
        // The super impl is going to try and interpolate the changes. Since the player can drag them, we want to force them to the "real"
        // position immediately. Any other effect causes the bar to "lag" which is bad UX.
        this.eterna = this.currentEterna;
        this.lastEterna = this.eterna;
        this.quanta = this.currentQuanta;
        this.lastQuanta = this.quanta;
        this.arcana = this.currentArcana;
        this.lastArcana = this.arcana;

        super.renderBg(gfx, partialTicks, mouseX, mouseY);

        int xCenter = (this.width - this.imageWidth) / 2;
        int yCenter = (this.height - this.imageHeight) / 2;

        // Overdraw the parent's int-precision bars with fractional-width fills so the bar tip
        // matches the handle position exactly. Same float formula governs both: value * 1.1.
        this.drawFractionalBar(gfx, xCenter + BAR_X, yCenter + ETERNA_BAR_Y, this.currentEterna, BAR_V_ETERNA);
        this.drawFractionalBar(gfx, xCenter + BAR_X, yCenter + QUANTA_BAR_Y, this.currentQuanta, BAR_V_QUANTA);
        this.drawFractionalBar(gfx, xCenter + BAR_X, yCenter + ARCANA_BAR_Y, this.currentArcana, BAR_V_ARCANA);

        this.drawFractionalHandle(gfx, xCenter + BAR_X, yCenter + ETERNA_BAR_Y, this.currentEterna, HANDLE_V_ETERNA);
        this.drawFractionalHandle(gfx, xCenter + BAR_X, yCenter + QUANTA_BAR_Y, this.currentQuanta, HANDLE_V_QUANTA);
        this.drawFractionalHandle(gfx, xCenter + BAR_X, yCenter + ARCANA_BAR_Y, this.currentArcana, HANDLE_V_ARCANA);
    }

    /**
     * Fill width = {@code value * 1.1F} (the float counterpart of the parent's {@code getBarLength}).
     * Source rect is sampled proportionally — for a uniform bar texture this is visually identical
     * to the int blit at integer widths, but adds sub-pixel anti-aliasing on the trailing edge.
     */
    private void drawFractionalBar(GuiGraphics gfx, float barX, float barY, int value, int barV) {
        float w = value * (BAR_W / 100F);
        if (w <= 0) return;
        blitFractional(gfx, TEXTURES, barX, barY, w, BAR_H, BAR_U, barV, w, BAR_H);
    }

    /**
     * Position the 4×7 handle sprite centered on the bar's fractional tip so the handle slides
     * smoothly as the value changes — no rounding-mode discrepancy with the bar tip.
     */
    private void drawFractionalHandle(GuiGraphics gfx, float barX, float barY, int value, int handleV) {
        float tipX = barX + value * (BAR_W / 100F);
        float handleX = tipX - HANDLE_W / 2F;
        float handleY = barY + (BAR_H - HANDLE_H) / 2F;
        blitFractional(gfx, TEXTURES, handleX, handleY, HANDLE_W, HANDLE_H, HANDLE_U, handleV, HANDLE_W, HANDLE_H);
    }

    /**
     * Float-precision blit; mirrors {@link GuiGraphics}'s innerBlit but with fractional geometry.
     * Replaces 26.1's FractionalBlitRenderState, which has no equivalent in the immediate-mode pipeline.
     */
    private static void blitFractional(GuiGraphics gfx, ResourceLocation tex, float x, float y, float w, float h, float u, float v, float srcW, float srcH) {
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f pose = gfx.pose().last().pose();
        float u0 = u / TEX_W;
        float u1 = (u + srcW) / TEX_W;
        float v0 = v / TEX_H;
        float v1 = (v + srcH) / TEX_H;
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.addVertex(pose, x, y, 0).setUv(u0, v0);
        buf.addVertex(pose, x, y + h, 0).setUv(u0, v1);
        buf.addVertex(pose, x + w, y + h, 0).setUv(u1, v1);
        buf.addVertex(pose, x + w, y, 0).setUv(u1, v0);
        BufferUploader.drawWithShader(buf.buildOrThrow());
    }

    private void updateDraggedValue(double mouseX, DraggedStat stat, int max) {
        double t = Mth.clamp((mouseX - (this.leftPos + BAR_X)) / (double) BAR_W, 0.0, 1.0);
        int value = Mth.clamp((int) Math.round(t * 100), 0, max);
        switch (stat) {
            case ETERNA -> {
                if (value != this.currentEterna) {
                    this.currentEterna = value;
                    this.dirty = true;
                }
            }
            case QUANTA -> {
                if (value != this.currentQuanta) {
                    this.currentQuanta = value;
                    this.dirty = true;
                }
            }
            case ARCANA -> {
                if (value != this.currentArcana) {
                    this.currentArcana = value;
                    this.dirty = true;
                }
            }
        }
    }

    // We use Minecraft.getInstance() here since this.minecraft is assigned after the first time we need to call this.
    private int eternaMax() {
        return (int) Minecraft.getInstance().player.getAttributeValue(Ench.Attributes.MAX_ETERNA);
    }

    private void sendStats() {
        PacketDistributor.sendToServer(new SetRavenStatsPayload(this.currentEterna, this.currentQuanta, this.currentArcana));
        this.ravenMenu.setRavenStats(this.currentEterna, this.currentQuanta, this.currentArcana);
    }

    private void readMenuStats() {
        RavenTableStats ravenStats = this.ravenMenu.getRavenStats();
        this.currentEterna = Math.min(ravenStats.eterna(), this.eternaMax());
        this.currentQuanta = ravenStats.quanta();
        this.currentArcana = ravenStats.arcana();
    }

    private static enum DraggedStat {
        ETERNA,
        QUANTA,
        ARCANA;
    }

}
