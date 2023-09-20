package modist.smp2advancement;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.advancement.CraftAdvancement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.Arrays;

import static net.kyori.adventure.text.Component.text;

@DefaultQualifier(NonNull.class)
public final class Smp2Advancement extends JavaPlugin implements Listener {
  @Override
  public void onEnable() {
    this.getServer().getPluginManager().registerEvents(this, this);
    this.getLogger().info("Smp2Advancement Plugin Enabled");
  }

  @Override
  public void onDisable() {
    this.getLogger().info("Smp2Advancement Plugin Disabled");
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onAdvancement(PlayerAdvancementDoneEvent e) {
      e.getAdvancement().getChildren().forEach(child -> e.getPlayer().getAdvancementProgress(child).awardCriteria("%parent%"));
      e.getAdvancement().getCriteria().stream()
        .filter(s -> s.startsWith("%execute%:"))
        .map(s->s.substring(10).replace("%player%", e.getPlayer().getName()))
        .forEach(s ->
            getServer().dispatchCommand(getServer().getConsoleSender(), s));
  }
}
