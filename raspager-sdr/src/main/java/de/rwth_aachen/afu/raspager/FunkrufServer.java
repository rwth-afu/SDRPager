package de.rwth_aachen.afu.raspager;

import java.util.Deque;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

final class FunkrufServer implements Runnable {
	private static final Logger log = Logger.getLogger(FunkrufServer.class.getName());
	private final int port;
	private final Deque<Message> messageQueue;
	private ChannelFuture serverFuture;

	public FunkrufServer(int port, Deque<Message> messageQueue) {
		this.port = port;
		this.messageQueue = messageQueue;
	}

	@Override
	public void run() {
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		try {
			FunkrufServerInitializer handler = new FunkrufServerInitializer(messageQueue);
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup);
			b.channel(NioServerSocketChannel.class);
			b.childHandler(handler);
			serverFuture = b.bind(port).sync();
			// Wait for shutdown
			serverFuture.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			log.log(Level.SEVERE, "Funkruf server interrupted.", e);
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	public void shutdown() {
		try {
			if (serverFuture != null) {
				serverFuture.channel().close().sync();
			}
		} catch (InterruptedException e) {
			log.log(Level.WARNING, "Close interrupted.", e);
		}
	}
}
