package net.totaldarkness.ChestHistory.client.gui;

import net.totaldarkness.ChestHistory.client.settings.Setting;
import net.totaldarkness.ChestHistory.client.settings.SettingEnum;
import net.totaldarkness.ChestHistory.client.util.Helper;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings("unchecked")
public class SettingsGui extends GuiScreen {
    private final ArrayList<Setting> commandList;
    private ChestGui lastScreen;

    public SettingsGui(ChestGui lastScreen) {
        this.lastScreen = lastScreen;
        commandList = new ArrayList<>();
        commandList.addAll(Setting.list);
        Collections.sort(commandList);
    }

    @Override
    public void initGui() {
        int maxX = ChestGui.getInstance().maxX;
        int y = (ChestGui.getInstance().maxY / 2) - commandList.size() * 11;
        for (Setting<?> setting : commandList) {
            this.buttonList.add(new GuiButton(999, (maxX - 150) / 2, y, 150, 20, setting.getName()));
            y += 21;
        }
        this.buttonList.add(new GuiButton(50, maxX / 2 - 30, 0, 60, 20, "Exit"));
        this.buttonList.add(new GuiButton(50, maxX / 2 - 30, ChestGui.getInstance().maxY - 20, 60, 20, "Exit"));
        this.buttonList.add(new GuiButton(1, (maxX - 150) / 2, y, 150, 20, "default settings"));
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
        } catch (Exception ignored) {}

        super.drawScreen(mouseX, mouseY, partialTicks);
        updateButtons(mouseX, mouseY);
    }


    public void updateButtons(int mouseX, int mouseY) {
        for (GuiButton button : this.buttonList) {
            if (button.id == 999) {
                String name = button.displayString;
                if (name.contains(":"))
                    name = name.substring(0, name.indexOf(":"));

                Setting<?> setting = getSetting(name);
                button.displayString = String.format("%s: %s", setting.getName(), setting.get());
                if (button.isMouseOver())
                    drawHoveringText(setting.getDescription(), button.x + button.width, button.y + button.height);
            } else if (button.id == 50 && button.isMouseOver())
                drawHoveringText("Click to exit", mouseX, mouseY);
            else if (button.id == 1 && button.isMouseOver())
                drawHoveringText("Change all settings to default.", button.x + button.width, button.y + button.height);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 || mouseButton == 1) {
            for (int i = 0; i < this.buttonList.size(); ++i) {
                GuiButton guibutton = this.buttonList.get(i);
                if (guibutton.mousePressed(this.mc, mouseX, mouseY)) {
                    net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent.Pre event = new net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent.Pre(this, guibutton, this.buttonList);
                    if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event))
                        break;
                    guibutton = event.getButton();
                    this.selectedButton = guibutton;
                    guibutton.playPressSound(this.mc.getSoundHandler());
                    actionPerformed(guibutton, mouseButton);
                    if (this.equals(this.mc.currentScreen))
                        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent.Post(this, event.getButton(), this.buttonList));
                }
            }
        }
    }

    /**
     * When a button is clicked perform a specific action for that button
     *
     * @param button The button that was clicked
     */
    protected void actionPerformed(GuiButton button, int mouseButton) {
        if (button.id == 1) {
            for (Setting<?> setting : commandList)
                getSetting(setting.getName()).reset(false);
        } else if (button.id == 50) onClose();
        else if (button.displayString.contains(":"))
            settingsSet(getSetting(button.displayString.substring(0, button.displayString.indexOf(":"))), mouseButton == 0 ? 1 : -1);
    }

    public void settingsSet(Setting<?> setting, int direction) {
        direction = direction >= 0 ? 1 : -1;
        if (setting.getDefault() instanceof Integer) {
            setting.set(setting.getAsInteger() + direction, false);
        } else if (setting.getDefault() instanceof Long) {
            setting.set(setting.getAsLong() + direction, false);
        } else if (setting.getDefault() instanceof Float) {
            setting.set(setting.getAsFloat() + direction, false);
        } else if (setting.getDefault() instanceof Double) {
            setting.set(setting.getAsDouble() + direction, false);
        } else if (setting.getDefault() instanceof Boolean) {
            setting.set(!setting.getAsBoolean(), false);
        } else if (setting.getDefault() instanceof Enum){
            setting.set(SettingEnum.getOrdinal(direction, (Enum) setting.get()));
            ChestHistory.INSTANCE.reSortChestList();
            lastScreen = new ChestGui(lastScreen);
        }
    }

    private Setting<?> getSetting(String name) {
        return Setting.getSetting(name);
    }

    private void onClose() {
        Helper.getMinecraft().displayGuiScreen(lastScreen);
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