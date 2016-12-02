package de.rwth_aachen.afu.raspager;

import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.handler.ipfilter.IpFilterRule;
import io.netty.handler.ipfilter.IpFilterRuleType;

/**
 * This class implements an IP-based filter for master servers.
 * 
 * @author Philipp Thiel
 */
final class MasterServerFilter implements IpFilterRule {
	private static final Logger log = Logger.getLogger(MasterServerFilter.class.getName());
	private final String[] masters;

	/**
	 * Creates a new filter instance.
	 * 
	 * @param masters
	 *            IP addresses of valid master servers.
	 */
	public MasterServerFilter(String[] masters) {
		this.masters = masters;
	}

	@Override
	public boolean matches(InetSocketAddress remoteAddress) {
		String addr = remoteAddress.getAddress().getHostAddress();
		for (String m : masters) {
			if (m.equalsIgnoreCase(addr)) {
				log.log(Level.FINE, "Valid master server: {0}", remoteAddress.getHostString());
				return false;
			}
		}

		log.log(Level.WARNING, "Not a master server: {0}", remoteAddress.getHostString());
		return true;
	}

	@Override
	public IpFilterRuleType ruleType() {
		return IpFilterRuleType.REJECT;
	}
}
