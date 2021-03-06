package net.totaldarkness.ChestHistory.client.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.List;

import static net.totaldarkness.ChestHistory.client.util.Helper.MC;
import static net.totaldarkness.ChestHistory.client.util.Helper.getLocalPlayer;
import static net.minecraft.client.renderer.GlStateManager.color;

/**
 * 2D rendering
 */
public class SurfaceHelper {

    public static void drawRect(int x, int y, int w, int h, int color) {
        GL11.glLineWidth(1.0f);
        Gui.drawRect(x, y, x + w, y + h, color);
    }

    public static void drawOutlinedRect(int x, int y, int w, int h, int color) {
        drawOutlinedRect(x, y, w, h, color, 1.f);
    }

    public static void drawOutlinedRectShaded(
            int x, int y, int w, int h, int colorOutline, int shade, float width) {
        int shaded = (0x00FFFFFF & colorOutline) | ((shade & 255) << 24); // modify the alpha value
        // int shaded = Utils.toRGBA(255,255,255, 100);
        drawRect(x, y, w, h, shaded);
        drawOutlinedRect(x, y, w, h, colorOutline, width);
    }

    public static void drawOutlinedRect(int x, int y, int w, int h, int color, float width) {
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        float a = (float) (color >> 24 & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder BufferBuilder = tessellator.getBuffer();

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        color(r, g, b, a);

        GL11.glLineWidth(width);

        BufferBuilder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        BufferBuilder.pos(x, y, 0.0D).endVertex();
        BufferBuilder.pos(x, (double) y + h, 0.0D).endVertex();
        BufferBuilder.pos((double) x + w, (double) y + h, 0.0D).endVertex();
        BufferBuilder.pos((double) x + w, y, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawText(String msg, int x, int y, int color) {
        MC.fontRenderer.drawString(msg, x, y, color);
    }

    public static void drawTextShadow(String msg, float x, float y, int color) {
        MC.fontRenderer.drawStringWithShadow(msg, x, y, color);
    }

    public static void drawText(String msg, float x, float y, int color, double scale, boolean shadow) {
        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.scale(scale, scale, scale);
        MC.fontRenderer.drawString(
                msg, (float) (x * (1 / scale)), (float) (y * (1 / scale)), color, shadow);
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    public static void drawText(String msg, int x, int y, int color, double scale) {
        drawText(msg, x, y, color, scale, false);
    }

    public static void drawTextShadow(String msg, float x, float y, int color, double scale) {
        drawText(msg, x, y, color, scale, true);
    }

    public static int getTextWidth(String text, double scale) {
        return (int) (MC.fontRenderer.getStringWidth(text) * scale);
    }

    public static int getTextWidth(String text) {
        return getTextWidth(text, 1.D);
    }

    public static int getTextHeight() {
        return MC.fontRenderer.FONT_HEIGHT;
    }

    public static int getTextHeight(double scale) {
        return (int) (MC.fontRenderer.FONT_HEIGHT * scale);
    }

    public static double getTextHeightDouble(double scale) {
        return (MC.fontRenderer.FONT_HEIGHT * scale);
    }

    public static void drawItem(ItemStack item, int x, int y) {
        MC.getRenderItem().renderItemAndEffectIntoGUI(item, x, y);
    }

    public static void drawItemOverlay(ItemStack stack, int x, int y) {
        MC.getRenderItem().renderItemOverlayIntoGUI(MC.fontRenderer, stack, x, y, null);
    }

    public static void drawItem(ItemStack item, double x, double y, double scale) {
        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableColorMaterial();
        GlStateManager.enableLighting();
        GlStateManager.scale(scale / 16.D, scale / 16.D, scale / 16.D);
        MC.getRenderItem().zLevel = 100.f;
        renderItemAndEffectIntoGUI(getLocalPlayer(), item, x * (16 / scale), y * (16 / scale), 16.D);
        MC.getRenderItem().zLevel = 0.f;
        GlStateManager.popMatrix();
        GlStateManager.disableLighting();
        GlStateManager.enableDepth();
        color(1.f, 1.f, 1.f, 1.f);
    }

    public static void drawItemWithOverlay(ItemStack item, double x, double y, double scale) {
        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableColorMaterial();
        GlStateManager.enableLighting();
        GlStateManager.scale(scale / 16.D, scale / 16.D, scale / 16.D);
        MC.getRenderItem().zLevel = 100.f;
        renderItemAndEffectIntoGUI(getLocalPlayer(), item, x * (16 / scale), y * (16 / scale), 16.D);
        renderItemOverlayIntoGUI(MC.fontRenderer, item, x * (16 / scale), y * (16 / scale), null, scale);
        MC.getRenderItem().zLevel = 0.f;
        GlStateManager.popMatrix();
        GlStateManager.disableLighting();
        GlStateManager.enableDepth();
        GlStateManager.color(1.f, 1.f, 1.f, 1.f);
    }

    protected static void renderItemAndEffectIntoGUI(
            @Nullable EntityLivingBase living, final ItemStack stack, double x, double y, double scale) {
        if (!stack.isEmpty()) {
            MC.getRenderItem().zLevel += 50.f;
            try {
                renderItemModelIntoGUI(
                        stack, x, y, MC.getRenderItem().getItemModelWithOverrides(stack, null, living), scale);
            } catch (Throwable ignored) { } finally {
                MC.getRenderItem().zLevel -= 50.f;
            }
        }
    }

    private static void renderItemModelIntoGUI(
            ItemStack stack, double x, double y, IBakedModel bakedmodel, double scale) {
        GlStateManager.pushMatrix();
        MC.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        MC.getTextureManager()
                .getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)
                .setBlurMipmap(false, false);
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        color(1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.translate(x, y, 100.0F + MC.getRenderItem().zLevel);
        GlStateManager.translate(8.0F, 8.0F, 0.0F);
        GlStateManager.scale(1.0F, -1.0F, 1.0F);
        GlStateManager.scale(scale, scale, scale);

        if (bakedmodel.isGui3d()) {
            GlStateManager.enableLighting();
        } else {
            GlStateManager.disableLighting();
        }

        bakedmodel =
                net.minecraftforge.client.ForgeHooksClient.handleCameraTransforms(
                        bakedmodel, ItemCameraTransforms.TransformType.GUI, false);
        MC.getRenderItem().renderItem(stack, bakedmodel);
        GlStateManager.disableAlpha();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableLighting();
        GlStateManager.popMatrix();
        MC.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        MC.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
    }

    protected static void renderItemOverlayIntoGUI(
            FontRenderer fr,
            ItemStack stack,
            double xPosition,
            double yPosition,
            @Nullable String text,
            double scale) {
        final double SCALE_RATIO = 1.23076923077D;

        if (!stack.isEmpty()) {
            if (stack.getCount() != 1 || text != null) {
                String s = text == null ? String.valueOf(stack.getCount()) : text;
                GlStateManager.disableLighting();
                GlStateManager.disableDepth();
                GlStateManager.disableBlend();

                fr.drawStringWithShadow(s, (float) (xPosition + 17 - fr.getStringWidth(s)), (float) (yPosition + 9), 16777215);

                GlStateManager.enableLighting();
                GlStateManager.enableDepth();
                // Fixes opaque cooldown overlay a bit lower
                GlStateManager.enableBlend();
            }

            if (stack.getItem().showDurabilityBar(stack)) {
                GlStateManager.disableLighting();
                GlStateManager.disableDepth();
                GlStateManager.disableTexture2D();
                GlStateManager.disableAlpha();
                GlStateManager.disableBlend();
                double health = stack.getItem().getDurabilityForDisplay(stack);
                int rgbfordisplay = stack.getItem().getRGBDurabilityForDisplay(stack);
                int i = Math.round(13.0F - (float) health * 13.0F);
                draw(xPosition + 2, yPosition + (16 / SCALE_RATIO), 13, 2, 0, 0, 0, 255);
                draw(
                        xPosition + 2,
                        yPosition + (16 / SCALE_RATIO),
                        i,
                        1,
                        rgbfordisplay >> 16 & 255,
                        rgbfordisplay >> 8 & 255,
                        rgbfordisplay & 255,
                        255);
                GlStateManager.enableBlend();
                GlStateManager.enableAlpha();
                GlStateManager.enableTexture2D();
                GlStateManager.enableLighting();
                GlStateManager.enableDepth();
            }

            EntityPlayerSP entityplayersp = Minecraft.getMinecraft().player;

            float f3 =
                    getLocalPlayer() == null
                            ? 0.0F
                            : getLocalPlayer()
                            .getCooldownTracker()
                            .getCooldown(stack.getItem(), MC.getRenderPartialTicks());

            if (f3 > 0.0F) {
                GlStateManager.disableLighting();
                GlStateManager.disableDepth();
                GlStateManager.disableTexture2D();
                draw(xPosition, yPosition + scale * (1.0F - f3), 16, scale * f3, 255, 255, 255, 127);
                GlStateManager.enableTexture2D();
                GlStateManager.enableLighting();
                GlStateManager.enableDepth();
            }
        }
    }

    private static void draw(
            double x, double y, double width, double height, int red, int green, int blue, int alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder renderer = tessellator.getBuffer();
        renderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        renderer
                .pos(x + 0, y + 0, 0.0D)
                .color(red, green, blue, alpha)
                .endVertex();
        renderer
                .pos(x + 0, y + height, 0.0D)
                .color(red, green, blue, alpha)
                .endVertex();
        renderer
                .pos(x + width, y + height, 0.0D)
                .color(red, green, blue, alpha)
                .endVertex();
        renderer
                .pos(x + width, y + 0, 0.0D)
                .color(red, green, blue, alpha)
                .endVertex();
        Tessellator.getInstance().draw();
    }

    public static void drawHoveringText(List<String> textLines, int x, int y) {
        drawHoveringText(textLines,x,y,MC.fontRenderer);
    }

    public static void drawHoveringText(List<String> textLines, int x, int y, FontRenderer font) {
        ScaledResolution res = new ScaledResolution(MC);
        int maxX = res.getScaledWidth(); int maxY = res.getScaledHeight();
        net.minecraftforge.fml.client.config.GuiUtils.drawHoveringText(textLines, x, y, maxX, maxY, -1, font);
    }

    public static void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        int z = 0;
        float f = (float)(startColor >> 24 & 255) / 255.0F;
        float f1 = (float)(startColor >> 16 & 255) / 255.0F;
        float f2 = (float)(startColor >> 8 & 255) / 255.0F;
        float f3 = (float)(startColor & 255) / 255.0F;
        float f4 = (float)(endColor >> 24 & 255) / 255.0F;
        float f5 = (float)(endColor >> 16 & 255) / 255.0F;
        float f6 = (float)(endColor >> 8 & 255) / 255.0F;
        float f7 = (float)(endColor & 255) / 255.0F;
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(right, top, z).color(f1, f2, f3, f).endVertex();
        bufferbuilder.pos(left, top, z).color(f1, f2, f3, f).endVertex();
        bufferbuilder.pos(left, bottom, z).color(f5, f6, f7, f4).endVertex();
        bufferbuilder.pos(right, bottom, z).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    protected static void drawScaledCustomSizeModalRect(
            double x,
            double y,
            float u,
            float v,
            double uWidth,
            double vHeight,
            double width,
            double height,
            double tileWidth,
            double tileHeight) {
        double f = 1.0F / tileWidth;
        double f1 = 1.0F / tileHeight;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder
                .pos(x, y + height, 0.0D)
                .tex(u * f, (v + (float) vHeight) * f1)
                .endVertex();
        bufferbuilder
                .pos(x + width, y + height, 0.0D)
                .tex((u + (float) uWidth) * f, (v + (float) vHeight) * f1)
                .endVertex();
        bufferbuilder
                .pos(x + width, y, 0.0D)
                .tex((u + (float) uWidth) * f, v * f1)
                .endVertex();
        bufferbuilder
                .pos(x, y, 0.0D)
                .tex(u * f, v * f1)
                .endVertex();
        tessellator.draw();
    }
}