package ru.helicraft.helienchanting.listener;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.particle.Particle;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
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
import java.util.Map;
import java.util.Set;

public final class DrillListener implements Listener {

    /** Флаг, предотвращающий рекурсивное бурение. */
    private static final String META_DRILLING = "helienchanting:drilling";

    /** level → {width, height}. */
    private static final Map<Integer, int[]> LEVELS = new HashMap<>();
    static {
        LEVELS.put(1, new int[]{1, 2});
        LEVELS.put(2, new int[]{3, 2});
        LEVELS.put(3, new int[]{3, 3});
        LEVELS.put(4, new int[]{5, 3});
        LEVELS.put(5, new int[]{5, 5});
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

    /** Разрешённые блоки для бурения (камень, сланец и т.д.). */
    private static final Set<Material> DRILL_WHITELIST = Set.of(
            Material.STONE,
            Material.DEEPSLATE,
            Material.GRANITE,
            Material.DIORITE,
            Material.ANDESITE,
            Material.TUFF,
            Material.CALCITE,
            Material.DRIPSTONE_BLOCK,
            Material.NETHERRACK,
            Material.BASALT,
            Material.SMOOTH_BASALT,
            Material.BLACKSTONE,
            Material.END_STONE,
            Material.COBBLESTONE,
            Material.COBBLED_DEEPSLATE,
            Material.MOSSY_COBBLESTONE,
            Material.AMETHYST_BLOCK,
            // Руды
            Material.COAL_ORE,
            Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE,
            Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE,
            Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE,
            Material.DEEPSLATE_GOLD_ORE,
            Material.NETHER_GOLD_ORE,
            Material.REDSTONE_ORE,
            Material.DEEPSLATE_REDSTONE_ORE,
            Material.EMERALD_ORE,
            Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE,
            Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.NETHER_QUARTZ_ORE,
            Material.ANCIENT_DEBRIS,
            Material.GILDED_BLACKSTONE,
            Material.RAW_IRON_BLOCK,
            Material.RAW_GOLD_BLOCK,
            Material.RAW_COPPER_BLOCK
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
        if (tool.getType() == Material.AIR) return;

        int level = tool.getEnchantmentLevel(DRILL);
        if (level <= 0) return;

        // точный режим при зажатом Shift
        if (event.getPlayer().isSneaking()) return;

        int[] dims = LEVELS.get(level);
        if (dims == null) return;

        int width = dims[0];
        int height = dims[1];

        int rW = (width - 1) / 2;
        int up = (height - 1) / 2;
        int down = height / 2;

        Block origin = event.getBlock();
        Location loc = origin.getLocation();

        // определяем плоскость бурения
        double pitch = event.getPlayer().getPitch();
        float yaw = event.getPlayer().getYaw();
        while (yaw <= -180) yaw += 360;
        while (yaw > 180) yaw -= 360;

        boolean vertical = Math.abs(pitch) > 60;

        int minX, maxX, minY, maxY, minZ, maxZ;

        boolean direction = Math.abs(yaw) > 45 && Math.abs(yaw) < 135;
        if (vertical) {
            minY = 0; maxY = 0;
            if (direction) {
                // East/West
                minZ = -rW; maxZ = rW;
                if (yaw > 0) { // West
                    minX = -up; maxX = down;
                } else { // East
                    minX = -down; maxX = up;
                }
            } else {
                // North/South
                minX = -rW; maxX = rW;
                if (Math.abs(yaw) > 135) { // North
                    minZ = -up; maxZ = down;
                } else { // South
                    minZ = -down; maxZ = up;
                }
            }
        } else {
            minY = -down; maxY = up;
            if (direction) {
                // East/West
                minX = 0; maxX = 0;
                minZ = -rW; maxZ = rW;
            } else {
                // North/South
                minZ = 0; maxZ = 0;
                minX = -rW; maxX = rW;
            }
        }

        int broken_blocks = 0;
        event.getPlayer().setMetadata(META_DRILLING, new FixedMetadataValue(plugin, true));
        try {
            for (int dx = minX; dx <= maxX; dx++) {
                for (int dy = minY; dy <= maxY; dy++) {
                    for (int dz = minZ; dz <= maxZ; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block b = loc.clone().add(dx, dy, dz).getBlock();
                        if (!canBreak(b, tool)) continue;
                        
                        // Spawn flame particles
                        spawnFlameParticle(b.getLocation().add(0.5, 0.5, 0.5));
                        
                        b.breakNaturally(tool, true); // вызовет своё BlockBreakEvent, которое мы пропустим

                        broken_blocks++;
                    }
                }
            }
        } finally {
            event.getPlayer().removeMetadata(META_DRILLING, plugin);
        }

        if(event.getPlayer().getGameMode() == GameMode.CREATIVE)
            return;

        tool.damage(broken_blocks, event.getPlayer());
    }

    private void spawnFlameParticle(Location location) {
        WrapperPlayServerParticle packet = new WrapperPlayServerParticle(
                new Particle<>(ParticleTypes.FLAME),
                true,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                new Vector3f(0.1f, 0.1f, 0.1f),
                0.05f,
                5
        );

        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) > 50) continue;

            Vector toTarget = location.toVector().subtract(player.getEyeLocation().toVector());
            if (toTarget.lengthSquared() < 0.01) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
                continue;
            }

            Vector dir = player.getEyeLocation().getDirection();
            if (dir.normalize().dot(toTarget.normalize()) > 0.5) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
            }
        }
    }

    /** Проверка пригодности блока для бурения. */
    private boolean canBreak(Block block, ItemStack tool) {
        BlockData data = block.getBlockData();
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
        if (data.getDestroySpeed(tool, true) <= 0) return false;

        // 6) Разрешённые блоки (камень, сланец, руды)
        if (!DRILL_WHITELIST.contains(mat) && !mat.name().endsWith("_ORE")) return false;

        // 7) Проверка, что инструмент достаточно хорош для добычи дропа (tier check)
        return !block.getDrops(tool).isEmpty();
    }
}
