package de.rwth_aachen.afu.raspager;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

final class Server implements Runnable {
	private static final Logger log = Logger.getLogger(Server.class.getName());
	private final Configuration config;
	private final ServerCallbacks callbacks = new ServerCallbacks();
	private ChannelFuture serverFuture;

	public Server(Configuration config) {
		this.config = config;
	}

	public void setAddMessageHandler(Consumer<Message> messageHandler) {
		callbacks.setAddMessageHandler(messageHandler);
	}

	public void setTimeCorrectionHandler(IntConsumer timeCorrectionHandler) {
		callbacks.setTimeCorrectionHandler(timeCorrectionHandler);
	}

	public void setTimeSlotsHandler(Consumer<String> timeSlotsHandler) {
		callbacks.setTimeSlotsHandler(timeSlotsHandler);
	}

	public void setGetTimeHandler(IntSupplier timeHandler) {
		callbacks.setTimeHandler(timeHandler);
	}

	@Override
	public void run() {
		int port = config.getInt(ConfigKeys.NET_PORT, 1337);

		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		try {
			ServerInitializer handler = new ServerInitializer(callbacks);
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
