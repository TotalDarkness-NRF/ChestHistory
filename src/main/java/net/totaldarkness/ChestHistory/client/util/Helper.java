package net.totaldarkness.ChestHistory.client.util;

import net.minecraft.entity.Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.Optional;

public class Helper {
    public static final Minecraft MC = Minecraft.getMinecraft();

    public static void printError(String message, Object... args) {
        outputMessage(
                getFormattedText("[Chest History]", TextFormatting.RED, true, false)
                        .appendSibling(
                                getFormattedText(" " + String.format(message, args).trim(),
                                        TextFormatting.GRAY, false, false)
                        )
        );
    }

    public static void printWarning(String message, Object... args) {
        outputMessage(
                getFormattedText("[Chest History]", TextFormatting.YELLOW, true, false)
                        .appendSibling(
                                getFormattedText(" " + String.format(message, args).trim(),
                                        TextFormatting.GRAY, false, false)
                        )
        );
    }

    public static void printInform(String message, Object... args) {
        outputMessage(
                getFormattedText("[Chest History]", TextFormatting.GREEN, true, false)
                        .appendSibling(
                                getFormattedText(" " + String.format(message, args).trim(),
                                        TextFormatting.GRAY, false, false)
                        )
        );
    }

    public static ITextComponent getFormattedText(String text, TextFormatting color, boolean bold, boolean italic) {
        return new TextComponentString(text.replaceAll("\r", ""))
                .setStyle(new Style().setColor(color).setBold(bold).setItalic(italic));
    }

    private static void outputMessage(ITextComponent text) {
        if (getLocalPlayer() == null) return;
        getLocalPlayer().sendMessage(text);
    }

    public static FileManager getFileManager() {
        return FileManager.getInstance();
    }

    @Nullable
    public static GuiScreen getCurrentScreen() {
        return MC.currentScreen;
    }

    @Nullable
    public static WorldClient getWorld() {
        return MC.world;
    }

    public static EntityPlayerSP getLocalPlayer() {
        return MC.player;
    }

    public static Entity getViewEntity() {
        return MC.getRenderViewEntity();
    }

    public static Minecraft getMinecraft() {
        return MC;
    }

    public static GameSettings getGameSettings() {
        return MC.gameSettings;
    }

    public static String getDimensionName(int dim) {
        switch (dim) {
            case 0:
                return "OverWorld";
            case 1:
                return "The End";
            case -1:
                return "Nether";
            default:
                return "Unknown";
        }
    }

    public static String getServerIP() {
        return Optional.ofNullable(MC.getCurrentServerData())
                .map(data -> data.serverIP)
                .orElse(getSinglePlayerName());
    }

    public static String getSinglePlayerName() {
        return Optional.ofNullable(MC.getIntegratedServer())
                .map(MinecraftServer::getWorldName)
                .orElse("SinglePlayer");
    }
}