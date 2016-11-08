package de.rwth_aachen.afu.raspager;

import java.util.Deque;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;

final class FunkrufServerInitializer extends ChannelInitializer<SocketChannel> {

	private static final StringEncoder encoder = new StringEncoder();
	private static final StringDecoder decoder = new StringDecoder();
	private final FunkrufServerHandler handler;
	private final SslContext sslContext;

	public FunkrufServerInitializer(Deque<Message> messageQueue) {
		this(null, messageQueue);
	}

	public FunkrufServerInitializer(SslContext sslContext, Deque<Message> messageQueue) {
		this.sslContext = sslContext;
		this.handler = new FunkrufServerHandler(messageQueue);
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
