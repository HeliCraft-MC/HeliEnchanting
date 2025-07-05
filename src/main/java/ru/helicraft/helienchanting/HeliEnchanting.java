package ru.helicraft.helienchanting;

import org.bukkit.plugin.java.JavaPlugin;
import ru.helicraft.helienchanting.listener.DrillListener;

public final class HeliEnchanting extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new DrillListener(this), this);
        getLogger().info("HeliEnchanting enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("HeliEnchanting disabled.");
    }
}
