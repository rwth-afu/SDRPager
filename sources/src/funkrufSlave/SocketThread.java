package funkrufSlave;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketThread extends Thread {
	private Socket socket = null;
	private FunkrufProtocol protocol;
	
	private boolean running = false;

	private PrintWriter pw = null;
	private BufferedReader reader = null;

	private Log log = null;
	
	
	// write message into log file (log level normal)
	private void log(String message, int type) {
		log(message, type, Log.DEBUG_TCP);
	}
	
	// write message with given log level into log file
	private void log(String message, int type, int logLevel) {
		// is there a log file?
		if(this.log != null) {
			// write message with given log level into log file
			this.log.println(message, type, logLevel);
		}
	}
	
	// constructor
	public SocketThread(Socket socket, FunkrufProtocol protocol, Log log) {
		super("SocketThread");
		
		this.log = log;
		
		this.socket = socket;
		this.protocol = protocol;
	}
	
	// check if thread is running
	public boolean isRunning() {
		return this.running;
	}
	
	// halt thread
	public void halt() {
		// set running to false
		this.running = false;
		
		// cancel listening (input)
		try {
			this.socket.shutdownInput();
		} catch (IOException e) {
		}
	}
	
	// "main"
	@Override
	public void run() {
		try {
			// open writer and reader
			this.pw = new PrintWriter(this.socket.getOutputStream(), true);
			this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			
			String inputBuffer = "";
			// send name
			this.pw.println(Main.config.getName());
			
			// set running to true
			this.running = true;
			
			// if thread is running, wait for input
			while(this.running && (inputBuffer = this.reader.readLine()) != null) {
				log("SocketThread: inputBuffer# " + inputBuffer, Log.MS);
				
				// handle received input
				this.protocol.handle(inputBuffer, this.pw, Main.timeSlots);
			}
			
			// close writer, reader and socket
			this.pw.close();
			this.reader.close();
			this.socket.close();
			
			// set running to false
			this.running = false;
			
			log("SocketThread: connection lost", Log.COMMUNICATION);
		} catch(IOException e) {
			e.printStackTrace();
		}
				
		// remove thread from thread list
		Main.removeSocketThread(this);
	}
}
