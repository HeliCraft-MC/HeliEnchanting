package ru.helicraft.helienchanting.listener;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import ru.helicraft.helienchanting.enchant.DrillEnchant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class DrillListener implements Listener {

    /** Флаг, предотвращающий рекурсивное бурение. */
    private static final String META_DRILLING = "helienchanting:drilling";

    /** level → (radiusX, radiusY, radiusZ). */
    private static final Map<Integer, Vector> SIZE = new HashMap<>();
    static {
        SIZE.put(1, new Vector(0, 1, 0));  // 2×1
        SIZE.put(2, new Vector(1, 1, 0));  // 2×3
        SIZE.put(3, new Vector(1, 1, 1));  // 3×3
        SIZE.put(4, new Vector(2, 1, 1));  // 3×5
        SIZE.put(5, new Vector(2, 2, 2));  // 5×5
    }

    /** Материалы, которые всегда запрещено бурить. */
    private static final Set<Material> UNBREAKABLE = Set.of(
            Material.BEDROCK,
            Material.BARRIER,
            Material.END_PORTAL,
            Material.END_PORTAL_FRAME,
            Material.END_GATEWAY,
            Material.REINFORCED_DEEPSLATE,
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.JIGSAW,
            Material.STRUCTURE_BLOCK,
            Material.STRUCTURE_VOID,
            Material.LIGHT,
            Material.NETHER_PORTAL
    );

    private final JavaPlugin plugin;

    private static final Registry<Enchantment> ENCHANT_REG =
            RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
    private static final Enchantment DRILL =
            ENCHANT_REG.getOrThrow(EnchantmentKeys.create(DrillEnchant.KEY));

    public DrillListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        // защита от пере-вызова
        if (event.getPlayer().hasMetadata(META_DRILLING)) return;

        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (tool == null || tool.getType() == Material.AIR) return;

        int level = tool.getEnchantmentLevel(DRILL);
        if (level <= 0) return;

        // точный режим при зажатом Shift
        if (event.getPlayer().isSneaking()) return;

        Vector delta = SIZE.getOrDefault(level, new Vector());
        Block origin = event.getBlock();
        Location loc = origin.getLocation();

        // определяем плоскость бурения
        double pitch = event.getPlayer().getPitch();
        double yaw   = event.getPlayer().getYaw();
        boolean vertical = Math.abs(pitch) > 60;
        boolean xAxis    = Math.abs(yaw) > 45 && Math.abs(yaw) < 135;

        int rx = vertical ? delta.getBlockX() : (xAxis ? 0 : delta.getBlockX());
        int ry = vertical ? 0 : delta.getBlockY();
        int rz = vertical ? delta.getBlockZ() : (xAxis ? delta.getBlockZ() : 0);

        event.getPlayer().setMetadata(META_DRILLING, new FixedMetadataValue(plugin, true));
        try {
            for (int dx = -rx; dx <= rx; dx++) {
                for (int dy = -ry; dy <= ry; dy++) {
                    for (int dz = -rz; dz <= rz; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block b = loc.clone().add(dx, dy, dz).getBlock();
                        if (!canBreak(b.getBlockData(), tool)) continue;
                        b.breakNaturally(tool, true); // вызовет своё BlockBreakEvent, которое мы пропустим
                    }
                }
            }
        } finally {
            event.getPlayer().removeMetadata(META_DRILLING, plugin);
        }

        tool.damage(level, event.getPlayer());
    }

    /** Проверка пригодности блока для бурения. */
    private boolean canBreak(BlockData data, ItemStack tool) {
        Material mat = data.getMaterial();

        // 1) явно запрещённые блоки
        if (UNBREAKABLE.contains(mat)) return false;

        // 2) воздух / «несолидные»
        if (mat.isAir() || !mat.isSolid()) return false;

        // 3) бесконечная или отрицательная твёрдость
        float hardness = mat.getHardness();
        if (Float.isInfinite(hardness) || hardness < 0F) return false;

        // 4) инструмент не подходит
        if (!data.isPreferredTool(tool)) return false;

        // 5) ненулевая скорость добычи
        return data.getDestroySpeed(tool, true) > 0;
    }
}
