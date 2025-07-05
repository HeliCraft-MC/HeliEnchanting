package ru.helicraft.helienchanting.enchant;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.tag.PostFlattenTagRegistrar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Drill — «Бур» (Area Mining).
 * <p>
 * Уровень I — 2×1, II — 2×3, III — 3×3, IV — 3×5, V — 5×5.
 */
public final class DrillEnchant {

    /** Registry key для кастомного зачарования. */
    public static final Key KEY = Key.key("helienchanting:drill");

    public static void register(final @NotNull BootstrapContext ctx) {

        /* ---------- 1. Регистрируем запись в реестре ----------- */
        ctx.getLifecycleManager().registerEventHandler(
                RegistryEvents.ENCHANTMENT.compose().newHandler(event -> event.registry().register(
                        EnchantmentKeys.create(KEY),
                        builder -> builder
                                .description(Component.text("Бур"))
                                .supportedItems(event.getOrCreateTag(ItemTypeTagKeys.PICKAXES))   // все кирки
                                .primaryItems(event.getOrCreateTag(ItemTypeTagKeys.PICKAXES))
                                .activeSlots(EquipmentSlotGroup.MAINHAND)
                                .maxLevel(5)
                                .weight(1)                                   // исключительно редкое
                                .anvilCost(5)
                                .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(30, 3))
                                .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(45, 5))
                ))
        );

        /* ------- 2. Добавляем Drill в ванильные теги ---------- */
        ctx.getLifecycleManager().registerEventHandler(
                LifecycleEvents.TAGS.postFlatten(RegistryKey.ENCHANTMENT),
                (ReloadableRegistrarEvent<PostFlattenTagRegistrar<Enchantment>> event) -> {
                    PostFlattenTagRegistrar<Enchantment> reg = event.registrar();

                    reg.addToTag(EnchantmentTagKeys.IN_ENCHANTING_TABLE,
                            Set.of(EnchantmentKeys.create(KEY)));           // доступно в чар-столе
                    reg.addToTag(EnchantmentTagKeys.TRADEABLE,
                            Set.of(EnchantmentKeys.create(KEY)));           // может появиться у жителей
                    reg.addToTag(EnchantmentTagKeys.ON_RANDOM_LOOT,
                            Set.of(EnchantmentKeys.create(KEY)));           // генерируется в сундуках/рыбалке
                }
        );
    }
}
