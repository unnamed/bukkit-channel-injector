package me.fixeddev.channelinjector;

import io.netty.channel.Channel;

public interface ChannelInitInjector {
    /**
     * Injects a change into a newly initialized channel.
     *
     * @param channel The channel to inject into.
     */
    void inject(Channel channel);
}
