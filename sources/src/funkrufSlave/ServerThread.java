package funkrufSlave;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Deque;

public class ServerThread extends Thread {

	private int port;
	private ServerSocket socket;
	private boolean running = true;
	private ArrayList<SocketThread> threads;
	private FunkrufProtocol protocol;

	private Log log = null;

	// write message into log file (log level normal)
	private void log(String message, int type) {
		log(message, type, Log.DEBUG_TCP);
	}

	// write message with given log level into log file
	private void log(String message, int type, int logLevel) {
		// is there a log file?
		if (this.log != null) {
			// write message with given log level into log file
			this.log.println(message, type, logLevel);
		}
	}

	// constructor
	public ServerThread(int port, Deque<Message> messageQueue, Log log) {
		super("ServerThread");

		this.log = log;

		this.port = port;
		this.socket = null;

		// create thread list
		this.threads = new ArrayList<SocketThread>();

		// create funkruf protocol with given messageQueue
		this.protocol = new FunkrufProtocol(messageQueue, this.log);

	}

	// set port
	public void setPort(int port) {
		// is server running?
		if (!this.running) {
			// if not, set port
			this.port = port;
		}
	}

	// halt server
	public void halt() {

		// set running to false
		this.running = false;

		for (int i = 0; i < this.threads.size(); ++i) {
			this.threads.get(i).halt();
		}

		// cancel listening (accept)
		try {
			// if socket is open
			if (this.socket != null) {
				// close socket
				this.socket.close();
			}

		} catch (IOException e) {
			// log("ServerThread: accept abgebrochen");
		}

	}

	// remove thread from thread list
	public void removeSocketThread(SocketThread thread) {
		// remove thread
		this.threads.remove(thread);
		log("ServerThread: Client disconnected.", Log.DEBUG_TCP);

		// are there threads left?
		if (this.threads.size() == 0) {
			// if not, set connection status to false
			Main.setStatus(false);
		}
	}

	// "main"
	@Override
	public void run() {

		try {
			// open socket and listen on port
			this.socket = new ServerSocket(this.port);

		} catch (IOException e) {
			// there was an error (port is busy)
			log("ServerThread: Port " + this.port + " could not been initialized!", Log.ERROR, Log.NORMAL);

			// set running to false
			this.running = false;

			// halt server and write error message
			Main.serverError("Server konnte nicht auf Port " + this.port + " initialisiert werden!");

			return;
		}

		// set running to true
		this.running = true;

		// as long as server is running
		while (this.running) {

			Socket tmpSocket = null;

			try {
				// wait for client
				tmpSocket = this.socket.accept();

			} catch (SocketException e) {
				// occures when halting server
				return;
			} catch (IOException e) {
				// there was an error
				log("ServerThread: Error socket.accept()", Log.ERROR, Log.NORMAL);

			}

			// is there a client?
			if (tmpSocket != null) {
				// yes
				log("ServerThread: new connection", Log.COMMUNICATION);

				// set connection status to true
				Main.setStatus(true);

				// get client ip
				String ip = tmpSocket.getInetAddress().getHostAddress();

				// check if client is not a master
				if (!Main.config.isMaster(ip)) {
					log("ServerThread: " + ip + " ist kein eingetragener Master.", Log.ERROR);

					try {
						// open writer
						PrintWriter pw = new PrintWriter(tmpSocket.getOutputStream(), true);

						// send message
						pw.println("Kein eingetragener Master!");

						// close writer and socket
						pw.close();
						tmpSocket.close();

					} catch (IOException e) {
						// there was an error
						log("ServerThread: tmpSocket.close()# IOException", Log.ERROR);
					}

					continue;
				}

				// create thread and start it
				SocketThread thread = new SocketThread(tmpSocket, this.protocol, this.log);
				thread.start();

				// add thread to thread list
				this.threads.add(thread);

			}
		}

		// server is not running anymore

		// halt all socket threads in thread list
		for (int i = 0; i < this.threads.size(); i++) {
			this.threads.get(i).halt();
		}

		// set connection status to false
		Main.setStatus(false);
	}
}
