package me.fixeddev.channelinjector;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;

public interface ServerInjector {

    final Object LOCK = new Object();

    // You shouldn't call this method more than once before calling removeServerInjection()
    void injectServer() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException;

    // Removes our injection from all the netty channels open by the server, you shouldn't call this method
    // before injectServer() is called
    void removeServerInjection();

    void addChannelInjector(ChannelInitInjector initInjector);

    void addChannelInjector(int index, ChannelInitInjector initInjector);

    void removeChannelInjector(ChannelInitInjector toRemove);

    void removeChannelInjector(int index);

    static ServerInjector getOrCreate(Plugin plugin) {
        ServerInjector injector = Bukkit.getServicesManager().load(ServerInjector.class);

        if (injector == null) {
            synchronized (LOCK) {
                injector = Bukkit.getServicesManager().load(ServerInjector.class);

                if (injector == null) {
                    // automatically registered
                    injector = new ServerInjectorImpl(plugin);
                }
            }

        }

        return injector;
    }
}
