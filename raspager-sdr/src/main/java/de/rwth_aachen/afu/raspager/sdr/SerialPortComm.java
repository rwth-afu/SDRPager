package de.rwth_aachen.afu.raspager.sdr;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

public class SerialPortComm {
	private static final Logger log = Logger.getLogger(SerialPortComm.class.getName());
	// pins
	public static final int DTR = 0;
	public static final int RTS = 1;

	// current settings (port, pin, invert)
	private SerialPort serialPort = null;
	private int pin = DTR;
	private boolean invert = false;

	// constructor
	public SerialPortComm(String portName, int pin, boolean invert)
			throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException {
		// set current settings
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
			serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
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
		if (serialPort == null) {
			log.severe("Serial port is null.");
			return;
		}

		// set current pin
		switch (this.pin) {
		case DTR:
			if (serialPort.isDTR() != on) {
				serialPort.setDTR(on);
				log.log(Level.FINE, "Set DTR to {0}.", on ? "on" : "off");
			}
			break;
		case RTS:
			if (serialPort.isRTS() != on) {
				serialPort.setRTS(on);
				log.log(Level.FINE, "Set RTS to {0}.", on ? "on" : "off");
			}
			break;
		}
	}

	// close port if port is open
	public void close() {
		if (serialPort != null) {
			setOff();
			serialPort.close();
			serialPort = null;
		}
	}

	// get available ports
	public static ArrayList<String> getPorts() {
		ArrayList<String> list = new ArrayList<String>();

		// get all ports
		@SuppressWarnings("unchecked")
		Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier.getPortIdentifiers();
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier portIdentifier = portEnum.nextElement();

			// if port is a serial port, add name to list
			if (portIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				list.add(portIdentifier.getName());
			}
		}

		return list;
	}

	// get name of pin
	public static String getSerialPin(int pin) {
		switch (pin) {
		case 0:
			return "DTR";
		case 1:
			return "RTS";
		default:
			return "unbekannt";
		}
	}
}
