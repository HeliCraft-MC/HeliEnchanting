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

    private static final String META_DRILLING = "helienchanting:drilling";

    private static final Map<Integer, int[]> LEVELS = new HashMap<>();
    static {
        LEVELS.put(1, new int[]{1, 2});
        LEVELS.put(2, new int[]{3, 2});
        LEVELS.put(3, new int[]{3, 3});
        LEVELS.put(4, new int[]{5, 3});
        LEVELS.put(5, new int[]{5, 5});
    }

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
        if (event.getPlayer().hasMetadata(META_DRILLING)) return;

        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (tool.getType() == Material.AIR) return;

        int level = tool.getEnchantmentLevel(DRILL);
        if (level <= 0) return;

        if (event.getPlayer().isSneaking()) return;

        int[] dims = LEVELS.get(level);
        if (dims == null) return;

        int width = dims[0];
        int height = dims[1];
        
        // ГЛУБИНА: на сколько блоков вперед пробивает бур
        int depth = (level > 3) ? 2 : 1; 

        int rW = (width - 1) / 2;
        int up = (height - 1) / 2;
        int down = height / 2;

        Block origin = event.getBlock();
        Location loc = origin.getLocation();

        double pitch = event.getPlayer().getPitch();
        float yaw = event.getPlayer().getYaw();
        while (yaw <= -180) yaw += 360;
        while (yaw > 180) yaw -= 360;

        boolean vertical = Math.abs(pitch) > 60;
        int minX, maxX, minY, maxY, minZ, maxZ;
        boolean direction = Math.abs(yaw) > 45 && Math.abs(yaw) < 135;

        if (vertical) {
            minY = (pitch > 0) ? -depth : 0; 
            maxY = (pitch > 0) ? 0 : depth;
            if (direction) {
                minZ = -rW; maxZ = rW;
                minX = (yaw > 0) ? -up : -down; 
                maxX = (yaw > 0) ? down : up;
            } else {
                minX = -rW; maxX = rW;
                minZ = (Math.abs(yaw) > 135) ? -up : -down;
                maxZ = (Math.abs(yaw) > 135) ? down : up;
            }
        } else {
            minY = -down; maxY = up;
            if (direction) {
                minX = (yaw > 0) ? -depth : 0; 
                maxX = (yaw > 0) ? 0 : depth;
                minZ = -rW; maxZ = rW;
            } else {
                minZ = (Math.abs(yaw) > 135) ? -depth : 0;
                maxZ = (Math.abs(yaw) > 135) ? 0 : depth;
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
                        
                        spawnFlameParticle(b.getLocation().add(0.5, 0.5, 0.5));
                        b.breakNaturally(tool, true); 
                        broken_blocks++;
                    }
                }
            }
        } finally {
            event.getPlayer().removeMetadata(META_DRILLING, plugin);
        }

        if(event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
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

    private boolean canBreak(Block block, ItemStack tool) {
        Material mat = block.getType();

        // Не ломаем бедрок и порталы
        if (UNBREAKABLE.contains(mat)) return false;

        // Не ломаем воздух и жидкости
        if (mat.isAir() || !mat.isSolid()) return false;

        // Не ломаем неразрушимые блоки (твёрдость < 0)
        if (mat.getHardness() < 0F) return false;

        // Проверка, что блок вообще можно повредить этим инструментом
        // (убрана жесткая проверка PreferredTool, чтобы кирка могла ломать землю)
        if (block.getBlockData().getDestroySpeed(tool, true) <= 0) return false;

        return true;
    }
}
