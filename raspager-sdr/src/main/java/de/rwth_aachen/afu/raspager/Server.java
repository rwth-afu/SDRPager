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
import io.netty.handler.ipfilter.RuleBasedIpFilter;

/**
 * RasPager server implementation.
 */
final class Server implements Runnable {
	private static final Logger log = Logger.getLogger(Server.class.getName());
	private final Configuration config;
	private final ServerCallbacks callbacks = new ServerCallbacks();
	private ChannelFuture serverFuture;

	/**
	 * Constructs a new server instance.
	 * 
	 * @param config
	 *            Configuration
	 */
	public Server(Configuration config) {
		this.config = config;
	}

	/**
	 * Sets the new message handler.
	 * 
	 * @param messageHandler
	 *            Handler to use.
	 */
	public void setAddMessageHandler(Consumer<Message> messageHandler) {
		callbacks.setAddMessageHandler(messageHandler);
	}

	/**
	 * Sets the time correction handler.
	 * 
	 * @param timeCorrectionHandler
	 *            Handler to use.
	 */
	public void setTimeCorrectionHandler(IntConsumer timeCorrectionHandler) {
		callbacks.setTimeCorrectionHandler(timeCorrectionHandler);
	}

	/**
	 * Sets the time slots handler.
	 * 
	 * @param timeSlotsHandler
	 *            Handler to use.
	 */
	public void setTimeSlotsHandler(Consumer<String> timeSlotsHandler) {
		callbacks.setTimeSlotsHandler(timeSlotsHandler);
	}

	/**
	 * Sets the get time handler.
	 * 
	 * @param timeHandler
	 *            Handler to use.
	 */
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
			b.handler(new RuleBasedIpFilter(new MasterServerFilter(config)));
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

	/**
	 * Stops the server if running. This method will block until the server is
	 * stopped.
	 */
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
