package net.totaldarkness.ChestHistory.client.services;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.totaldarkness.ChestHistory.client.events.Render2DEvent;
import net.totaldarkness.ChestHistory.client.events.RenderEvent;
import net.totaldarkness.ChestHistory.client.util.render.GeometryTessellator;
import org.lwjgl.opengl.GL11;

import static net.totaldarkness.ChestHistory.client.util.Helper.getViewEntity;

public class RenderEventService{

    private static final GeometryTessellator TESSELLATOR = new GeometryTessellator();

    public RenderEventService() {
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableDepth();

        GlStateManager.glLineWidth(1.f);

        Vec3d renderPos = getInterpolatedPos(getViewEntity(), event.getPartialTicks());

        RenderEvent e = new RenderEvent(TESSELLATOR, renderPos, event.getPartialTicks());
        e.resetTranslation();
        MinecraftForge.EVENT_BUS.post(e);

        GlStateManager.glLineWidth(1.f);

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.popMatrix();
    }

    public Vec3d getInterpolatedPos(Entity entity, double ticks) {
        return new Vec3d(entity.lastTickPosX, entity.lastTickPosY, entity.lastTickPosZ)
                .add(getInterpolatedAmount(entity, ticks, ticks, ticks));
    }

    public static Vec3d getInterpolatedAmount(Entity entity, double x, double y, double z) {
        return new Vec3d(
                (entity.posX - entity.lastTickPosX) * x,
                (entity.posY - entity.lastTickPosY) * y,
                (entity.posZ - entity.lastTickPosZ) * z);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderGameOverlayEvent(final RenderGameOverlayEvent.Text event) {
        if (event.getType().equals(RenderGameOverlayEvent.ElementType.TEXT)) {
            MinecraftForge.EVENT_BUS.post(new Render2DEvent(event.getPartialTicks()));
            GlStateManager.color(1.f, 1.f, 1.f, 1.f); // reset color
        }
    }
}

