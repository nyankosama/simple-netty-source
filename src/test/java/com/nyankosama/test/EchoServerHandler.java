package com.nyankosama.test;

import org.jboss.netty.channel.core.ChannelHandlerContext;
import org.jboss.netty.channel.event.ChannelStateEvent;
import org.jboss.netty.channel.event.MessageEvent;
import org.jboss.netty.channel.core.impl.SimpleChannelUpstreamHandler;

/**
 * Created by hlr@superid.cn on 2014/8/15.
 */
public class EchoServerHandler extends SimpleChannelUpstreamHandler {

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        e.getChannel().write(e.getMessage());
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelOpen(ctx, e);
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelConnected(ctx, e);
    }
}
