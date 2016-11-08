package de.rwth_aachen.afu.raspager;

import java.util.Deque;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

final class FunkrufServerHandler extends SimpleChannelInboundHandler<String> {

	private static final Logger log = Logger.getLogger(FunkrufServerHandler.class.getName());
	private final Deque<Message> messageQueue;

	public FunkrufServerHandler(Deque<Message> messageQueue) {
		if (messageQueue == null) {
			throw new IllegalArgumentException("Invalid message queue.");
		}

		this.messageQueue = messageQueue;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.fine("Accepted new connection.");
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log.fine("Connection closed.");
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String request) throws Exception {
		if (request.isEmpty()) {
			log.warning("Received empty request.");
			return;
		}

		char type = request.charAt(0);
		log.log(Level.FINE, "Received message of type: {0}", type);

		switch (type) {
		case '#':
			handleMessage(ctx, request);
			break;
		case '2':
			handleMasterIdent(ctx, request);
			break;
		case '3':
			handleTimeCorrection(ctx, request);
			break;
		case '4':
			handleTimeSlots(ctx, request);
			break;
		default:
			log.log(Level.WARNING, "Invalid message type: {0}", type);
			ackError(ctx);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.log(Level.SEVERE, "Exception caught in channel handler.", cause);
	}

	/**
	 * Adds a message to the message queue.
	 * 
	 * @param ctx
	 *            Client connection.
	 * @param request
	 *            Request which contains the message.
	 */
	private void handleMessage(ChannelHandlerContext ctx, String request) {
		try {
			// Add message to our queue
			messageQueue.push(new Message(request));

			// Send message ID as response
			int messageId = Integer.parseInt(request.substring(1, 3), 16);
			String response = String.format("#%02x +\r\n", messageId);
			ctx.write(response);
		} catch (Throwable t) {
			log.log(Level.WARNING, "Failed to add message or send response.", t);
			ackError(ctx);
		}
	}

	/**
	 * Handles the master identify message.
	 * 
	 * @param ctx
	 *            Client connection.
	 * @param request
	 *            Request
	 */
	private void handleMasterIdent(ChannelHandlerContext ctx, String request) {
		try {
			String[] parts = request.split(":", 2);
			// TODO impl
			int time = Main.scheduler.getTime();

			String response = String.format("2:%s:%04x\r\n", parts[1], time);
			ctx.write(response);
			ackSuccess(ctx);
		} catch (Throwable t) {
			log.log(Level.WARNING, "Failed to handle master packet.", t);
			ackError(ctx);
		}
	}

	/**
	 * Handles the correct time message.
	 * 
	 * @param ctx
	 *            Client connection
	 * @param request
	 *            Time data
	 */
	private void handleTimeCorrection(ChannelHandlerContext ctx, String request) {
		try {
			String[] parts = request.split(":", 2);
			int delay = 0;
			if (parts[1].charAt(1) == '+') {
				delay = Integer.parseInt(parts[1].substring(1), 16);
			} else {
				// No need to strip leading "-" char
				delay = Integer.parseInt(parts[1], 16);
			}

			// TODO impl
			Main.scheduler.correctTime(delay);

			ackSuccess(ctx);
		} catch (Throwable t) {
			log.log(Level.WARNING, "Failed to correct time.", t);
			ackError(ctx);
		}
	}

	/**
	 * Handles the set time slots message.
	 * 
	 * @param ctx
	 *            Client connection.
	 * @param request
	 *            Time slot data.
	 */
	private void handleTimeSlots(ChannelHandlerContext ctx, String request) {
		try {
			String[] parts = request.split(":", 2);
			// TODO impl
			Main.timeSlots.setSlots(parts[1]);
			ackSuccess(ctx);
		} catch (Throwable t) {
			log.log(Level.WARNING, "Failed to set time slots.", t);
			ackError(ctx);
		}
	}

	/**
	 * Sends a success ack to the client.
	 * 
	 * @param ctx
	 *            Client connection.
	 */
	private void ackSuccess(ChannelHandlerContext ctx) {
		ctx.write("+\r\n");
	}

	/**
	 * Sends an error ack to the client.
	 * 
	 * @param ctx
	 *            Client connection.
	 */
	private void ackError(ChannelHandlerContext ctx) {
		ctx.write("-\r\n");
	}

	/**
	 * Sends a retry ack to the client.
	 * 
	 * @param ctx
	 *            Client connection.
	 */
	@SuppressWarnings("unused")
	private void ackRetry(ChannelHandlerContext ctx) {
		ctx.write("%\r\n");
	}
}
