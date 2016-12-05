package de.rwth_aachen.afu.raspager;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * RasPager server implementation.
 * 
 * @author Philipp Thiel
 */
final class Server implements Runnable {
	private static final Logger log = Logger.getLogger(Server.class.getName());
	private static final StringEncoder encoder = new StringEncoder();
	private static final StringDecoder decoder = new StringDecoder();
	private final ServerHandler protocol = new ServerHandler();
	private final MasterServerFilter ipFilter;
	private final int port;
	private ChannelFuture serverFuture;

	/**
	 * Creates a new server instance.
	 * 
	 * @param port
	 *            Port number to listen on.
	 * @param masters
	 *            Master server list (null to accept all incoming connections).
	 */
	public Server(int port, String[] masters) {
		this.port = port;

		if (masters != null) {
			ipFilter = new MasterServerFilter(masters);
		} else {
			ipFilter = null;
		}
	}

	/**
	 * Sets the new message handler.
	 * 
	 * @param messageHandler
	 *            Handler to use.
	 */
	public void setAddMessageHandler(Consumer<Message> messageHandler) {
		protocol.setAddMessageHandler(messageHandler);
	}

	/**
	 * Sets the time correction handler.
	 * 
	 * @param timeCorrectionHandler
	 *            Handler to use.
	 */
	public void setTimeCorrectionHandler(IntConsumer timeCorrectionHandler) {
		protocol.setTimeCorrectionHandler(timeCorrectionHandler);
	}

	/**
	 * Sets the time slots handler.
	 * 
	 * @param timeSlotsHandler
	 *            Handler to use.
	 */
	public void setTimeSlotsHandler(Consumer<String> timeSlotsHandler) {
		protocol.setTimeSlotsHandler(timeSlotsHandler);
	}

	/**
	 * Sets the get time handler.
	 * 
	 * @param timeHandler
	 *            Handler to use.
	 */
	public void setGetTimeHandler(IntSupplier timeHandler) {
		protocol.setTimeHandler(timeHandler);
	}

	/**
	 * Sets the handler for new connections.
	 * 
	 * @param handler
	 *            Handler to use.
	 */
	public void setConnectionHandler(Runnable handler) {
		protocol.setConnectHandler(handler);
	}

	/**
	 * Sets the handler for closed connections.
	 * 
	 * @param handler
	 *            Handler to use.
	 */
	public void setDisconnectHandler(Runnable handler) {
		protocol.setDisconnectHandler(handler);
	}

	@Override
	public void run() {
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup);
			b.channel(NioServerSocketChannel.class);

			// Define channel initializer
			b.childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline pip = ch.pipeline();

					if (ipFilter != null) {
						pip.addLast("filter", ipFilter);
					}

					pip.addLast(new DelimiterBasedFrameDecoder(4096, Delimiters.lineDelimiter()));
					// Static as both encoder and decoder are sharable.
					pip.addLast("decoder", decoder);
					pip.addLast("encoder", encoder);
					// Our custom message handler
					pip.addLast("protocol", protocol);
				}
			});

			serverFuture = b.bind(port).sync();
			// Wait for shutdown
			serverFuture.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			log.log(Level.SEVERE, "Funkruf server interrupted.", e);
		} catch (Throwable t) {
			log.log(Level.SEVERE, "Exception in server.", t);
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	/**
	 * Stops the server if it is running. This method will block until the
	 * server is stopped.
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
