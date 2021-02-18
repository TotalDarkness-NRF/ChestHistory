package net.totaldarkness.ChestHistory.client.gui;

import net.totaldarkness.ChestHistory.client.util.render.SurfaceHelper;
import net.totaldarkness.ChestHistory.client.util.Helper;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ServerListGui extends GuiScreen {

    public ServerListGui() {}

    @Override
    public void initGui() {
        int x = 20;
        int y = 20;
        for (String serverIP : getServerList()) {
            int width = SurfaceHelper.getTextWidth(serverIP) + 10;
            if (x + width + 1 >= ChestGui.getInstance().maxX) {
                x = 20;
                y += 21;
            }

            this.buttonList.add(new GuiButton(200, x, y, width, 20, serverIP));
            x += width + 1;
        }

        this.buttonList.add(new GuiButton(50, ChestGui.getInstance().maxX / 2 - 30, 0, 60, 20, "Exit"));
        this.buttonList.add(new GuiButton(50, ChestGui.getInstance().maxX / 2 - 30, ChestGui.getInstance().maxY - 20, 60, 20, "Exit"));
    }

    /**
     * Render the screen for ChestGui
     *
     * @param mouseX       The x position of the mouse
     * @param mouseY       The y position of the mouse
     * @param partialTicks The ticks
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        try {
            drawDefaultBackground();
        } catch (Exception ignored) {
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        updateButtons(mouseX, mouseY);
    }

    public List<String> getServerList() {
        List<String> serverList = new LinkedList<>();
        Path path = Helper.getFileManager().getMkBaseResolve("/chests/");
        try {
            serverList = Files.walk(path).filter(Files::isDirectory).map(filePath -> filePath.getFileName().toString()).collect(Collectors.toList());
            serverList.remove(0); // Remove parent directory
            serverList.remove(Helper.getServerIP()); // Do not include current server
        } catch (Exception ignored) {
        }
        Collections.sort(serverList);
        return serverList;
    }

    public void updateButtons(int mouseX, int mouseY) {
        for (GuiButton button : this.buttonList) {
            if (button.id == 200 && button.isMouseOver())
                drawHoveringText("Click to view saved chest for " + button.displayString, mouseX, mouseY);
            else if (button.id == 50 && button.isMouseOver())
                drawHoveringText("Click to exit", mouseX, mouseY);
        }
    }

    /**
     * When a button is clicked perform a specific action for that button
     *
     * @param button The button that was clicked
     */
    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 200) Helper.getMinecraft().displayGuiScreen(ChestGui.getCachedGui(button.displayString));
        else if (button.id == 50) onClose();
    }

    /**
     * When gui is closed send back to either terminal or normal game
     *
     */
    private void onClose() {
        Helper.getMinecraft().displayGuiScreen(ChestGui.getInstance());
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        if (keyCode == Keyboard.KEY_ESCAPE) onClose();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}