package de.rwth_aachen.afu.raspager.sdr;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

/**
 * Serial port controller used by the SDR transmitter.
 * 
 * @author Philipp Thiel
 */
public final class SerialPortComm {
	public static final int DTR = 0;
	public static final int RTS = 1;

	private SerialPort serialPort = null;
	private int pin = DTR;
	private boolean invert = false;

	/**
	 * Gets the pin number for a given name.
	 * 
	 * @param pin
	 *            Pin name
	 * @return Pin number the corresponds to the given name.
	 * @throws IllegalArgumentException
	 *             If the given name does not match a pin.
	 */
	public static int getPinNumber(String pin) {
		if ("DTR".equalsIgnoreCase(pin)) {
			return DTR;
		} else if ("RTS".equalsIgnoreCase(pin)) {
			return RTS;
		} else {
			throw new IllegalArgumentException("Invalid pin.");
		}
	}

	/**
	 * Converts the pin number to a name.
	 * 
	 * @param pin
	 *            Pin number to convert.
	 * @return Pin name for the given number.
	 * @throws IllegalArgumentException
	 *             If the pin number is invalid.
	 */
	public static String getPinName(int pin) {
		switch (pin) {
		case DTR:
			return "DTR";
		case RTS:
			return "RTS";
		default:
			throw new IllegalArgumentException("Invalid pin number.");
		}
	}

	/**
	 * Construct a new serial port controller.
	 * 
	 * @param portName
	 *            Serial port to use.
	 * @param pin
	 *            Pin number to use (DTR or RTS).
	 * @param invert
	 *            Invert
	 * @throws NoSuchPortException
	 *             If the given port does not exist.
	 * @throws PortInUseException
	 *             If the given port is already in use.
	 * @throws UnsupportedCommOperationException
	 *             If rxtx is not happy.
	 */
	public SerialPortComm(String portName, int pin, boolean invert)
			throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException {
		this.pin = pin;
		this.invert = invert;

		CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
		if (portIdentifier.isCurrentlyOwned()) {
			throw new PortInUseException();
		} else {
			serialPort = (SerialPort) portIdentifier.open("FunkrufSlave", 2000);
			serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
		}

		setOff();
	}

	/**
	 * Enable the pin.
	 * 
	 * @throws IllegalStateException
	 *             If the serial port is not initialized.
	 */
	public void setOn() {
		setStatus(!this.invert);
	}

	/**
	 * Disables the pin.
	 * 
	 * @throws IllegalStateException
	 *             If the serial port is not initialized.
	 */
	public void setOff() {
		setStatus(this.invert);
	}

	/**
	 * Sets the pin status.
	 * 
	 * @param on
	 *            Enable or disable the pin.
	 * @throws IllegalStateException
	 *             If the serial port is not initialized.
	 */
	private void setStatus(boolean on) {
		if (serialPort == null) {
			throw new IllegalStateException("Serial port is not initialized.");
		}

		switch (this.pin) {
		case DTR:
			if (serialPort.isDTR() != on) {
				serialPort.setDTR(on);
			}
			break;
		case RTS:
			if (serialPort.isRTS() != on) {
				serialPort.setRTS(on);
			}
			break;
		}
	}

	/**
	 * Closes the serial port.
	 */
	public void close() {
		if (serialPort != null) {
			setOff();
			serialPort.close();
			serialPort = null;
		}
	}

	/**
	 * Gets a list of available serial ports.
	 * 
	 * @return List of serial ports.
	 */
	public static List<String> getPorts() {
		List<String> list = new ArrayList<String>();

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
}
