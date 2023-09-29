package modist.smp2tools;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class LootPenalty {
  int count; //how many times will the chest be transported
  int level; //how many times has the player opened a chest
  public static final LootPenaltyType LOOT_PENALTY = new LootPenaltyType();

  public boolean update(Int2IntFunction function) { //return true if the player is allowed to open the chest
    if(count<=0){
      level++;
      count = function.applyAsInt(level);
      return true;
    } else {
      count--;
      return false;
    }
  }

  public LootPenalty(int count, int level) {
    this.count = count;
    this.level = level;
  }

  public LootPenalty() {
  }

  static class LootPenaltyType implements PersistentDataType<int[], LootPenalty> {
    @Override
    public @NotNull Class<int[]> getPrimitiveType() {
      return int[].class;
    }

    @Override
    public @NotNull Class<LootPenalty> getComplexType() {
      return LootPenalty.class;
    }

    @Override
    public int @NotNull [] toPrimitive(@NotNull LootPenalty complex, @NotNull PersistentDataAdapterContext context) {
      return new int[]{complex.count, complex.level};
    }

    @Override
    public @NotNull LootPenalty fromPrimitive(int @NotNull [] primitive, @NotNull PersistentDataAdapterContext context) {
      return new LootPenalty(primitive[0], primitive[1]);
    }
  }
}
