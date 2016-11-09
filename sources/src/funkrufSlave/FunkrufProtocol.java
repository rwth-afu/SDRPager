package funkrufSlave;

import java.io.PrintWriter;
import java.util.Deque;

public class FunkrufProtocol {
	public Deque<Message> messageQueue = null;
	public static final int E_SUCCESS = 0, E_ERROR = -1, E_RETRY = 1;

	private Log log = null;

	private void log(String message, int type) {
		log(message, type, Log.DEBUG_TCP);
	}

	private void log(String message, int type, int logLevel) {
		if (this.log != null) {
			this.log.println(message, type, logLevel);
		}
	}

	public FunkrufProtocol(Deque<Message> messageQueue, Log log) {
		this.messageQueue = messageQueue;
		this.log = log;
	}

	public void handle(String msg, PrintWriter pw, TimeSlots timeSlots) {
		int errorState = E_SUCCESS;

		String[] parts = msg.split(":", 5);
		char type = msg.charAt(0);

		log("FunkrufProtocol: handle# type (" + type + ")", Log.MS);

		switch (type) {
			case '#':
				// Funkrufe
				try {
					int messageID = Integer.parseInt(parts[0].substring(1, 3), 16);
					this.messageQueue.push(new Message(parts));

					messageID = (messageID + 1) % 256;

					send(String.format("#%02x +", messageID), pw);
				} catch (NumberFormatException e) {
					log("FunkrufProtocol: handle# (#) NumberFormatException", Log.ERROR);
					errorState = E_ERROR;

				}

				break;
			case '2':
				String ident = parts[1];
				if (Main.scheduler == null)
					log("Scheduler ist null!", Log.ERROR);
				int time = Main.scheduler.getTime();

				send(String.format("2:%s:%04x", ident, time), pw);
				ack(errorState, pw);

				break;
			case '3':
				// correct system time
				try {
					int delay;

					if (parts[1].charAt(0) == '+') {
						parts[1] = parts[1].substring(1);
						delay = Integer.parseInt(parts[1], 16);
					} else {
						parts[1] = parts[1].substring(1);
						delay = -Integer.parseInt(parts[1], 16);
					}

					if (Main.scheduler == null)
						log("Scheduler ist null!", Log.ERROR);
					Main.scheduler.correctTime(delay);
				} catch (NumberFormatException e) {
					log("FunkrufProtocol: handle# (3) NumberFormatException", Log.ERROR);
					errorState = E_ERROR;
				}

				ack(errorState, pw);

				break;
			case '4':
				// set slots
				timeSlots.setSlots(parts[1]);

				log(timeSlots.getSlots(), Log.COMMUNICATION, Log.DEBUG_CONNECTION);

				ack(errorState, pw);

				break;
			default:
				log("FunkrufProtocol: handle# unknown type", Log.ERROR);
				errorState = E_ERROR;

				break;
		}
	}

	public void ack(int errorState, PrintWriter pw) {
		String outputBuffer = "";

		switch (errorState) {
			case FunkrufProtocol.E_SUCCESS:
				outputBuffer = "+";
				break;

			case FunkrufProtocol.E_ERROR:
				outputBuffer = "-";
				break;

			case FunkrufProtocol.E_RETRY:
				outputBuffer = String.format("%%");
				break;
		}

		if (outputBuffer != null && !outputBuffer.equals("")) {
			pw.printf(outputBuffer + "\r\n");
			log("FunkrufProtocol: outputBuffer# " + outputBuffer, Log.SM);
		}
	}

	public void send(String answer, PrintWriter pw) {
		if (!answer.equals(""))
			log("FunkrufProtocol: send# " + answer, Log.SM);

		pw.printf("%s\r\n", answer);
	}
}