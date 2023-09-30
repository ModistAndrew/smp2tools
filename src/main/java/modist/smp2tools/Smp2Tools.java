package modist.smp2tools;

import io.papermc.paper.event.block.BlockBreakBlockEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.border.WorldBorder;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

@DefaultQualifier(NonNull.class)
public final class Smp2Tools extends JavaPlugin implements Listener {
  private LootTableConfig lootTableConfig;
  public static Smp2Tools INSTANCE;

  private NamespacedKey getKey(String key) {
    return new NamespacedKey(this, key);
  }

  private NamespacedKey getKey(ResourceLocation resourceLocation) {
    return new NamespacedKey(resourceLocation.getNamespace(), resourceLocation.getPath());
  }

  public void info(String s, Object... args) {
    this.getLogger().info(String.format(s, args));
  }

  public void execute(String s, Object... args) {
    getServer().dispatchCommand(getServer().getConsoleSender(), String.format(s, args));
  }

  @Override
  public void onEnable() {
    INSTANCE = this;
    lootTableConfig = new LootTableConfig(this.getConfig());
    this.getServer().getPluginManager().registerEvents(this, this);
    info("Smp2Tools Plugin Enabled");
  }

  @Override
  public void onDisable() {
    info("Smp2Tools Plugin Disabled");
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onAdvancement(PlayerAdvancementDoneEvent e) {
    e.getAdvancement().getChildren().forEach(child -> e.getPlayer().getAdvancementProgress(child).awardCriteria("%parent%"));
    e.getAdvancement().getCriteria().stream()
      .filter(s -> s.startsWith("%execute%:"))
      .map(s -> s.substring(10).replace("%player%", e.getPlayer().getName()))
      .forEach(s ->
      {
        info("Executing command: %s", s);
        execute(s);
      });
  }

  private BlockPos getPos(Location loc) {
    return new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onOpenChest(PlayerInteractEvent e) {
    if (e.hasBlock()) {
      Location location = e.getClickedBlock().getLocation();
      CraftWorld cw = (CraftWorld) location.getWorld();
      BlockEntity be = cw.getHandle().getBlockEntity(getPos(location));
      if (be instanceof RandomizableContainerBlockEntity randomizableContainerBlockEntity) {
        ResourceLocation lootTable = randomizableContainerBlockEntity.lootTable;
        if (lootTable != null && lootTableConfig.contains(lootTable.toString())) {
          LootTableConfig.LootTableSettings settings = lootTableConfig.getSettings(lootTable.toString());
          PdcUtil.map(e.getPlayer().getPersistentDataContainer(), getKey("loot_penalty"), PersistentDataType.TAG_CONTAINER,
            e.getPlayer().getPersistentDataContainer().getAdapterContext().newPersistentDataContainer(),
            pdc -> {
              PdcUtil.map(pdc, getKey(lootTable), LootPenalty.LOOT_PENALTY, new LootPenalty(),
                lp -> {
                  if (!lp.update(settings::getCount)) {
                    e.getPlayer().sendMessage(Component.text(settings.failInfo.replace("%count%", String.valueOf(lp.count))
                      .replace("%level%", String.valueOf(lp.level))
                      .replace("%loot_table%", lootTable.toString()), NamedTextColor.RED));
                    teleport(cw, location, settings.range);
                    e.setCancelled(true);
                  } else {
                    e.getPlayer().sendMessage(Component.text(settings.successInfo.replace("%count%", String.valueOf(lp.count))
                      .replace("%level%", String.valueOf(lp.level))
                      .replace("%loot_table%", lootTable.toString()), NamedTextColor.GREEN));
                  }
                  return lp;
                });
              return pdc;
            });
        }
      }
    }
  }

  @EventHandler
  public void onExplosion(BlockExplodeEvent event) {
    if(event.getBlock())

  }

  @EventHandler
  public void onExplosion(EntityExplodeEvent event) {
    event.blockList().stream().filter(b -> {

    });
  }

  private void teleport(CraftWorld cw, Location loc, int range) {
    Level world = cw.getHandle();
    BlockPos pos = getPos(loc);
    WorldBorder worldborder = world.getWorldBorder();
    for (int i = 0; i < 1000; ++i) {
      BlockPos to = pos.offset(world.random.nextInt(range) - world.random.nextInt(range), world.random.nextInt(range) - world.random.nextInt(range), world.random.nextInt(range) - world.random.nextInt(range));
      if (world.getBlockState(to).isAir() && worldborder.isWithinBounds(to)) {
        execute("clone %d %d %d %d %d %d %d %d %d", pos.getX(), pos.getY(), pos.getZ(),
          pos.getX(), pos.getY(), pos.getZ(),
          to.getX(), to.getY(), to.getZ());
        execute("data remove block %d %d %d LootTableSeed", pos.getX(), pos.getY(), pos.getZ());
        execute("data modify block %d %d %d LootTable set value %s", pos.getX(), pos.getY(), pos.getZ(), "\"\"");
        execute("setblock %d %d %d air", pos.getX(), pos.getY(), pos.getZ());
        for (int j = 0; j < 128; ++j) {
          double d0 = world.random.nextDouble();
          float f = (world.random.nextFloat() - 0.5F) * 0.2F;
          float f1 = (world.random.nextFloat() - 0.5F) * 0.2F;
          float f2 = (world.random.nextFloat() - 0.5F) * 0.2F;
          double d1 = Mth.lerp(d0, to.getX(), pos.getX()) + (world.random.nextDouble() - 0.5D) + 0.5D;
          double d2 = Mth.lerp(d0, to.getY(), pos.getY()) + world.random.nextDouble() - 0.5D;
          double d3 = Mth.lerp(d0, to.getZ(), pos.getZ()) + (world.random.nextDouble() - 0.5D) + 0.5D;
          cw.spawnParticle(Particle.PORTAL, d1, d2, d3, 1, f, f1, f2);
        }
        return;
      }
    }
  }
}
