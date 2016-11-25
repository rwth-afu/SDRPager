package de.rwth_aachen.afu.raspager;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * This class initializes a new socket channel when a connection is accepted.
 */
final class ServerInitializer extends ChannelInitializer<SocketChannel> {

	private static final StringEncoder encoder = new StringEncoder();
	private static final StringDecoder decoder = new StringDecoder();
	private final ServerHandler handler;

	/**
	 * Constructs a new server initializer instance.
	 * 
	 * @param callbacks
	 *            Callback provider
	 */
	public ServerInitializer(ServerCallbacks callbacks) {
		this.handler = new ServerHandler(callbacks);
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pip = ch.pipeline();

		pip.addLast(new DelimiterBasedFrameDecoder(4096, Delimiters.lineDelimiter()));
		// Static as both encoder and decoder are sharable.
		pip.addLast(decoder);
		pip.addLast(encoder);
		// Out custom message handler
		pip.addLast(handler);
	}

}
