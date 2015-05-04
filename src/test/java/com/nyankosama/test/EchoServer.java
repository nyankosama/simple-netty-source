package com.nyankosama.test;


import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.core.ChannelPipeline;
import org.jboss.netty.channel.core.ChannelPipelineFactory;
import org.jboss.netty.channel.core.Channels;
import org.jboss.netty.channel.socket.nio.server.NioServerSocketChannelFactory;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Created by hlr@superid.cn on 2014/8/15.
 */
public class EchoServer {

    public void run() throws Exception{
        // Configure the server.
        System.out.println("server start");
        ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));

        // Set up the pipeline factory.
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(new EchoServerHandler());
            }
        });

        // Bind and start to accept incoming connections.
        bootstrap.bind(new InetSocketAddress(9123));
    }

    public static void main(String args[]) throws Exception {
        new EchoServer().run();
    }
}
