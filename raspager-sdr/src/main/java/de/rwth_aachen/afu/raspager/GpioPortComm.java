package de.rwth_aachen.afu.raspager;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

public class GpioPortComm {
	private static final Logger log = Logger.getLogger(GpioPortComm.class.getName());
	private GpioController gpio;
	private GpioPinDigitalOutput gpioPin = null;
	private boolean invert = false;

	private boolean curOn;

	public GpioPortComm(String pinName, boolean invert) {
		this.invert = invert;
		gpio = GpioFactory.getInstance();

		Pin pin = RaspiPin.getPinByName(pinName);
		gpioPin = gpio.provisionDigitalOutputPin(pin, "FunkrufSlave", PinState.LOW);
		gpioPin.setShutdownOptions(true, PinState.LOW);
		curOn = true;
	}

	// set pin on
	public void setOn() {
		// if invert, then status is set to false
		// if not invert, then status is set to true
		setStatus(!invert);
	}

	// set pin off
	public void setOff() {
		// if invert, then status is
		setStatus(invert);
	}

	// set pin status
	private void setStatus(boolean on) {
		if (this.gpioPin == null) {
			log.severe("gpioPin is null");
			return;
		}

		if (on && !curOn) {
			gpioPin.low();
			curOn = true;

			log.log(Level.FINE, "Set pin {0} to on.", gpioPin.getName());
		} else if (!on && curOn) {
			gpioPin.high();
			curOn = false;

			log.log(Level.FINE, "Set pin {0} to on.", gpioPin.getName());
		}
	}

	public void close() {
		if (gpio != null) {
			gpioPin.low();
			gpio.shutdown();
			gpio.unprovisionPin(gpioPin);
		}
	}
}
