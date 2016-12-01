package de.rwth_aachen.afu.raspager;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.handler.ipfilter.IpFilterRule;
import io.netty.handler.ipfilter.IpFilterRuleType;

final class MasterServerFilter implements IpFilterRule {
	private static final Logger log = Logger.getLogger(MasterServerFilter.class.getName());
	private final List<String> masters = new ArrayList<>();

	public MasterServerFilter(Configuration config) {
		// TODO Does not resolve host names to IP addresses
		String masterList = config.getString(ConfigKeys.NET_MASTERS, null);
		Arrays.stream(masterList.split(" +")).forEach((m) -> masters.add(m));
	}

	@Override
	public boolean matches(InetSocketAddress remoteAddress) {
		if (masters.contains(remoteAddress.getHostString())) {
			log.log(Level.FINE, "Valid master server: {0}", remoteAddress.getHostString());
			return true;
		} else {
			log.log(Level.WARNING, "Not a master server: {0}", remoteAddress.getHostString());
			return false;
		}
	}

	@Override
	public IpFilterRuleType ruleType() {
		return IpFilterRuleType.ACCEPT;
	}

}
