package net.totaldarkness.ChestHistory.client.gui;

import net.totaldarkness.ChestHistory.client.util.color.Colors;
import net.totaldarkness.ChestHistory.client.util.render.SurfaceHelper;
import net.totaldarkness.ChestHistory.client.util.AutoRemoveSet;
import net.totaldarkness.ChestHistory.client.util.SimpleTimer;
import net.totaldarkness.ChestHistory.client.util.ChestElement;
import net.totaldarkness.ChestHistory.client.util.Helper;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

import static net.totaldarkness.ChestHistory.client.gui.ChestHistory.*;
import static net.totaldarkness.ChestHistory.client.services.ChestGuiService.*;

public class ChestGui extends GuiScreen {
    private static ChestGui INSTANCE;
    public static boolean showMarked;
    private final ResourceLocation box = new ResourceLocation("textures/gui/container/generic_54.png");
    private final String serverIP, checked = "\u29BB", unchecked = "\u25EF";
    private String search;
    private List<int[]> pageIndexes, startPositions;
    private ScaledResolution scaledResolution;
    private final List<ChestElement> chestList;
    private List<ChestElement> chestsToRender;
    public int maxX, maxY, page;
    private GuiTextField indexField, searchField;
    private SimpleTimer timer;
    private final List<GuiButton> buttons;
    private final static AutoRemoveSet<ChestGui> cachedGuis = new AutoRemoveSet<>(1000 * 60 * 10); // Keeps old guis for max 10 mins

    private ChestGui() {
        this(ChestHistory.INSTANCE.isEnabled() ? getChestsList() : ChestHistory.INSTANCE.getChestList(Helper.getServerIP()), Helper.getServerIP());
    }

    public ChestGui(List<ChestElement> chestList, String serverIP) {
        this.chestList = chestList;
        this.buttons = new LinkedList<>();
        this.scaledResolution = new ScaledResolution(Helper.getMinecraft());
        this.serverIP = serverIP;
        this.timer = new SimpleTimer();
        search = "";
        setUpButtons();
        initGui();
    }

    public ChestGui(ChestGui chestGui) {
        // Need this for settings gui so that if I change sort mode or direction it updates
        this.chestList = sortChestList(chestGui.chestList);
        this.buttons = chestGui.buttons;
        this.scaledResolution = new ScaledResolution(Helper.getMinecraft());
        this.serverIP = chestGui.serverIP;
        this.timer = chestGui.timer;
        this.search = chestGui.search;
        this.indexField = chestGui.indexField;
        this.searchField = chestGui.searchField;
        this.page = chestGui.page;
        this.maxX = chestGui.maxX;
        this.maxY = chestGui.maxY;
        initGui();
    }

    /**
     * Gets and instance of this object
     * @return An instance of this object
     */
    public static ChestGui getInstance() {
        return (INSTANCE == null) ? (INSTANCE = new ChestGui()) : INSTANCE;
    }

    public static void resetInstance() {
        INSTANCE = null;
    }

    /**
     * Initializes the gui screen, and calculates start positions of all pages
     * and adds buttons for each one
     */
    @Override
    public void initGui() {
        buttonList.clear();
        chestsToRender = !showMarked ? this.chestList : ChestElement.getAllMarked(this.chestList);
        if (!search.isEmpty() && (getSearchText() || getSearchItem())) {
            final List<ChestElement> chestsInSearch = new LinkedList<>();
            for (ChestElement chest : chestsToRender) {
                if (getSearchText() && (inSearch(chest.getText()) || inSearch(chest.getId()))) {
                    chestsInSearch.add(chest);
                    continue; // Don't re add
                }

                if (getSearchItem()) {
                    for (ItemStack item : chest.getItems()) {
                        if (inSearch(item.getDisplayName()) || inSearch(item.getUnlocalizedName())) {
                            chestsInSearch.add(chest);
                            break;
                        }
                    }
                }
            }
            chestsToRender = chestsInSearch;
        }
        getStartInfo();

        if (getScale() >= 16) {
            for (int page = 0; page < pageIndexes.size(); page++) {
                for (int index = pageIndexes.get(page)[0]; index <= pageIndexes.get(page)[1]; index++) {
                    if (index >= startPositions.size() || index >= chestsToRender.size()) break;
                    ChestElement chest = chestsToRender.get(index);
                    int[] position = startPositions.get(index);
                    int x = position[0];
                    int y = position[1];
                    String text = chest.markUntilUpdate ? checked : unchecked; // "⦻" : "◯"
                    if (this.page == page)
                        buttonList.add(new GuiButton(page + 1, (int) (x + multiplyFactor(140)), (int) (y + multiplyFactor(2)), 10, 10, text));
                    else
                        buttonList.add(makeButtonInvisible(new GuiButton(page + 1, (int) (x + multiplyFactor(140)), (int) (y + multiplyFactor(2)), 10, 10, text)));
                    text = chest.pin ? checked : unchecked;
                    if (this.page == page)
                        buttonList.add(new GuiButton(-(page + 1), (int) (x + multiplyFactor(155)), (int) (y + multiplyFactor(2)), 10, 10, text));
                    else
                        buttonList.add(makeButtonInvisible(new GuiButton(-(page + 1), (int) (x + multiplyFactor(155)), (int) (y + multiplyFactor(2)), 10, 10, text)));
                }
            }
        }
        buttons.get(buttons.size() - 1).visible = showMarked;
        setUpTextFields();
        resetPositions();
        buttonList.addAll(buttons);
    }

    private boolean inSearch(String find) {
        if (getSearchCaseSensitive()) return find.contains(search);
        else return find.toLowerCase().contains(search.toLowerCase());
    }

    /**
     * Used to set up a list of buttons that we will use often and require its info not to change
     */
    private void setUpButtons() {
        buttons.add(new GuiButton(99, 0, 0, 70, 20, !showMarked ? "Show marked" : "Show all")); // 0
        buttons.add(makeButtonInvisible(new GuiButton(100, maxX / 2 + 40, maxY - 20, 20, 20, ">"))); // 1
        buttons.add(makeButtonInvisible(new GuiButton(100, maxX / 2 + 40, 0, 20, 20, ">"))); // 2
        buttons.add(makeButtonInvisible(new GuiButton(0, maxX / 2 - 60, maxY - 20, 20, 20, "<"))); // 3;
        buttons.add(makeButtonInvisible(new GuiButton(0, maxX / 2 - 60, 0, 20, 20, "<"))); // 4
        buttons.add(new GuiButton(50, maxX / 2 - 30, 0, 60, 20, "/")); //5
        buttons.add(new GuiButton(50, maxX / 2 - 30, maxY - 20, 60, 20, "/")); // 6
        buttons.add(new GuiButton(200, 71, 0, 70, 20, "Servers")); // 7
        buttons.add(new GuiButton(200, 142, 0, 70, 20, "Settings")); // 8
        buttons.add(new GuiButton(88, 0, maxY - 20, 70, 20, "Unmark all")); // 9
    }

    private void resetPositions() {
        // If scaled res changes, this prepares buttons in correct positions
        buttons.get(0).x = 0;          buttons.get(0).y = 0;
        buttons.get(1).x = maxX / 2 + 40; buttons.get(1).y = maxY - 20;
        buttons.get(2).x = maxX / 2 + 40; buttons.get(2).y = 0;
        buttons.get(3).x = maxX / 2 - 60; buttons.get(3).y = maxY - 20;
        buttons.get(4).x = maxX / 2 - 60; buttons.get(4).y = 0;
        buttons.get(5).x = maxX / 2 - 30; buttons.get(5).y = 0;
        buttons.get(6).x = maxX / 2 - 30; buttons.get(6).y = maxY - 20;
        buttons.get(7).x = 71;            buttons.get(7).y = 0;
        buttons.get(8).x = 142;           buttons.get(8).y = 0;
        buttons.get(9).x = 0;            buttons.get(9).y = maxY - 20;

        indexField.x = maxX / 2 - 30;     indexField.y = 1;
        searchField.x = 213;              searchField.y = 2;
    }

    private GuiButton makeButtonInvisible(GuiButton button) {
        button.visible = false;
        return button;
    }

    private void setUpTextFields() {
        if (indexField != null || searchField != null) return;
        int textLength = SurfaceHelper.getTextWidth(String.format("%s/%s", pageIndexes.size(), (pageIndexes.size())));
        this.indexField = new GuiTextField(0, Helper.getMinecraft().fontRenderer, maxX / 2 - 30, 2, textLength + 6, 16);
        this.indexField.setMaxStringLength((pageIndexes.size() + "").length());
        this.indexField.setText("");
        this.indexField.setFocused(false);
        this.indexField.setVisible(false);
        this.searchField = new GuiTextField(1, Helper.getMinecraft().fontRenderer, 213, 2, 100, 16);
        this.searchField.setMaxStringLength(Integer.MAX_VALUE);
        this.searchField.setText("");
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
        } catch (Exception ignored) { }

        scaledResolution = new ScaledResolution(Helper.getMinecraft());

        indexField.drawTextBox();
        if (indexField.getVisible()) {
            if (!chestsToRender.isEmpty()) {
                String text = String.format("%s/%s", indexField.getText().isEmpty() ? page + 1 : indexField.getText(), pageIndexes.size());
                SurfaceHelper.drawText(text, indexField.x + 4, indexField.y + 4, -1);
            } else SurfaceHelper.drawText(String.format("%d/%d", 0 ,0), indexField.x + 4, indexField.y + 4, -1);
        }

        searchField.drawTextBox();
        if (searchField.getText().isEmpty() && searchField.getVisible())
            SurfaceHelper.drawTextShadow("Search...", searchField.x + 4, searchField.y + 4, Colors.LIGHT_GRAY.toBuffer());

        if (pageIndexes.size() != 0 && startPositions.size() != 0) {
            for (int index = pageIndexes.get(page)[0]; index <= pageIndexes.get(page)[1]; index++) {
                if (index >= startPositions.size() || index >= chestsToRender.size()) break;
                renderChest(startPositions.get(index), chestsToRender.get(index));
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        updateButtons(mouseX, mouseY);
        drawMarkedList(maxX + 12, 0);
        renderToolTips(mouseX, mouseY);
    }

    /**
     * Draw a small side list of all marked chests
     * @param x The x position
     * @param y The y position
     */
    public void drawMarkedList(int x, int y) {
        if (showMarked && getMarkedChestsSetting()) {
            List<String> text = new LinkedList<>();
            text.add(TextFormatting.GOLD + "All marked chests:");
            int num = 0;
            for (ChestElement chest : ChestElement.getAllMarked(chestsToRender)) {
                if (num++ % 2 == 0) text.add(TextFormatting.AQUA + (getCensorCoordsSetting() ? chest.getChestName() : chest.getText()));
                else text.add(TextFormatting.LIGHT_PURPLE + (getCensorCoordsSetting() ? chest.getChestName() : chest.getText()));
            }
            if (text.size() == 1) return;
            SurfaceHelper.drawHoveringText(text, x, y);
        }
    }

    /**
     * Render a specified chests at a certain position on screen
     * @param startPosition An array containing the x and y positions
     * @param chest         The chest to render
     */
    public void renderChest(int[] startPosition, ChestElement chest) {
        if (startPosition.length > 2) return;
        int x = startPosition[0], y = startPosition[1], slot = 0;
        double itemOffsetX = multiplyFactor(5), itemOffsetY = multiplyFactor(14), slotX, slotY = 0;
        boxRender((int) (x * (1 / getFactor())), (int) (y * (1 / getFactor())), chest);
        for (ItemStack item : chest.getItems()) {
            slotX = x + (slot % 9) * multiplyFactor(18);
            slotY = y + (double) (slot / 9) * multiplyFactor(18);
            slot++;
            SurfaceHelper.drawItemWithOverlay(item, slotX + itemOffsetX, slotY + itemOffsetY, getScale());
        }
        renderText((int) (x + itemOffsetX), (int) (y + multiplyFactor(3)), chest);
        renderTimeText(x, (int) (slotY + multiplyFactor(35)), chest);
    }

    /**
     * Render tooltips for the item that you are hovering over
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     */
    private void renderToolTips(int mouseX, int mouseY) {
        if (pageIndexes.size() == 0 || startPositions.size() == 0) return;

        double itemOffsetX = multiplyFactor(4), itemOffsetY = multiplyFactor(13);
        for (int index = pageIndexes.get(page)[0]; index <= pageIndexes.get(page)[1]; index++) {
            if (index >= startPositions.size() || index >= chestsToRender.size()) break;
            int[] position = startPositions.get(index);
            int slot = 0, startX = position[0], startY = position[1];
            double slotX, slotY;

            ChestElement chest = chestsToRender.get(index);

            for (ItemStack item : chest.getItems()) {
                slotX = startX + (slot % 9) * multiplyFactor(18);
                slotY = startY + (double) (slot / 9) * multiplyFactor(18);
                slot++;
                if ((mouseX >= slotX + itemOffsetX && mouseX < slotX + itemOffsetX + multiplyFactor(18))
                        && (mouseY >= slotY + itemOffsetY && mouseY < slotY + itemOffsetY + multiplyFactor(18))
                        && !item.isEmpty()) {
                    super.renderToolTip(item, mouseX, mouseY);
                }
            }

            if (mouseX >= startX && mouseX < startX + multiplyFactor(140) && mouseY >= startY && mouseY <= startY + multiplyFactor(12)) {
                List<String> text = new LinkedList<>();
                ITextComponent name = new TextComponentString(chest.getChestName());
                name.getStyle().setColor(chest.isEnderChest() ? TextFormatting.DARK_PURPLE : TextFormatting.GOLD).setBold(true);
                text.add(name.getFormattedText());
                text.add(TextFormatting.GRAY + "minecraft:" + chest.getId());
                if (!chest.isEnderChest()) {
                    text.add(TextFormatting.AQUA + "[XYZ] " + (getCensorCoordsSetting() ? TextFormatting.OBFUSCATED : "") + chest.formatPosition());
                    text.add("Dimension: " + Helper.getDimensionName(chest.getDimension()));
                }
                text.add(TextFormatting.RED + "Middle click to remove");
                drawHoveringText(text, mouseX, mouseY);
            }


            if (getLastUpdatedSetting() || getTimeElapsedSetting()) {
                int yOffset = getNextYAmount(chest.getItems().size());
                if (mouseX >= startX && mouseX <= startX + multiplyFactor(169) &&
                        mouseY >= startY + yOffset - multiplyFactor(5) && mouseY <= startY + yOffset + multiplyFactor(8)) {
                    List<String> text = new LinkedList<>();
                    if (getLastUpdatedSetting())
                        text.add("Time last updated: " + getFormattedDate(chest.getTime()));
                    if (getTimeElapsedSetting())
                        text.add("Time since updated: " + getTimeSinceUpdate(chest.getTime()));
                    drawHoveringText(text, mouseX, mouseY);
                }
            }
        }
    }

    /**
     * Render the text on the chest box
     * NOTE: used in chest gui (array of chests screen) and chest boxes
     *
     * @param x     The x coordinate to render on screen
     * @param y     The y coordinate to render on screen
     * @param chest The chest element for the chest name
     */
    public void renderText(int x, int y, ChestElement chest) {
        if (!getTextSetting()) return;
        SurfaceHelper.drawText(!getCensorCoordsSetting() ? chest.getText() : chest.getChestName(), x, y, getTextColor(chest), getFactor());
    }

    /**
     * Render either or both the elapsed time since updated and the time it was updated
     * NOTE: used in chest gui (array of chests screen) and chest boxes
     *
     * @param x     The x coordinate to render on screen
     * @param y     The y coordinate to render on screen
     * @param chest The chest element for the chest name
     */
    public void renderTimeText(int x, int y, ChestElement chest) {
        if (!getTimeElapsedSetting() && !getLastUpdatedSetting()) return;
        long time = chest.getTime();
        final String timeSplitter = " \u0489\u01C0\u0489 ";
        String text = "";
        if (getLastUpdatedSetting()) text += getFormattedDate(time) + timeSplitter;
        if (getTimeElapsedSetting()) text += getTimeSinceUpdate(time);
        else text = text.replace(timeSplitter, "");
        SurfaceHelper.drawText(text, (int) (x + multiplyFactor(5)), y, getTextColor(chest), getFactor());
    }

    /**
     * A helper method to format the date that a chest was opened
     * @param time The time the chest was opened
     * @return The formatted date that the chest was opened
     */
    public String getFormattedDate(final long time) {
        String format;
        long timeSince = System.currentTimeMillis() - time;
        if (timeSince >= 3.154e10) format = "[MMMM dd, yyyy] hh:mm:ss";
        else if (timeSince >= 2.628e9) format = "[MMMM dd] hh:mm:ss";
        else if (timeSince >= 6.048e8) format = "[EEEE] hh:mm:ss";
        else format = "h:mm:ss";
        return new SimpleDateFormat(format).format(time);
    }

    /**
     * Calculate the time elapsed since the chest was opened, then format it
     * @param time The time the chest was opened
     * @return The formatted elapsed time
     */
    public String getTimeSinceUpdate(final long time) {
        String format;
        long timeSince = System.currentTimeMillis() - time;
        if (timeSince >= 3.154e10) format = "y:M:dd:HH:mm:ss";
        else if (timeSince >= 2.628e9) format = "M:dd:HH:mm:ss";
        else if (timeSince >= 8.64e7) format = "d:HH:mm:ss";
        else if (timeSince >= 3.6e6) format = "H:mm:ss";
        else if (timeSince >= 6e4) format = "m:ss";
        else format = "s";
        return DurationFormatUtils.formatPeriod(time, System.currentTimeMillis(), format);
    }

    /**
     * Render a chest box for a chest
     *
     * @param x     The x coordinate to render on screen
     * @param y     The y coordinate to render on screen
     * @param chest The chest element to render
     */
    public void boxRender(int x, int y, ChestElement chest) {
        GlStateManager.pushMatrix();
        GlStateManager.disableBlend();
        GlStateManager.disableDepth();
        GlStateManager.color(1.f, 1.f, 1.f, 1.f); //This resets colors
        GlStateManager.scale(getFactor(), getFactor(), getFactor());
        Helper.getMinecraft().renderEngine.bindTexture(box);
        int height = (chest.getItems().size() >= 54 ? 121 : 67);
        // Main chest with top
        Helper.getMinecraft().ingameGUI.drawTexturedModalRect(x, y, 3, 4, 169, height);
        // Bottom part of chest
        Helper.getMinecraft().ingameGUI.drawTexturedModalRect(x, y + height, 3, 125, 169, 13);
        height += 13;
        int color = getBackgroundColor(chest.getId());
        if (color != -666) SurfaceHelper.drawOutlinedRectShaded(x, y, 169, height, color, 210, 1);
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    /**
     * Get the color for the background of the chest box
     * NOTE: used in chest gui (array of chests screen) and chest boxes
     *
     * @param id The item id of the chest element
     * @return An integer representation for rgb color
     */
    private int getBackgroundColor(String id) {
        if (id == null) return -666;
        id = id.replaceAll("_", "").replace("shulkerbox", "");
        switch (id) {
            case "purple": return 8991416;
            case "white": return 16383998;
            case "orange": return 16351261;
            case "magenta": return 13061821;
            case "lightblue": return 3847130;
            case "yellow": return 16701501;
            case "lime": return 8439583;
            case "pink": return 15961002;
            case "gray": return 4673362;
            case "silver": return 10329495;
            case "cyan": return 1481884;
            case "blue": return 3949738;
            case "brown": return 8606770;
            case "green": return 6192150;
            case "red": return 11546150;
            case "black": return 0;
            case "enderchest": return 3407924;
            default: return -666;
        }
    }

    /**
     * Get the text color for the chest element
     * NOTE: used in chest gui (array of chests screen) and chest boxes
     * @param chest The chest element to get the color
     * @return An integer representation for rgb color
     */
    public int getTextColor(ChestElement chest) {
        int color = getBackgroundColor(chest.getId());
        if (chest.isEnderChest()) color = -62886;
        else if (color == 0 || color == 4673362) color = -1;
        else color = 4210752;
        return color;
    }

    /**
     * Calculate all the start positions and the page indexes
     */
    private void getStartInfo() {
        startPositions = new LinkedList<>();
        pageIndexes = new LinkedList<>();
        int startX = 21, extraX = 0;
        final int timeExtra = (int) multiplyFactor(10);
        maxY = scaledResolution.getScaledHeight();
        maxX = scaledResolution.getScaledWidth();
        if (showMarked) {
            for (ChestElement chestElement : chestsToRender) {
                int width = SurfaceHelper.getTextWidth(getCensorCoordsSetting() ? chestElement.getChestName() : chestElement.getText());
                if (width > extraX)
                    extraX = width;
            }
        } else {
            for (int i = 0; i < chestsToRender.size(); i++) {
                startX += multiplyFactor(171);
                if (startX + multiplyFactor(162) + 21 > maxX || i + 1 >= chestsToRender.size()) {
                    extraX = (int) ((3 * maxX - 3 * startX + multiplyFactor(162) + 21) / 4);
                    if (extraX >= maxX / 3) extraX = 0;
                    break;
                }
            }
        }

        int start = 0;
        startX = showMarked ? 21 : 21 + extraX;
        int startY = 25;
        if (getGuiDirection() == ChestGuiDirection.HORIZONTAL) {
            int highestRows = 4;
            for (int index = 0; index < chestsToRender.size(); index++) {
                startPositions.add(index, new int[]{startX, startY});
                startX += multiplyFactor(171);
                if (chestsToRender.get(index).getItems().size() >= 54) {
                    highestRows = 7;
                }
                if (showMarked) {
                    if (startX + 21 + multiplyFactor(180) + extraX > maxX) {
                        startX = 21;
                        startY += highestRows * multiplyFactor(18) + timeExtra;
                        highestRows = 4;
                    }
                } else {
                    if (startX + 21 + multiplyFactor(180) > maxX) {
                        startX = 26 + extraX;
                        startY += highestRows * multiplyFactor(18) + timeExtra;
                        highestRows = 4;
                    }
                }
                if (startY + 25 + multiplyFactor(126) > maxY) {
                    pageIndexes.add(new int[]{start, index});
                    startX = 21 + extraX;
                    startY = 25;
                    start = index + 1;
                }
                if (index + 1 >= chestsToRender.size()) {
                    pageIndexes.add(new int[]{start, index});
                    break;
                }
            }
        } else {
            for (int index = 0; index < chestsToRender.size(); index++) {
                ChestElement chest = chestsToRender.get(index);
                List<ItemStack> items = chest.getItems();
                startPositions.add(index, new int[]{startX, startY});
                startY += getNextYAmount(items.size()) + timeExtra;
                if (index + 1 >= chestsToRender.size()) {
                    pageIndexes.add(new int[]{start, index});
                    break;
                }
                int nextSize = chestsToRender.get(index + 1).getItems().size();
                if (startY + getNextYAmount(nextSize) + timeExtra > maxY) {
                    startY = 25;
                    startX += multiplyFactor(171);
                }
                if (showMarked) {
                    if (startX + 21 + multiplyFactor(153) + extraX > maxX) {
                        pageIndexes.add(new int[]{start, index});
                        startX = 21;
                        startY = 25;
                        start = index + 1;
                    }
                } else {
                    if (startX + 21 + multiplyFactor(153) > maxX) {
                        pageIndexes.add(new int[]{start, index});
                        startX = 21 + extraX;
                        startY = 25;
                        start = index + 1;
                    }
                }
            }
        }

        if (page >= pageIndexes.size() || page < 0) page = 0;
    }

    /**
     * Calculate the nextYAmount to increase by
     * @param size the size of the chest
     * @return The next y amount to add
     */
    private int getNextYAmount(int size) {
        return (int) (((size / 9) + 1) * multiplyFactor(18));
    }

    /**
     * Update the buttons text, positions, and functions, as well as any hovering text
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     */
    private void updateButtons(int mouseX, int mouseY) {
        for (GuiButton button : this.buttonList) {
            if (button.displayString.equals("<")) {
                button.visible = page != 0;
                if (button.isMouseOver() && button.visible) drawHoveringText("Previous Page", mouseX, mouseY);
            } else if (button.displayString.equals(">")) {
                button.visible = page <= pageIndexes.size() - 2;
                if (button.isMouseOver() && button.visible) drawHoveringText("Next Page", mouseX, mouseY);
            } else if (button.displayString.contains("/")) {
                if (!button.visible && timer.isStarted() && timer.hasTimeElapsed(3000)) {
                    button.visible = true;
                    indexField.setVisible(false);
                    indexField.setText("");
                    timer = new SimpleTimer();
                }
                button.displayString = String.format("%s/%s", pageIndexes.size() == 0 ? 0 : page + 1, pageIndexes.size());
                if (button.isMouseOver() && button.visible) {
                    List<String> text = new LinkedList<>();
                    text.add("Left click to exit");
                    text.add("Right click to enter page number");
                    drawHoveringText(text, mouseX, mouseY);
                }
            } else if (button.id == page + 1) {
                button.visible = !buttonList.get(buttonList.indexOf(button) + 1).displayString.equals(checked);
                if (button.isMouseOver()) drawHoveringText("Click to mark until opened", mouseX, mouseY);
            } else if (button.id == -(page + 1)) {
                button.visible = true;
                if (button.isMouseOver()) drawHoveringText("Click to pin", mouseX, mouseY);
            } else if (button.displayString.contains("Show")) {
                button.x = 0; button.y = 0;
                button.displayString = !showMarked ? "Show marked" : "Show all";
                if (button.isMouseOver()) {
                    if (showMarked) drawHoveringText("Click to show all chests", mouseX, mouseY);
                    else drawHoveringText("Click to show marked chests", mouseX, mouseY);
                }
            } else if (button.displayString.equals("Unmark all")) {
                if (button.isMouseOver()) drawHoveringText("Click to unmark all chests", mouseX, mouseY);
            } else if (button.displayString.equals("Servers")) {
                if (button.isMouseOver()) drawHoveringText("Click to show all servers with saved chests", mouseX, mouseY);
            } else if (button.displayString.equals("Settings")) {
                if (button.isMouseOver()) drawHoveringText("Click to change setting", mouseX, mouseY);
            } else button.visible = false;
        }
    }

    /**
     * When a button is clicked perform a specific action for that button
     * @param button The button that was clicked
     */
    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.displayString.equals(">")) {
            page = page >= pageIndexes.size() - 1 ? pageIndexes.size() - 1 : page + 1;
            indexField.setText(page + 1 + "");
        } else if (button.displayString.equals("<")) {
            page = page < 0 ? 0 : page - 1;
            indexField.setText(page + 1 + "");
        } else if (button.displayString.contains("/")) onClose();
        else if (button.displayString.equals("Servers")) Helper.getMinecraft().displayGuiScreen(new ServerListGui());
        else if (button.displayString.equals("Settings")) Helper.getMinecraft().displayGuiScreen(new SettingsGui(this));
        else if (button.displayString.equals("Unmark all")) {
            for (ChestElement chest : ChestElement.getAllMarked(chestsToRender)) {
                chest.markUntilUpdate = false;
                chest.pin = false;
                ChestHistory.INSTANCE.saveChest(chest, serverIP);
            }
            initGui();
        } else if (button.displayString.contains("Show")) {
            showMarked = !showMarked;
            initGui();
            button.x = maxX; button.y = maxY; // Move out of way, stops double clicks
        } else if (button.id == page + 1) {
            button.displayString = button.displayString.equals(unchecked) ? checked : unchecked;
            int index = buttonList.indexOf(button);
            chestsToRender.get(index / 2).markUntilUpdate = !button.displayString.equals("\u25EF");
            ChestHistory.INSTANCE.saveChest(chestsToRender.get(index / 2), serverIP);
            if (showMarked && !chestsToRender.get(index / 2).markUntilUpdate && !chestsToRender.get(index / 2).pin)
                initGui();
        } else if (button.id == -(page + 1)) {
            int index = buttonList.indexOf(button);
            button.displayString = button.displayString.equals(unchecked) ? checked : unchecked;
            chestsToRender.get((index - 1) / 2).pin = !button.displayString.equals(unchecked);
            ChestHistory.INSTANCE.saveChest(chestsToRender.get((index - 1) / 2), serverIP);
            if (showMarked && !chestsToRender.get((index - 1) / 2).markUntilUpdate && !chestsToRender.get((index - 1) / 2).pin)
                initGui();
        }
    }

    private double multiplyFactor(int number) {
        return number * getFactor();
    }

    /**
     * Get a gui if it is in our list of cached guis. Cached guis last for max of 10 mins
     * If not found, creates and adds to the new cache
     * @param serverIP The serverIP for the gui
     * @return The chest gui for the serverIP
     */
    public static ChestGui getCachedGui(String serverIP) {
        if (serverIP.equals(Helper.getServerIP())) return getInstance();
        for (ChestGui chestGui : cachedGuis) {
            if (serverIP.equals(chestGui.serverIP)) {
                cachedGuis.resetTimer(chestGui);
                return chestGui;
            }
        }
        ChestGui chestGui = new ChestGui(ChestHistory.INSTANCE.getChestList(serverIP), serverIP);
        cachedGuis.add(chestGui);
        return chestGui;
    }

    /**
     * When gui is closed send back to either terminal or normal game
     */
    private void onClose() {
        Helper.getMinecraft().displayGuiScreen(null);
    }

    /**
     * Called from the main game loop to update the screen.
     */
    @Override
    public void updateScreen() {
        super.updateScreen();
        this.indexField.updateCursorCounter();
        this.searchField.updateCursorCounter();
        this.searchField.setVisible((getSearchText() || getSearchItem()));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        if (keyCode == Keyboard.KEY_ESCAPE) onClose();
        if (indexField.isFocused()) {
            if (keyCode == Keyboard.KEY_DELETE) {
                this.indexField.setText("");
                setPage(indexField.getText());
            } else if (keyCode == Keyboard.KEY_BACK) {
                if (!indexField.getText().isEmpty()) {
                    String text = indexField.getText().substring(0, indexField.getText().length() - 1);
                    setPage(text);
                    if (text.isEmpty()) indexField.setText("");
                    else indexField.setText(page + 1 + "");
                }
            } else if (indexField.getText().length() < indexField.getMaxStringLength() && setPage(indexField.getText() + typedChar))
                indexField.setText(page + 1 + "");
        }
        if (searchField.isFocused()) {
            if (keyCode == Keyboard.KEY_DELETE)
                this.searchField.setText("");
            else if (searchField.getText().length() < searchField.getMaxStringLength())
                searchField.textboxKeyTyped(typedChar, keyCode);
            if (!search.equals(searchField.getText())) {
                search = searchField.getText();
                initGui();
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 1 && !indexField.getVisible()) {
            GuiButton exit1 = buttons.get(5), exit2 = buttons.get(6);
            if (exit1.isMouseOver() || exit2.isMouseOver()) {
                exit1.visible = !exit1.isMouseOver();
                exit2.visible = !exit2.isMouseOver();
                indexField.x = (maxX - indexField.getWidth())/ 2;
                indexField.y = exit1.isMouseOver() ? 2 : exit2.y + 2;
                indexField.setEnabled(true);
                indexField.setFocused(true);
                indexField.setVisible(true);
                timer.start();
            }
        }
        if (mouseButton == 2 && pageIndexes.size() > 0 && startPositions.size() > 0) {
            for (int index = pageIndexes.get(page)[0]; index <= pageIndexes.get(page)[1]; index++) {
                if (index >= startPositions.size() || index >= chestsToRender.size()) break;
                int[] position = startPositions.get(index);
                int startX = position[0], startY = position[1];
                if (mouseX >= startX && mouseX < startX + multiplyFactor(140) && mouseY >= startY && mouseY <= startY + multiplyFactor(12)) {
                    ChestHistory.INSTANCE.removeChest(chestList, index, serverIP);
                    initGui();
                    break;
                }
            }
        }
        indexField.mouseClicked(mouseX, mouseY, mouseButton);
        searchField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean setPage(String text) {
        timer.reset(); timer.start();
        if (chestsToRender.isEmpty()) return false;
        try {
            page = Integer.parseInt(text) - 1;
        } catch (NumberFormatException ignored) {
            return false;
        }
        if (page >= pageIndexes.size()) page = pageIndexes.size() - 1;
        else if (page < 0) page = 0;
        return true;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}