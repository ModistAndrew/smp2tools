package modist.smp2tools;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class PdcUtil {

  public static <T, Z> void map(PersistentDataContainer pdc, @NotNull NamespacedKey key, @NotNull PersistentDataType<T, Z> type, @NotNull Z defaultValue, Function<Z, Z> function) {
    Z value = pdc.getOrDefault(key, type, defaultValue);
    pdc.set(key, type, function.apply(value));
  }
}
