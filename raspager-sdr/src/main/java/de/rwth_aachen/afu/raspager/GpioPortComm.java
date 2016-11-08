package de.rwth_aachen.afu.raspager;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;

public class GpioPortComm {
	private GpioController gpio;
	private GpioPinDigitalOutput gpioPin = null;
	private boolean invert = false;

	private boolean curOn;
	private Log log = null;

	// write message into log file (log level normal)
	private void log(String message, int type) {
		log(message, type, Log.DEBUG_SENDING);
	}

	// write message with given log level into log file
	private void log(String message, int type, int level) {
		// is there a log file?
		if (log != null) {
			// write message with given log level into log file
			log.println(message, type, level);
		}
	}

	// constructor
	public GpioPortComm(Pin pin, boolean invert, Log log) {
		// set current settings
		this.log = log;
		this.invert = invert;

		this.gpio = GpioFactory.getInstance();
		this.gpioPin = this.gpio.provisionDigitalOutputPin(pin, "FunkrufSlave", PinState.LOW);
		this.gpioPin.setShutdownOptions(true, PinState.LOW);

		this.curOn = false;
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
		if (this.gpioPin == null) {
			log("GpioPortComm # setStatus - gpioPin ist null", Log.ERROR);
			return;
		}

		if (on && !curOn) {
			gpioPin.low();
			this.curOn = true;

			log("GpioPortComm # Set Pin " + this.gpioPin.getName() + " to on", Log.INFO);
		} else if (!on && curOn) {
			gpioPin.high();
			this.curOn = false;

			log("GpioPortComm # Set Pin " + this.gpioPin.getName() + " to off", Log.INFO);
		}
	}

	public void close() {
		if (this.gpio != null) {
			this.gpioPin.low();
			this.gpio.shutdown();
			this.gpio.unprovisionPin(this.gpioPin);
		}
	}
}
