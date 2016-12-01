package de.rwth_aachen.afu.raspager;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.netty.handler.ipfilter.IpFilterRule;
import io.netty.handler.ipfilter.IpFilterRuleType;

final class MasterServerFilter implements IpFilterRule {

	private final List<String> masters = new ArrayList<>();

	public MasterServerFilter(Configuration config) {
		// TODO Does not resolve host names to IP addresses
		String masterList = config.getString(ConfigKeys.NET_MASTERS, null);
		Arrays.stream(masterList.split(" +")).forEach((m) -> masters.add(m));
	}

	@Override
	public boolean matches(InetSocketAddress remoteAddress) {
		return masters.contains(remoteAddress.getHostString());
	}

	@Override
	public IpFilterRuleType ruleType() {
		return IpFilterRuleType.ACCEPT;
	}

}
