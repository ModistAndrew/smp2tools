package modist.smp2tools;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class Smp2Tools extends JavaPlugin implements Listener {
  private LootTableConfig lootTableConfig;
  public static Smp2Tools INSTANCE;

  private NamespacedKey getKey(String key) {
    return new NamespacedKey(this, key);
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
    this.getServer().getPluginManager().registerEvents(this, this);
    info("Smp2Tools Plugin Enabled");
  }

  @Override
  public void onDisable() {
    info("Smp2Tools Plugin Disabled");
  }

  @Override
  public void onLoad() {
    lootTableConfig = new LootTableConfig(this.getConfig());
    info("Smp2Tools Plugin Loaded");
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

  @EventHandler
  public void protectLoot(BlockBreakEvent event) {
    if(shouldProtect(event.getBlock())!=null){
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void protectLoot(BlockExplodeEvent event) {
    event.blockList().removeAll(event.blockList().stream().filter(b -> shouldProtect(b)!=null).toList());
  }

  @EventHandler
  public void protectLoot(EntityExplodeEvent event) {
    event.blockList().removeAll(event.blockList().stream().filter(b -> shouldProtect(b)!=null).toList());
  }

  @Nullable
  private ResourceLocation shouldProtect(Block block) { //check if there is a valid loot table that should be protected
    Location location = block.getLocation();
    CraftWorld cw = (CraftWorld) location.getWorld();
    BlockEntity be = cw.getHandle().getBlockEntity(getPos(location));
    if (be instanceof RandomizableContainerBlockEntity randomizableContainerBlockEntity) {
      ResourceLocation lootTable = randomizableContainerBlockEntity.lootTable;
      if(lootTable!=null) {
        LootTableConfig.LootTableSettings settings = lootTableConfig.getSettings(lootTable.getNamespace(), lootTable.getPath());
        if (settings != null && settings.protection) {
          info("protecting loot table %s in %s", lootTable, location);
          return lootTable;
        }
      }
    }
    return null;
  }

  @EventHandler
  public void onLoot(LootGenerateEvent event) {
    NamespacedKey lootTable = event.getLootTable().getKey();
    LootTableConfig.LootTableSettings settings = lootTableConfig.getSettings(lootTable.getNamespace(), lootTable.getKey());
    if (event.getInventoryHolder() instanceof BlockInventoryHolder blockInventoryHolder && settings!=null) {
      if (event.getEntity() instanceof Player player) {
        PdcUtil.map(player.getPersistentDataContainer(), getKey("loot_penalty"), PersistentDataType.TAG_CONTAINER,
          player.getPersistentDataContainer().getAdapterContext().newPersistentDataContainer(),
          pdc -> {
            PdcUtil.map(pdc, lootTable, LootPenalty.LOOT_PENALTY, new LootPenalty(),
              lp -> {
                if (!lp.update(settings::getCount)) {
                  if(!settings.failInfo.isEmpty()) {
                    player.sendMessage(Component.text(settings.failInfo.replace("%count%", String.valueOf(lp.count))
                      .replace("%level%", String.valueOf(lp.level))
                      .replace("%loot_table%", lootTable.toString()), NamedTextColor.RED));
                  }
                  teleport(blockInventoryHolder.getBlock(), settings.range, event.getLootTable().getKey().asString());
                  event.setCancelled(true);
                } else {
                  if(!settings.successInfo.isEmpty()) {
                    player.sendMessage(Component.text(settings.successInfo.replace("%count%", String.valueOf(lp.count))
                      .replace("%level%", String.valueOf(lp.level))
                      .replace("%loot_table%", lootTable.toString()), NamedTextColor.GREEN));
                  }
                  event.setCancelled(false);
                }
                return lp;
              });
            return pdc;
          });
      } else {
        teleport(blockInventoryHolder.getBlock(), settings.range, event.getLootTable().getKey().asString());
        event.setCancelled(true);
      }
    }
  }

  private void teleport(Block block, int range, String lootTable) {
    Location from = block.getLocation();
    CraftWorld cw = (CraftWorld) from.getWorld();
    Level world = cw.getHandle();
    for (int i = 0; i < 1000; ++i) {
      Location to = from.clone().add(world.random.nextInt(range) - world.random.nextInt(range), world.random.nextInt(range) - world.random.nextInt(range), world.random.nextInt(range) - world.random.nextInt(range));
      if (cw.getBlockAt(to).getType().isAir() && cw.getWorldBorder().isInside(to)) {
        BlockEntity blockEntity = world.getBlockEntity(getPos(from));
        CompoundTag compoundTag = blockEntity.saveWithoutMetadata();
        long seed = 0;
        if (blockEntity instanceof RandomizableContainerBlockEntity randomizableContainerBlockEntity) {
          seed = randomizableContainerBlockEntity.lootTableSeed;
        }
        cw.getBlockAt(to).setType(block.getType());
        cw.getBlockAt(to).setBlockData(block.getBlockData());
        BlockEntity newBlockEntity = world.getBlockEntity(getPos(to));
        if (compoundTag != null && newBlockEntity instanceof RandomizableContainerBlockEntity randomizableContainerBlockEntity) {
          newBlockEntity.load(compoundTag);
          newBlockEntity.setChanged();
          randomizableContainerBlockEntity.setLootTable(new ResourceLocation(lootTable), seed);
        }
        cw.getBlockAt(from).setType(Material.AIR);
        info("teleporting block from %s to %s, loot table: %s, seed: %d", from, to, lootTable, seed);
        for (int j = 0; j < 128; ++j) {
          double d0 = world.random.nextDouble();
          float f = (world.random.nextFloat() - 0.5F) * 0.2F;
          float f1 = (world.random.nextFloat() - 0.5F) * 0.2F;
          float f2 = (world.random.nextFloat() - 0.5F) * 0.2F;
          double d1 = Mth.lerp(d0, to.getX(), from.getX()) + (world.random.nextDouble() - 0.5D) + 0.5D;
          double d2 = Mth.lerp(d0, to.getY(), from.getY()) + world.random.nextDouble() - 0.5D;
          double d3 = Mth.lerp(d0, to.getZ(), from.getZ()) + (world.random.nextDouble() - 0.5D) + 0.5D;
          cw.spawnParticle(Particle.PORTAL, d1, d2, d3, 1, f, f1, f2);
        }
        return;
      }
    }
  }
}
