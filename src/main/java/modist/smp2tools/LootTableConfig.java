package modist.smp2tools;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LootTableConfig {
  private final FileConfiguration config;
  private LootTableSettings global;
  private static final String PREFIX = "loot_tables";

  public LootTableConfig(FileConfiguration config) {
    this.config = config;
    this.global = new LootTableSettings();
    this.global = getSettings("global", "");
  }

  @Nullable
  public LootTableSettings getSettings(String namespace, String key) {
    LootTableSettings settings = new LootTableSettings();
    String path = getPath(namespace, key);
    if (path != null) {
      ConfigurationSection section = config.getConfigurationSection(path);
      settings.range = section.getInt("range", global.range);
      settings.function = section.getString("function", global.function);
      settings.failInfo = section.getString("fail_info", global.failInfo);
      settings.successInfo = section.getString("success_info", global.successInfo);
      settings.protection = section.getBoolean("protection", global.protection);
      return settings;
    }
    return null;
  }

  @Nullable
  private String getPath(String namespace, String key) {
    String path;
    if(!key.isEmpty()) {
      path = PREFIX + '.' + namespace + ':' + key; //first check full path
      if (config.contains(path)) {
        return path;
      }
    }
    path = PREFIX + '.' + namespace; //then check namespace
    if (config.contains(path)) {
      return path;
    }
    return null;
  }

  public static class LootTableSettings {
    public int range = 16;
    public String function = "%level";
    public String failInfo = "";
    public String successInfo = "";
    public boolean protection = true;

    public int getCount(int level) {
      String expression = function.replace("%level%", String.valueOf(level));
      try {
        return (int) MathHelper.eval(expression);
      } catch (Exception e) {
        Smp2Tools.INSTANCE.getLogger().warning("Fail to eval function " + expression);
        e.printStackTrace();
      }
      return 0;
    }
  }
}
