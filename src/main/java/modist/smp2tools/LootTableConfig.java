package modist.smp2tools;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class LootTableConfig {
  private final FileConfiguration config;
  private final LootTableSettings global;
  private static final String PREFIX = "loot_tables";

  public LootTableConfig(FileConfiguration config) {
    this.config = config;
    this.global = getSettings("global");
  }

  public LootTableSettings getSettings(String lootTable) {
    LootTableSettings settings = new LootTableSettings();
    ConfigurationSection section = config.getConfigurationSection(getPath(lootTable));
    settings.range = section.getInt("range", global != null ? global.range : 16);
    settings.function = section.getString("function", global != null ? global.function : "%level");
    settings.failInfo = section.getString("fail_info", global != null ? global.failInfo : "");
    settings.successInfo = section.getString("success_info", global != null ? global.successInfo : "");
    return settings;
  }

  public boolean contains(String lootTable) {
    return config.contains(getPath(lootTable));
  }

  private String getPath(String lootTable) {
    return PREFIX + '.' + lootTable;
  }

  public static class LootTableSettings {
    public int range;
    public String function;
    public String failInfo;
    public String successInfo;

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
