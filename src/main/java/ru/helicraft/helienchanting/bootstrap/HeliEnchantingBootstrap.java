package ru.helicraft.helienchanting.bootstrap;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import ru.helicraft.helienchanting.enchant.DrillEnchant;

public final class HeliEnchantingBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {
        DrillEnchant.register(context);
    }
}
