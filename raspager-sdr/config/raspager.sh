#!/bin/sh

JAVA=/usr/bin/java
LOG_CONF=logging.properties
OPTS=

$JAVA -jar -Djava.util.logging.config.file=$LOG_CONF SDRPager.jar $OPTS
