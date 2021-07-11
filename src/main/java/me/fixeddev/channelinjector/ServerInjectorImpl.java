package me.fixeddev.channelinjector;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import me.fixeddev.channelinjector.list.ListenableList;
import me.fixeddev.channelinjector.list.Listener;
import me.fixeddev.channelinjector.netty.ChannelInitializationInjector;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class ServerInjectorImpl implements ServerInjector {

    private final Plugin plugin;
    private final Logger pluginLogger;

    private Class<?> serverClass;
    private Field minecraftServerField;
    private Object minecraftServer;
    private Class<?> minecraftServerClass;
    private Method getServerConnectionMethod;

    private Object serverConnection;

    private Field openChannelsField;

    private List<ChannelFuture> openChannels;

    private ChannelInitializationInjector listenerInjector;

    private final List<ChannelInitInjector> injectorList;

    // Don't allow creation from other plugins.
    ServerInjectorImpl(Plugin plugin) {
        this.plugin = plugin;
        this.pluginLogger = plugin.getLogger();
        injectorList = new LinkedList<>();

        // Be fucking assholes and register this implementation just after creation.
        Bukkit.getServicesManager().register(ServerInjector.class, this, plugin, ServicePriority.High);
    }

    // This is freaking bad, but we know what we're doing
    // Don't suppress the warnings unless you know what you're doing
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void injectServer() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        serverConnection = this.serverConnection != null ? this.serverConnection : getServerConnection();

        if (openChannelsField == null) {
            openChannelsField = getOpenChannelsField(serverConnection.getClass());

            if (openChannelsField == null) {
                pluginLogger.info("This server version is not supported, failed to find the open channels field!");
                Bukkit.getPluginManager().disablePlugin(plugin);

                return;
            }

            openChannelsField.setAccessible(true);
        }

        if (openChannels == null) {
            openChannels = (List) openChannelsField.get(serverConnection);
        }

        if (listenerInjector == null) {
            listenerInjector = new ChannelInitializationInjector() {
                @Override
                protected void init(Channel channel) {
                    for (ChannelInitInjector channelInitInjector : injectorList) {
                        channelInitInjector.inject(channel);
                    }
                }
            };
        }

        for (ChannelFuture future : openChannels) {
            future.channel().pipeline().addFirst(listenerInjector);

            pluginLogger.info("Injected an already open channel " + future.toString());
        }

        List<ChannelFuture> newChannelsList = new ListenableList<>(openChannels);

        ((ListenableList<ChannelFuture>) newChannelsList).addListener(new Listener<ChannelFuture>() {
            @Override
            public void onAdd(ChannelFuture futureChannel) {
                futureChannel.channel().pipeline().addFirst(listenerInjector);

                pluginLogger.info("Added and injected a new open channel " + futureChannel.toString());
            }
        });

        newChannelsList = Collections.synchronizedList(newChannelsList);
        openChannels = newChannelsList;

        openChannelsField.set(serverConnection, newChannelsList);
        pluginLogger.info("Replaced original open channels list with a listenable one");
    }

    @Override
    public void removeServerInjection() {
        for (ChannelFuture openChannel : openChannels) {
            openChannel.channel().pipeline().remove(listenerInjector);
        }
    }

    @Override
    public void addChannelInjector(ChannelInitInjector initInjector) {
        injectorList.add(initInjector);
    }

    @Override
    public void addChannelInjector(int index, ChannelInitInjector initInjector) {
        injectorList.add(index, initInjector);
    }

    @Override
    public void removeChannelInjector(ChannelInitInjector toRemove) {
        injectorList.remove(toRemove);
    }

    @Override
    public void removeChannelInjector(int index) {
        injectorList.remove(index);
    }

    private Object getServerConnection() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, NoSuchFieldException {
        Server server = Bukkit.getServer();

        if (serverClass == null) {
            serverClass = server.getClass();
        }

        if (minecraftServerField == null) {
            minecraftServerField = serverClass.getDeclaredField("console");
            minecraftServerField.setAccessible(true);
        }

        if (minecraftServer == null) {
            minecraftServer = minecraftServerField.get(server);
        }

        if (minecraftServerClass == null) {
            minecraftServerClass = minecraftServer.getClass();
        }

        if (getServerConnectionMethod == null) {
            getServerConnectionMethod = minecraftServerClass.getMethod("getServerConnection");
        }

        return serverConnection = getServerConnectionMethod.invoke(minecraftServer);
    }

    private Field getOpenChannelsField(Class<?> serverConnectionClass) {
        Field openChannelsField = null;

        for (Field field : serverConnectionClass.getDeclaredFields()) {
            Type genericType = field.getGenericType();

            if (!(genericType instanceof ParameterizedType)) {
                continue;
            }

            ParameterizedType type = (ParameterizedType) field.getGenericType();

            if (type.getRawType() != List.class) {
                continue;
            }

            Type firstParameter = type.getActualTypeArguments()[0];

            if (!firstParameter.getTypeName().endsWith("ChannelFuture")) {
                continue;
            }

            openChannelsField = field;
            break;
        }

        return openChannelsField;
    }
}
