package ru.helicraft.helienchanting;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;
import ru.helicraft.helienchanting.listener.DrillListener;

public final class HeliEnchanting extends JavaPlugin {

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().reEncodeByDefault(false)
                .checkForUpdates(false)
                .bStats(true);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        PacketEvents.getAPI().init();
        getServer().getPluginManager().registerEvents(new DrillListener(this), this);
        getLogger().info("HeliEnchanting enabled!");
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();

        getLogger().info("HeliEnchanting disabled.");
    }
}
