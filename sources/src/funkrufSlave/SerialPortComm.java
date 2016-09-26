package funkrufSlave;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.util.ArrayList;
import java.util.Enumeration;

public class SerialPortComm {
	// pins
	public static final int DTR = 0;
	public static final int RTS = 1;
	
	// current settings (port, pin, invert)
	private SerialPort serialPort = null;
	private int pin = DTR;
	private boolean invert = false;
	
	private Log log = null;
	

	// write message into log file (log level normal)
	private void log(String message, int type) {
		log(message, type, Log.DEBUG_SENDING);
	}

	// write message with given log level into log file
	private void log(String message, int type, int level) {
		// is there a log file?
		if(log != null) {
			// write message with given log level into log file
			log.println(message, type, level);
		}
	}
	
	// constructor
	public SerialPortComm(String portName, int pin, boolean invert, Log log) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException {
		// set current settings
		this.log = log;
		this.pin = pin;
		this.invert = invert;
		
		// check port
		CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
		// is port currently in use?
		if (portIdentifier.isCurrentlyOwned()) {
			throw new PortInUseException();
		} else {
			// open port
			serialPort = (SerialPort) portIdentifier.open("FunkrufSlave", 2000);
			serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		}
		
		// set pin off
		setOff();
	}
	
	// set pin on
	public void setOn() {
		// if invert, then status is set to false
		// if not invert, then status is set to true
		setStatus(!this.invert);
	}

	// set pin off
	public void setOff() {
		// if invert, then status is 
		setStatus(this.invert);
	}
	
	// set pin status
	private void setStatus(boolean on) {
		if(serialPort == null) {
			log("SerialPortComm # setStatus - serialPort ist null", Log.ERROR);
			return;
		}
		
		// set current pin
		switch(this.pin) {
			case DTR:
				if(serialPort.isDTR() != on) {
					serialPort.setDTR(on);
					log("SerialPortComm # Set DTR to " + (on ? "on" : "off"), Log.INFO);
				}
				break;
			case RTS:
				if(serialPort.isRTS() != on) {
					serialPort.setRTS(on);
					log("SerialPortComm # Set RTS to " + (on ? "on" : "off"), Log.INFO);
				}
				break;
		}
	}
	
	// close port if port is open
	public void close() {
		if(serialPort != null) {
			setOff();
			serialPort.close();
			serialPort = null;
		}
	}
	
	// get available ports
	public static ArrayList<String> getPorts()
    {
		ArrayList<String> list = new ArrayList<String>();
		
		// get all ports
        @SuppressWarnings("unchecked")
		Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier.getPortIdentifiers();
        while(portEnum.hasMoreElements()) 
        {
            CommPortIdentifier portIdentifier = portEnum.nextElement();
            
            // if port is a serial port, add name to list
            if(portIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL) {
            	list.add(portIdentifier.getName());
            }
        }    
        
        return list;
    }
	
	// get name of pin
	public static String getSerialPin(int pin) {
		switch(pin) {
			case 0: return "DTR";
			case 1: return "RTS";
			default: return "unbekannt";
		}
	}
}
