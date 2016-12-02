package de.rwth_aachen.afu.raspager;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * This class handles incoming packets like new messages to send from a client
 * connection.
 */
@Sharable
final class ServerHandler extends SimpleChannelInboundHandler<String> {
	private static final Logger log = Logger.getLogger(ServerHandler.class.getName());
	private Consumer<Message> messageHandler;
	private IntConsumer timeCorrectionHandler;
	private Consumer<String> timeSlotsHandler;
	private IntSupplier timeHandler;

	/**
	 * Sets the handler for new message packets.
	 * 
	 * @param messageHandler
	 *            Handler to use.
	 */
	public void setAddMessageHandler(Consumer<Message> messageHandler) {
		this.messageHandler = messageHandler;
	}

	/**
	 * Sets the handler for time correction packets.
	 * 
	 * @param timeCorrectionHandler
	 *            Handler to use.
	 */
	public void setTimeCorrectionHandler(IntConsumer timeCorrectionHandler) {
		this.timeCorrectionHandler = timeCorrectionHandler;
	}

	/**
	 * Sets the handler for time slot activation packets.
	 * 
	 * @param timeSlotsHandler
	 *            Handler to use.
	 */
	public void setTimeSlotsHandler(Consumer<String> timeSlotsHandler) {
		this.timeSlotsHandler = timeSlotsHandler;
	}

	/**
	 * Sets the handler for time packets.
	 * 
	 * @param timeHandler
	 *            Handler to use.
	 */
	public void setTimeHandler(IntSupplier timeHandler) {
		this.timeHandler = timeHandler;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.fine("Accepted new connection.");

		// TODO Adjust version string
		ctx.write("[SDRPager v1.2-SCP-#2345678]\r\n");
		ctx.flush();
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
			handleMasterIdentify(ctx, request);
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
		// TODO Keep connection open?
		ctx.close();
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
			if (messageHandler != null) {
				messageHandler.accept(new Message(request));

				// Send message ID as response
				int messageId = Integer.parseInt(request.substring(1, 3), 16);
				messageId = (messageId + 1) % 256;
				String response = String.format("#%02x +\r\n", messageId);
				ctx.write(response);
			} else {
				log.severe("No message handler registered.");
				ackError(ctx);
			}
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
	private void handleMasterIdentify(ChannelHandlerContext ctx, String request) {
		try {
			if (timeHandler != null) {
				int time = timeHandler.getAsInt();
				String[] parts = request.split(":", 2);
				String response = String.format("2:%s:%04x\r\n", parts[1], time);
				ctx.write(response);
				ackSuccess(ctx);
			} else {
				log.severe("No time handler registered.");
				ackError(ctx);
			}
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
			if (timeCorrectionHandler != null) {
				String[] parts = request.split(":", 2);
				int delay = 0;
				if (parts[1].charAt(1) == '+') {
					delay = Integer.parseInt(parts[1].substring(1), 16);
				} else {
					// No need to strip leading "-" char
					delay = Integer.parseInt(parts[1], 16);
				}

				timeCorrectionHandler.accept(delay);

				ackSuccess(ctx);
			} else {
				log.severe("No set time correction handler registered.");
				ackError(ctx);
			}
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
		log.fine("TimeSlots");
		try {
			if (timeSlotsHandler != null) {
				String[] parts = request.split(":", 2);
				timeSlotsHandler.accept(parts[1]);
				ackSuccess(ctx);
			} else {
				log.severe("No set time slots handler registered.");
				ackError(ctx);
			}
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
