package net.totaldarkness.ChestHistory.client.events;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.totaldarkness.ChestHistory.client.util.render.SurfaceBuilder;

import static net.totaldarkness.ChestHistory.client.util.Helper.getMinecraft;

public class Render2DEvent extends Event {

    private final ScaledResolution resolution = new ScaledResolution(getMinecraft());
    private final SurfaceBuilder surfaceBuilder = new SurfaceBuilder();
    private final float partialTicks;

    public Render2DEvent(float partialTicks) {
        this.partialTicks = partialTicks;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public double getScreenWidth() {
        return resolution.getScaledWidth_double();
    }

    public double getScreenHeight() {
        return resolution.getScaledHeight_double();
    }

    public SurfaceBuilder getSurfaceBuilder() {
        return surfaceBuilder;
    }
}
