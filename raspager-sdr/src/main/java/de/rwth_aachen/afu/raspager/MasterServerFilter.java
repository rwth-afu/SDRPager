package de.rwth_aachen.afu.raspager;

import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ipfilter.AbstractRemoteAddressFilter;

/**
 * This class implements an IP-based filter for master servers.
 * 
 * @author Philipp Thiel
 */
final class MasterServerFilter extends AbstractRemoteAddressFilter<InetSocketAddress> {
	private static final Logger log = Logger.getLogger(MasterServerFilter.class.getName());
	private final String[] masters;

	/**
	 * Creates a new filter instance.
	 * 
	 * @param masters
	 *            IP addresses of valid master servers.
	 */
	public MasterServerFilter(String... masters) {
		if (masters != null) {
			this.masters = masters;
		} else {
			throw new NullPointerException("masters");
		}
	}

	@Override
	protected boolean accept(ChannelHandlerContext ctx, InetSocketAddress remoteAddress) throws Exception {
		String addr = remoteAddress.getAddress().getHostAddress();
		for (String m : masters) {
			if (m.equalsIgnoreCase(addr)) {
				return true;
			}
		}

		return false;
	}

	@Override
	protected ChannelFuture channelRejected(ChannelHandlerContext ctx, InetSocketAddress remoteAddress) {
		log.log(Level.WARNING, "Connection rejected: {0}", remoteAddress.getHostString());
		return null;
	}
}
