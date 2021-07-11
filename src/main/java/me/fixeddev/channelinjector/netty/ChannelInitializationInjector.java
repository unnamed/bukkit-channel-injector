package me.fixeddev.channelinjector.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;

public abstract class ChannelInitializationInjector extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        Channel channel = (Channel) o;

        channel.pipeline().addLast(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) {
                channel.pipeline().addLast(new ChannelDuplexHandler() {
                    private boolean injected;

                    @Override
                    public void channelRead(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
                        if (!injected) {
                            channelHandlerContext.pipeline().remove(this);

                            init(channelHandlerContext.channel());

                            injected = !injected;
                        }

                        super.channelRead(channelHandlerContext, o);
                    }

                    @Override
                    public void write(ChannelHandlerContext channelHandlerContext, Object o, ChannelPromise channelPromise) throws Exception {
                        if (!injected) {
                            channelHandlerContext.pipeline().remove(this);

                            init(channelHandlerContext.channel());

                            injected = !injected;
                        }

                        super.write(channelHandlerContext, o, channelPromise);
                    }
                });
            }
        });

        super.channelRead(channelHandlerContext, o);
    }

    protected abstract void init(Channel channel);
}
