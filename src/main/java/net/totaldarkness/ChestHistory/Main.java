package net.totaldarkness.ChestHistory;

import net.totaldarkness.ChestHistory.client.gui.ChestHistory;
import net.totaldarkness.ChestHistory.client.services.ChestGuiService;
import net.totaldarkness.ChestHistory.client.services.RenderEventService;
import net.totaldarkness.ChestHistory.client.events.KeyboardEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = "chesthistory", name = "Chest History", version = "0.0.1", acceptedMinecraftVersions = MinecraftForge.MC_VERSION, clientSideOnly = true)
public class Main {
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {}

    @EventHandler
    public void init(FMLInitializationEvent event) {
    }
    
    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
    	MinecraftForge.EVENT_BUS.register(new KeyboardEvent());
        MinecraftForge.EVENT_BUS.register(new RenderEventService());
        MinecraftForge.EVENT_BUS.register(new ChestHistory());
        MinecraftForge.EVENT_BUS.register(new ChestGuiService());
    }
}