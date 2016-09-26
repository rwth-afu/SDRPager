package funkrufSlave;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
	// log levels
	public static final int NORMAL = 0;
	public static final int DEBUG_CONNECTION = 1;
	public static final int DEBUG_SENDING = 2;
	public static final int DEBUG_TCP = 3;
	
	public static final int INFO = 0;
	public static final int ERROR = 1;
	public static final int COMMUNICATION = 2;
	public static final int MS = 3;
	public static final int SM = 4;

	private final String[] types = {"I", "E", "C", "MS", "SM"};
	
	private String logFile = null;
	private int logLevel = 0;
	private PrintWriter writer = null;
	
	private boolean cmdOutput = false;
	
	
	// constructor
	public Log(String logFile) {
		this.logFile = logFile;
		this.logLevel = NORMAL;

		if(logFile != null && !logFile.equals("")) {
		
			try {
				
				this.writer = new PrintWriter(new FileWriter(this.logFile, true), true);
				println("Logging gestartet...", INFO);
				
			} catch(IOException e) {
				println("LogFile konnte nicht erstellt werden!", ERROR);
			}
		}
	}
	
	// constructor
	public Log(String logFile, boolean cmdOutput) {
		this(logFile);
		
		this.cmdOutput = cmdOutput;
	}
	
	// constructor
	public Log(String logFile, int logLevel) {
		this(logFile);
		
		this.logLevel = (logLevel < 0 ? 0 : logLevel);
	}
	
	// constructor
	public Log(String logFile, boolean cmdOutput, int logLevel) {
		this(logFile, logLevel);
		
		this.cmdOutput = cmdOutput;
	}
	
	// print message (log level normal)
	public void println(String message, int type) {
		this.println(message, type, NORMAL);
	}
	
	// print message with given log level
	public void println(String message, int type, int logLevel) {
		if(logLevel <= this.logLevel) {
			
			String time = (new SimpleDateFormat("dd.MM.yyyy HH:mm:ss")).format(new Date());
			
			
			if(this.cmdOutput) {
				System.out.printf("%2s: %s %s", types[type], time, message);
				System.out.println();
			}
			
			
			
			if(this.writer != null) {		
				this.writer.printf("%2s: %s %s", types[type], time, message);
				this.writer.println();
			}
		}
		
	}
	
	// set log level
	public void setLogLevel(int level) {
		this.logLevel = (!correctLogLevel(level) ? 0 : level);
	}
	
	// check log level
	public static boolean correctLogLevel(int level) {
		return level >= 0 && level <= 3;
	}
	
	// close log file
	public void close() {
		if(this.writer != null)  {
			this.writer.close();
			this.writer = null;
		}
	}
}
