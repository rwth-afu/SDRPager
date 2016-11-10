package de.rwth_aachen.afu.raspager;

import java.util.List;

public interface Transmitter extends AutoCloseable {

	void init(Configuration config) throws Exception;

	void send(List<Integer> data) throws Exception;
}
