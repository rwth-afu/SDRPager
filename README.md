# SDRPager
POCSAG pager software based on soundcard generation of baseband signal

## Authors:
* Ralf Wilke DH3WR, Aachen
* Michael Delissen, Aachen
* Marvin Menzerath, Aachen

This software is released free of charge under the Creative Commons License of type "by-nc-sa". No commercial use
is allowed.
The software licenses of the used libs as stated below apply in any case.


## Run
* Install RXTX
    * `sudo apt-get install librxtx-java`
    * alternatively: follow [these](http://www.jcontrol.org/download/rxtx_de.html) instructions
* Run `java -Djava.library.path=/usr/lib/jni -jar FunkrufSlave.jar`

## Example-Configuration
```
#[slave config]
# Port
port=1337

# Allowed Masters, seperated by a space
master=127.0.0.1

# Correctionfactor
correction=0.35

# Serial: Port, Pin
serial=/dev/ttyS0 DTR

# GPIO-Pin: RasPi-Type / GPIO-Pin
gpio=RaspberryPi_3B / GPIO 9

# Additional configuration of Serial and GPIO

# use gpio / serial
use=gpio

# 1 (yes) / 0 (no)
invert=1

# in ms
delay=100

# Sound Device (as it is identified by AudioSystem.getMixerInfo())
sounddevice=ALSA [default]

# LogLevel
# NORMAL = 0; DEBUG_CONNECTION = 1; DEBUG_SENDING = 2; DEBUG_TCP = 3;
loglevel=0
```

## Build
* Java JDK 1.8
* Libraries
	* [Pi4J](http://pi4j.com/)
		* `pi4j-core.jar`
		* `pi4j-device.jar`
		* `pi4j-gpio-extension.jar`
		* `pi4j-service.jar`
	* [RXTX](http://www.jcontrol.org/download/rxtx_de.html)
