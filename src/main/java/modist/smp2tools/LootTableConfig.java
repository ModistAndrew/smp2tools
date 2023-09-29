package modist.smp2tools;

import net.minecraft.resources.ResourceLocation;
import org.bukkit.configuration.file.FileConfiguration;

public class LootTableConfig {
  private final FileConfiguration config;

  public LootTableConfig(FileConfiguration config){
    this.config = config;
  }

  public LootTableSettings getSettings(ResourceLocation lootTable) {
    return config.get("loot")
  }

  public static class LootTableSettings {
    public int range;
    public int
  }
}
