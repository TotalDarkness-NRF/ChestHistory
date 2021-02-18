package net.totaldarkness.ChestHistory.client.util;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedList;
import java.util.List;

public class ChestElement {
    private List<ItemStack> items;
    private final BlockPos position;
    private final int dimension;
    private String chestName, id;
    private final boolean isEnderChest;
    public boolean markUntilUpdate, pin;
    public long time;


    public ChestElement(final BlockPos position, final String id,
                        final String chestName, final boolean markUntilUpdate,
                        final boolean pin, final List<ItemStack> items,
                        final int dimension, final long time) {
        this.id = id;
        this.isEnderChest = id.equals("ender_chest");
        this.position = isEnderChest ? new BlockPos(0, -1, 0) : position;
        this.dimension = isEnderChest ? -9 : dimension;
        this.chestName = chestName;
        this.markUntilUpdate = markUntilUpdate;
        this.pin = pin;
        this.items = items;
        this.time = time;
    }

    public ChestElement(final BlockPos position, final String id, final String chestName, final List<ItemStack> items, final int dimension, final long time) {
        this(position, id, chestName, false, false, items, dimension, time);
    }

    public ChestElement(final String id, final String chestName, final boolean markUntilUpdate, final boolean pin,
                        final List<ItemStack> items, final long time) {
        this.id = id;
        this.chestName = chestName;
        this.markUntilUpdate = markUntilUpdate;
        this.pin = pin;
        this.items = items;
        this.isEnderChest = true;
        this.dimension = -9;
        this.position = new BlockPos(0, -1, 0);
        this.time = time;
    }

    public BlockPos getPosition() {
        return position;
    }

    public String getId() {
        return id;
    }

    public String getChestName() {
        return chestName;
    }

    public int getDimension() {
        return dimension;
    }

    public boolean isEnderChest() {
        return isEnderChest;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public long getTime() {
        return time;
    }

    public void updateChest(final ChestElement chestElement) {
        this.time = System.currentTimeMillis();
        this.markUntilUpdate = false;
        this.items = chestElement.items;
        this.chestName = chestElement.chestName;
        this.id = chestElement.id;
    }

    public String getText() {
        if (isEnderChest()) return getChestName();
        return String.format("%s: %s", getChestName(), formatPosition());
    }

    public String formatPosition() {
        return formatPosition(position);
    }

    public static String formatPosition(BlockPos position) {
        return String.format("%d %d %d", position.getX(), position.getY(), position.getZ());
    }

    /**
     * Get all chests that have been marked or pinned
     * @return A list of marked chests
     */
    public static List<ChestElement> getAllMarked(List<ChestElement> chestsList) {
        List<ChestElement> marked = new LinkedList<>();
        if (chestsList == null) return marked; // Special dumb case
        for (ChestElement chest : chestsList)
            if (chest.markUntilUpdate || chest.pin)
                marked.add(chest);
        return marked;
    }

    /**
     * Check if this chest has the same positions
     * @param chestElement The chest to check
     * @return True when positions are equal, false otherwise
     */
    public boolean samePosition(ChestElement chestElement) {
        return dimension == chestElement.dimension && position.equals(chestElement.position);
    }


    /**
     * Used to check if this chest
     * @param  chest The chest to compare
     * @return If there was a neighbouring chest
     */
    public boolean neighbouringChest(ChestElement chest) {
        if (Helper.getWorld() == null || dimension != chest.dimension) return false;
        BlockPos blockPos = chest.position;
        Block block = Helper.getWorld().getBlockState(blockPos).getBlock();
        if (block != Blocks.TRAPPED_CHEST && block != Blocks.CHEST) return false;
        for (int i = 0; i < 4; i++) {
            BlockPos adj = blockPos.offset(EnumFacing.getHorizontal(i));
            Block other = Helper.getWorld().getBlockState(adj).getBlock();
            if (position.equals(adj) && other == block) return true;
        }
        return false;
    }

    public boolean itemsEqual(List<ItemStack> items) {
        if (this.items.size() != items.size()) return false;
        for (int i = 0; i < this.items.size(); i++)
            if (!ItemStack.areItemStacksEqual(this.items.get(i),items.get(i))) return false;
        return true;
    }

    /**
     * ChestElements are equal when they either:
     * Share the same address in memory or
     * Have the same dimension, position, id, chest name, items, and chest type
     * @param object The object that is to be compared to this object
     * @return True if they have the same dimension, position, id, chest name, items, and both are the same chest type, false otherwise
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ChestElement)) return false;
        ChestElement chestElement = (ChestElement) object;
        return similarPosition(chestElement) &&
                id.equals(chestElement.id) &&
                chestName.equals(chestElement.chestName) &&
                itemsEqual(chestElement.getItems())
                && isEnderChest == chestElement.isEnderChest;
    }

    /**
     * Check if the chests are similar
     * @param chestElement The chest to compare
     * @return True if the dimension, position or adjacent position and items are the same, false otherwise
     */
    public boolean similarPosition(ChestElement chestElement) {
        return samePosition(chestElement) || neighbouringChest(chestElement);
    }
}
