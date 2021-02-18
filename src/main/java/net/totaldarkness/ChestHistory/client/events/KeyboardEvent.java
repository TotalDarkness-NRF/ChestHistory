package net.totaldarkness.ChestHistory.client.events;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.totaldarkness.ChestHistory.client.gui.ChestGui;
import net.totaldarkness.ChestHistory.client.services.ChestGuiService;
import net.totaldarkness.ChestHistory.client.util.Helper;

public class KeyboardEvent {

	@SubscribeEvent
	public void onKeyPress(InputEvent.KeyInputEvent event) {
		if(Helper.getCurrentScreen() == null && ChestGuiService.bind.isKeyDown())
			Helper.getMinecraft().displayGuiScreen(ChestGui.getCachedGui(Helper.getServerIP()));
	}
}
