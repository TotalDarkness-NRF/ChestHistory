package net.totaldarkness.ChestHistory.client.services;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.totaldarkness.ChestHistory.client.settings.Setting;
import net.totaldarkness.ChestHistory.client.settings.SettingEnum;
import org.lwjgl.input.Keyboard;

public class ChestGuiService {

  public static KeyBinding bind;
  // TODO create key binds to open gui and add to minecraft settings
  public static ChestGuiService INSTANCE;

  private final Setting<Boolean> text = Setting.build("text", "Show text.", true);
  private final Setting<Boolean> showMarkedChestsList = Setting.build("show-marked-list", "Show marked chest list", true);
  private final Setting<Boolean> showLastTimeUpdated = Setting.build("last-time-updated", "Show the last time it was updated.", true);
  private final Setting<Boolean> showElapsedTimeUpdated = Setting.build("elapsed-time", "Show how long its been since last updated.", true);
  private final Setting<Boolean> censorCoords = Setting.build("censor-coords", "Censor coords for save messages.", false);
  private final Setting<Boolean> searchCaseSensitive = Setting.build("case-sensitive", "Decide if search is case sensitive", true);
  private final Setting<Boolean> searchByText = Setting.build("search-text", "Search using text", true);
  private final Setting<Boolean> searchByItem = Setting.build("search-item", "Search using item names", true);
  private final Setting<SortingModes> sortMode = SettingEnum.build("sorting", "How to sort chest list.", SortingModes.NONE);
  private final Setting<ChestGuiDirection> chestGuiDirection = SettingEnum.build("direction", "What direction to render chests in ChestGui.", ChestGuiDirection.VERTICAL);

  public enum SortingModes {
    NONE, NEWEST, OLDEST, RANDOM, LARGEST, SMALLEST
  }

  public enum ChestGuiDirection {
    HORIZONTAL, VERTICAL
  }

  public ChestGuiService() {
    INSTANCE = this;
    bind = new KeyBinding("ChestHistory", Keyboard.KEY_H, "ChestHistory");
    ClientRegistry.registerKeyBinding(bind);
  }

  public static boolean getTextSetting() {
    return INSTANCE.text.get();
  }

  public static boolean getMarkedChestsSetting() {
    return INSTANCE.showMarkedChestsList.get();
  }

  public static boolean getCensorCoordsSetting() {
    return INSTANCE.censorCoords.get();
  }

  public static boolean getLastUpdatedSetting() {
    return INSTANCE.showLastTimeUpdated.get();
  }

  public static boolean getTimeElapsedSetting() {
    return INSTANCE.showElapsedTimeUpdated.get();
  }

  public static boolean getSearchCaseSensitive() {
    return INSTANCE.searchCaseSensitive.get();
  }

  public static boolean getSearchText() {
    return INSTANCE.searchByText.get();
  }

  public static boolean getSearchItem() {
    return INSTANCE.searchByItem.get();
  }

  public static ChestGuiDirection getGuiDirection() {
    return INSTANCE.chestGuiDirection.get();
  }

  public static SortingModes getSortMode() {
    return INSTANCE.sortMode.get();
  }
}