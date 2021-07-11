package me.fixeddev.channelinjector;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

public final class ChannelInjectorPlugin extends JavaPlugin implements Listener {

    private ServerInjector injector;

    @Override
    public void onLoad() {
        injector = ServerInjector.getOrCreate(this);
    }

    @Override
    public void onEnable() {
        // Just in case someone replaces our implementation
        injector = Bukkit.getServicesManager().load(ServerInjector.class);

        try {
            injector.injectServer();
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            this.getLogger().log(Level.SEVERE, "This server version is not supported!", e);

            this.setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        injector.removeServerInjection();
    }
}
