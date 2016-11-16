package de.rwth_aachen.afu.raspager;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;

/**
 * This class initializes a new socket channel when a connection is accepted.
 */
final class ServerInitializer extends ChannelInitializer<SocketChannel> {

	private static final StringEncoder encoder = new StringEncoder();
	private static final StringDecoder decoder = new StringDecoder();
	private final ServerHandler handler;
	private final SslContext sslContext;

	/**
	 * Constructs a new server initializer instance.
	 * 
	 * @param callbacks
	 *            Callback provider
	 */
	public ServerInitializer(ServerCallbacks callbacks) {
		this(null, callbacks);
	}

	/**
	 * Constructs a new server initializer instance.
	 * 
	 * @param sslContext
	 *            SSL context to use.
	 * @param callbacks
	 *            Callback provider.
	 */
	public ServerInitializer(SslContext sslContext, ServerCallbacks callbacks) {
		this.sslContext = sslContext;
		this.handler = new ServerHandler(callbacks);
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pip = ch.pipeline();

		if (sslContext != null) {
			pip.addLast(sslContext.newHandler(ch.alloc()));
		}

		pip.addLast(new DelimiterBasedFrameDecoder(4096, Delimiters.lineDelimiter()));
		// Static as both encoder and decoder are sharable.
		pip.addLast(decoder);
		pip.addLast(encoder);
		// Out custom message handler
		pip.addLast(handler);
	}

}
