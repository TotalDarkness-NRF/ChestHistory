package net.totaldarkness.ChestHistory.client.gui;

import net.totaldarkness.ChestHistory.client.settings.Setting;
import net.totaldarkness.ChestHistory.client.events.RenderEvent;
import net.totaldarkness.ChestHistory.client.services.ChestGuiService;
import net.totaldarkness.ChestHistory.client.util.render.GeometryMasks;
import net.totaldarkness.ChestHistory.client.util.render.GeometryTessellator;
import net.totaldarkness.ChestHistory.client.util.ChestElement;
import net.totaldarkness.ChestHistory.client.util.Helper;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiShulkerBox;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerShulkerBox;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityShulkerBox;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.GuiContainerEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.minecart.MinecartInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static net.totaldarkness.ChestHistory.client.util.Helper.MC;
import static net.totaldarkness.ChestHistory.client.util.Helper.getFileManager;

public class ChestHistory {
    public static double scale = 16;
    public static boolean enabled;
    public static ChestHistory INSTANCE;
    private static List<ChestElement> chestsList;
    private String id, serverIP;
    private BlockPos blockpos;

    private final Setting<Boolean> saveMsgs = Setting.build("save-messages", "Show save messages.", true);
    private final Setting<Boolean> removeMsgs = Setting.build("remove-messages", "Show remove messages.", true);

    public ChestHistory() {
        onEnabled();
        INSTANCE = this;
        chestsList = new LinkedList<>();
    }

    public void onEnabled() {
        enabled = !enabled;
        if (!Helper.getServerIP().equals(serverIP)) updateChestList();
    }

    public boolean isEnabled() {
        return enabled;
    }

    //
    // Render stuff start
    //

    /**
     * Render chests on screen for a game overlay event
     *
     * @param event The event for render an overlay
     */
    @SubscribeEvent
    public void renderMarked(final RenderGameOverlayEvent.Text event) {
        if (Helper.getCurrentScreen() instanceof ChestGui) return;
        ChestGui.getInstance().drawMarkedList(MC.displayWidth + 12, 0);
    }

    /**
     * Render a few buttons on a chest inventory screen
     *
     * @param event The event for a gui of chests
     */
    @SubscribeEvent
    public void onGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof GuiChest || event.getGui() instanceof GuiShulkerBox) {
            int offset = Helper.getLocalPlayer().openContainer.getInventory().size() > 70 ? 111 : 84;
            event.getButtonList().add(new GuiButton(99, (event.getGui().width / 2) + 31, (event.getGui().height / 2) - offset, 50, 17, "Chests"));
        }
    }

    /**
     * Renders a box around chests that are pinned or marked for opening
     *
     * @param event The event for when rendering
     */
    @SubscribeEvent
    public void onRender(final RenderEvent event) {
        if (Helper.getGameSettings().hideGUI || !ChestGui.showMarked || Helper.getWorld() == null) return;

        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        GlStateManager.glLineWidth(2);

        for (ChestElement chest : ChestElement.getAllMarked(chestsList)) {
            int color;
            if (chest.pin) color = -1;
            else color = -65536;
            if (chest.isEnderChest()) {
                for (TileEntity tile : Helper.getWorld().loadedTileEntityList)
                    if (Helper.getWorld().getTileEntity(tile.getPos()) instanceof TileEntityEnderChest)
                        GeometryTessellator.drawCuboid(event.getBuffer(), tile.getPos(), GeometryMasks.Line.ALL, color);
            } else GeometryTessellator.drawCuboid(event.getBuffer(), chest.getPosition(), GeometryMasks.Line.ALL, color);
        }

        event.getTessellator().draw();
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.glLineWidth(1.0f);
    }

    //
    // Rendering stuff end
    // Get info stuff Start
    //

    /**
     * When a chest is opened and its inventory is being drawn, save the chest and its detail
     *
     * @param event The event for drawing a chest foreground
     */
    @SubscribeEvent
    public void chestOpened(final GuiContainerEvent.DrawForeground event) {
        if (Helper.getWorld() == null || blockpos == null) return;
        final Container container = event.getGuiContainer().inventorySlots;
        final List<ItemStack> items = container.getInventory().subList(0, container.getInventory().size() > 70 ? 54 : 27);
        String chestName = container.getSlot(0).inventory.getName();
        if (container instanceof ContainerChest || container instanceof ContainerShulkerBox) {
            ChestElement chestBeingViewed = new ChestElement(blockpos, id, chestName, items, Helper.getLocalPlayer().dimension, System.currentTimeMillis());
            ChestElement chestInList = getSimilarChest(chestBeingViewed);
            if ((!removeIfDuplicate(chestBeingViewed, chestInList) &&
                    (chestBeingViewed == chestInList || !chestInList.itemsEqual(items))) || chestInList.markUntilUpdate) {
                updateChest(chestInList, chestBeingViewed);
                saveChest(chestInList);
            }
        }
    }

    /**
     * Occurs when a player right clicks a block, gets the coordinates and id of the block
     *
     * @param event The event for a player right clicking on a block
     */
    @SubscribeEvent
    public void chestClicked(final PlayerInteractEvent.RightClickBlock event) {
        if (Helper.getWorld() == null) return;

        TileEntity tile = Helper.getWorld().getTileEntity(event.getPos());
        if (tile instanceof TileEntityChest || tile instanceof TileEntityShulkerBox || tile instanceof TileEntityEnderChest) {
            blockpos = event.getPos();
            id = Objects.requireNonNull(Helper.getWorld().getBlockState(blockpos).getBlock().getRegistryName()).getResourcePath();
        }
    }

    /**
     * A special case event for minecarts, get its coordinates
     *
     * @param event The event for interacting with a minecart
     */
    @SubscribeEvent
    public void minecartOpened(final MinecartInteractEvent event) {
        blockpos = event.getMinecart().getPosition();
        id = "chest_minecart";
    }

    //
    // Get info stuff end
    // Files stuff starts
    //

    /**
     * Save a chest and its details
     *
     * @param chest The chest to save
     */
    private void saveChest(final ChestElement chest) {
        saveChest(chest, serverIP);
    }

    public void saveChest(final ChestElement chest, final String serverIP) {
        final Path path;
        final StringBuilder nbtData;
        if (chest.isEnderChest()) {
            path = getFileManager().getMkBaseResolve("chests", serverIP + "/" + "Ender_Chest");
            nbtData = new StringBuilder(chest.getId() + "\n" + String.format("%s\n%s\n", chest.markUntilUpdate, chest.pin));
        } else {
            final String fileName = String.format("%s [%d]", chest.formatPosition(), chest.getDimension());
            path = getFileManager().getMkBaseResolve("chests", serverIP + "/" + fileName);
            nbtData = new StringBuilder(chest.getId() + "\n" + chest.getChestName() + "\n" + String.format("%s\n%s\n", chest.markUntilUpdate, chest.pin));
        }

        if (!path.toFile().exists()) addChestElement(getSaveIndex(chest.getItems().size()), chest);

        chest.getItems().forEach(item -> nbtData.append(item.serializeNBT().toString()).append("\n"));
        try {
            Files.write(path, nbtData.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {
        }
    }

    /**
     * Remove any duplicate files and chest if they are there
     *
     * @param chest1 Is the first chest that is currently in view
     * @param chest2 Is the old chest that was in view at one point
     */
    private boolean removeIfDuplicate(ChestElement chest1, ChestElement chest2) {
        if (chest1.getItems().size() < 54 || chest2.getItems().size() < 54 || chest1.getDimension() != chest2.getDimension())
            return false;
        Path path2 = null;
        String fileName;
        if (chest1.neighbouringChest(chest2)) { // Check for neighbouring chest
            fileName = String.format("%s [%d]", chest2.formatPosition(), chest2.getDimension());
            path2 = getFileManager().getMkBaseResolve("chests", serverIP + "/" + fileName);
        } else { // Check if a file with position beside it
            for (int i = 0; i < 4; i++) {
                BlockPos adj = chest1.getPosition().offset(EnumFacing.getHorizontal(i));
                Block block = Objects.requireNonNull(Helper.getWorld()).getBlockState(adj).getBlock();
                fileName = String.format("%s [%d]", ChestElement.formatPosition(adj), chest1.getDimension());
                path2 = getFileManager().getMkBaseResolve("chests", serverIP + "/" + fileName);
                if (path2.toFile().exists() && (block == Blocks.TRAPPED_CHEST || block == Blocks.CHEST)) break;
                else path2 = null;
            }
            if (path2 == null) return false;
        }

        fileName = String.format("%s [%d]", chest1.formatPosition(), chest1.getDimension());
        Path path1 = getFileManager().getMkBaseResolve("chests", serverIP + "/" + fileName);

        if (path1.toFile().exists() && path2.toFile().exists()) {
            Helper.printWarning("The current chest had a duplicate file, it will be deleted and saved to one file");
            try {
                Files.delete(path1);
                Files.delete(path2);
                reSortChestList();
            } catch (Exception ignored) {}
            return true;
        } else return false;
    }

    public void removeChest(List<ChestElement> chestsList, int index, String serverIP) {
        if (index >= chestsList.size()) return;
        ChestElement chest = chestsList.get(index);
        final Path file;
        if (chest.isEnderChest())
            file = getFileManager().getMkBaseResolve("chests", serverIP + "/Ender Chest");
        else {
            String position = ChestElement.formatPosition(chest.getPosition());
            file = getFileManager().getMkBaseResolve("chests", serverIP + "/" + position + " [" + chest.getDimension() + "]");
        }
        try {
            Files.delete(file);
            if (!ChestGuiService.getCensorCoordsSetting() && removeMsgs.get())
                Helper.printInform("Deleted file from " + file.toAbsolutePath());
            if (chestsList.remove(chest) && removeMsgs.get()) Helper.printInform("Successfully removed the chest");
            reSortChestList();
        } catch (Exception e) {
            Helper.printError(e.getMessage()); // pls don't cause crash
        }
    }

    /**
     * Update the list of saved chests, gets the chests items and important details
     */
    public void updateChestList() {
        serverIP = Helper.getServerIP();
        chestsList = getChestList(serverIP);
        ChestGui.resetInstance();
    }

    public List<ChestElement> getChestList(String serverIP) {
        final Path path = getFileManager().getMkBaseResolve("chests", serverIP);
        final List<ChestElement> list = new LinkedList<>();
        final List<File> files = new LinkedList<>();

        // Getting all files and adding to the list
        try {
            files.addAll(Files.walk(path).filter(Files::isRegularFile).map(Path::toFile).collect(Collectors.toList()));
        } catch (Exception ignored) {
        }
        if (files.isEmpty()) {
            Helper.printInform("No chests found.");
            return list;
        }

        for (final File file : files) {
            if (file == null || !file.exists()) continue;
            final String fileName = file.getName();
            if (fileName.isEmpty()) continue;

            // Get file lines and last time modified
            long time = 0;
            final List<String> lines = new LinkedList<>();
            try {
                lines.addAll(Files.readAllLines(file.toPath()));
                time = Files.getLastModifiedTime(file.toPath()).toMillis();
            } catch (Exception ignored) {
            }
            if (lines.isEmpty() || lines.size() < 5) continue;

            // Get special details about the chest
            String id, chestName = "";
            id = lines.get(0);
            lines.remove(0);
            if (!fileName.equals("Ender_Chest")) {
                chestName = lines.get(0);
                lines.remove(0); // Only in a normal chest
            }
            final boolean marked = lines.get(0).equalsIgnoreCase("true");
            lines.remove(0);
            final boolean pinned = lines.get(0).equalsIgnoreCase("true");
            lines.remove(0);

            // Get all items and adds to its list
            final List<ItemStack> items = new LinkedList<>();
            lines.forEach(nbtJson -> items.add(getItemFromJson(nbtJson)));
            if (items.isEmpty()) continue;

            if (fileName.equals("Ender_Chest")) list.add(new ChestElement(id, fileName, marked, pinned, items, time));
            else {
                // Get Dimension
                if (!fileName.contains("[") || !fileName.contains("]")) continue;
                final int indexOfDimension = fileName.indexOf("[") + 1;
                final int indexOfEnd = fileName.indexOf("]");
                if (indexOfDimension == indexOfEnd) continue;
                int dimension = -9;
                try {
                    dimension = Integer.parseInt(fileName.substring(indexOfDimension, indexOfEnd));
                } catch (NumberFormatException ignored) {
                }

                // Get position
                int i = 0;
                final int[] position = new int[3];
                for (String split : fileName.split(" ")) {
                    if (i >= 3) break;
                    try {
                        position[i] = Integer.parseInt(split);
                        i++;
                    } catch (NumberFormatException ignored) {
                    }
                }
                final BlockPos blockPos = new BlockPos(position[0], position[1], position[2]);
                list.add(new ChestElement(blockPos, id, chestName, marked, pinned, items, dimension, time));
            }
        }
        return sortChestList(list);
    }

    public void reSortChestList() {
        serverIP = Helper.getServerIP();
        chestsList = sortChestList(chestsList);
        ChestGui.resetInstance();
    }

    public static List<ChestElement> sortChestList(final List<ChestElement> chestsList) {
        if (ChestGuiService.getSortMode() == ChestGuiService.SortingModes.NONE || ChestGuiService.getSortMode() == ChestGuiService.SortingModes.RANDOM) {
            List<ChestElement> chests = new LinkedList<>();
            chestsList.forEach(chest -> chests.add(INSTANCE.getSaveIndex(chest.getItems().size(), chests.size()), chest));
            return chests;
        }

        chestsList.sort(Comparator.comparingLong(ChestElement::getTime));
        if (ChestGuiService.getSortMode() == ChestGuiService.SortingModes.NEWEST) {
            Collections.reverse(chestsList);
        } else if (ChestGuiService.getSortMode() == ChestGuiService.SortingModes.SMALLEST) {
            chestsList.sort(Comparator.comparingInt(chest -> chest.getItems().size()));
        } else if (ChestGuiService.getSortMode() == ChestGuiService.SortingModes.LARGEST) {
            chestsList.sort(Comparator.comparingInt(chest -> chest.getItems().size()));
            Collections.reverse(chestsList);
        }
        return chestsList;
    }

    private ItemStack getItemFromJson(final String nbtJson) {
        ItemStack itemStack = ItemStack.EMPTY;
        try {
            itemStack = new ItemStack(JsonToNBT.getTagFromJson(nbtJson));
        } catch (Exception ignored) { }
        return itemStack;
    }

    /**
     * Get the index and use sorting for next index
     *
     * @param chestSize The size of the list of saved chests
     * @return The index in the list of chests
     */
    private int getSaveIndex(int chestSize) {
        return getSaveIndex(chestSize, chestsList.size());
    }

    private int getSaveIndex(int chestSize, int listSize) {
        switch (ChestGuiService.getSortMode()) {
            case SMALLEST: {
                if (chestSize >= 54) return listSize;
                else return 0;
            }
            case LARGEST: {
                if (chestSize < 54) return listSize;
                else return 0;
            }
            case RANDOM: return ThreadLocalRandom.current().nextInt(0, listSize + 1);
            case NEWEST: return 0;
            case OLDEST:
            default: return listSize;
        }
    }

    //
    // Files stuff ends
    // Helper stuff starts
    //

    /**
     * Occurs for buttons presses
     *
     * @param event The event for a button press
     */
    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if ((event.getGui() instanceof GuiChest || event.getGui() instanceof GuiShulkerBox) && event.getButton().id == 99) {
            MC.displayGuiScreen(ChestGui.getInstance());
        }
    }

    /**
     * Update the chest list
     *
     * @param event The event for the world loading
     */
    @SubscribeEvent
    public void worldLoad(final WorldEvent.Load event) {
        if (!Helper.getServerIP().equals(serverIP)) updateChestList();
    }

    /**
     * The scaling factor for text of the chest boxes
     * NOTE: used in chest gui (array of chests screen) and chest boxes
     *
     * @return The scale factor for text
     */
    public static double getFactor() {
        return getScale() / 16;
    }

    /**
     * The scale to render the chest boxes
     * NOTE: used in chest gui (array of chests screen) and chest boxes
     *
     * @return The scale
     */
    public static double getScale() {
        return scale;
    }

    //
    // Helper stuff ends
    // ChestElements stuff starts
    //

    /**
     * Get the chest list saved by Chest History
     *
     * @return A list of ChestElement's
     */
    public static List<ChestElement> getChestsList() {
        return chestsList;
    }

    /**
     * Look trough the chest list to find a similar, if found return it,
     * if not return the input chest
     *
     * @param chestElement The chest element to check
     * @return a similar chest if found, if not return chestElement
     */
    private ChestElement getSimilarChest(ChestElement chestElement) {
        for (ChestElement chest : chestsList)
            if (chest.similarPosition(chestElement) && chest.getId().equals(chestElement.getId()))
                return chest;
        return chestElement;
    }

    /**
     * Add a chest element
     *
     * @param chest The chest element to add
     * @param index The index of the chest
     */
    private void addChestElement(final int index, final ChestElement chest) {
        chestsList.add(index, chest);
        if (!saveMsgs.get()) return;
        String name = chest.getId().replaceAll("_", " ");
        String position = "";
        if (!chest.isEnderChest() && !ChestGuiService.getCensorCoordsSetting())
            position = String.format(" at %s", ChestElement.formatPosition(chest.getPosition()));
        Helper.printInform(String.format("Saving new %s%s.", name, position));
    }

    /**
     * Update a chest elements items and then sorting
     *
     * @param chestInList      The chest in our chest list
     * @param chestBeingViewed The chest that is currently in view
     */
    private void updateChest(final ChestElement chestInList, final ChestElement chestBeingViewed) {
        if (chestInList.itemsEqual(chestBeingViewed.getItems()) && !chestInList.markUntilUpdate) return;
        chestInList.updateChest(chestBeingViewed);
        updateSorting(chestInList);
        if (!saveMsgs.get()) return;
        String name = chestInList.getId().replaceAll("_", " ");
        String position = "";
        if (!chestInList.isEnderChest() && !ChestGuiService.getCensorCoordsSetting())
            position = String.format(" at %s", ChestElement.formatPosition(chestInList.getPosition()));
        Helper.printInform(String.format("Updating %s%s.", name, position));
    }

    /**
     * Update the sorting of the chest list
     */
    private void updateSorting(ChestElement chest) {
        if (chestsList.isEmpty() || ChestGuiService.getSortMode() == ChestGuiService.SortingModes.NONE) return;
        chestsList.remove(chest);
        chestsList.add(getSaveIndex(chest.getItems().size()), chest);
    }
}