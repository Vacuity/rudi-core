package ai.vacuity.rudi.sensor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.slf4j.LoggerFactory;

import ai.vacuity.rudi.adaptors.bo.Cache;
import ai.vacuity.rudi.adaptors.bo.Config;
import ai.vacuity.rudi.adaptors.bo.p2p.Input;
import ai.vacuity.rudi.adaptors.regex.GraphMaster;
import rice.environment.Environment;
import rice.p2p.commonapi.Id;
import rice.pastry.NodeHandle;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.leafset.LeafSet;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;

/**
 * Routes events from other sensors.
 * 
 * @author Jeff Hoye
 * @author In Lak'ech.
 */
public class Router {
	// Loads pastry settings
	public static final Environment env = new Environment();

	static {
		// disable the UPnP setting (in case you are testing this on a NATted LAN)
		env.getParameters().setString("nat_search_policy", "never");
	}

	private static final List<InetSocketAddress> peers = new ArrayList<InetSocketAddress>();
	public static final int PORT_DEFAULT = 10100;

	public final static org.slf4j.Logger logger = LoggerFactory.getLogger(Router.class);

	OverlaySensor[] overlayAccess = new OverlaySensor[0];

	/**
	 * This constructor sets up a PastryNode. It will bootstrap to an existing ring if it can find one at the specified location, otherwise it will start a new ring.
	 * 
	 * @param bindport
	 *            the local port to bind to
	 * @param bootaddress
	 *            the IP:port of the node to boot from
	 * @param env
	 *            the environment for these nodes
	 */
	public Router(int bindport, InetSocketAddress[] bootaddresses) throws Exception {

		overlayAccess = new OverlaySensor[bootaddresses.length];
		for (int i = 0; i < bootaddresses.length; i++) {
			InetSocketAddress boot = bootaddresses[i];

			// Generate the NodeIds Randomly
			NodeIdFactory nidFactory = new RandomNodeIdFactory(env);

			// construct the PastryNodeFactory, this is how we use rice.pastry.socket
			PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory, bindport, env);

			// construct a node
			PastryNode node = factory.newNode();

			// construct a new App
			this.overlayAccess[i] = new OverlaySensor(node);

			node.boot(boot);
			join(nidFactory, node, this.overlayAccess[i]);
		}

	}

	private void join(NodeIdFactory nidFactory, PastryNode node, OverlaySensor access) throws InterruptedException, IOException {
		// the node may require sending several messages to fully boot into the ring
		synchronized (node) {
			while (!node.isReady() && !node.joinFailed()) {
				// delay so we don't busy-wait
				node.wait(500);

				// abort if can't join
				if (node.joinFailed()) { throw new IOException("Could not join the FreePastry ring.  Reason:" + node.joinFailedReason()); }
			}
		}

		logger.debug("Finished creating new node " + node);

		// wait 10 seconds
		env.getTimeSource().sleep(10000);

		// route 10 messages
		for (int i = 0; i < 10; i++) {
			// pick a key at random
			Id randId = nidFactory.generateNodeId();

			// send to that key
			Packet msg = new Packet();
			msg.setTo(randId);
			msg.setEvent(new Input());
			msg.getEvent().setLabel("I have your overlay id.");
			access.send(msg);

			// wait a sec
			env.getTimeSource().sleep(1000);
		}

		// wait 10 seconds
		env.getTimeSource().sleep(10000);

		// send directly to my leafset
		LeafSet leafSet = node.getLeafSet();

		// this is a typical loop to cover your leafset. Note that if the leafset
		// overlaps, then duplicate nodes will be sent to twice
		for (int i = -leafSet.ccwSize(); i <= leafSet.cwSize(); i++) {
			if (i != 0) { // don't send to self
				// select the item
				NodeHandle nh = leafSet.get(i);
				Packet pkt = new Packet();
				pkt.setTo(nh);
				pkt.setEvent(new Input());
				pkt.getEvent().setLabel("I have your node host id.");

				// send the message directly to the node
				access.send(pkt);

				// wait a sec
				env.getTimeSource().sleep(1000);
			}
		}
	}

	public boolean hasPeers() {
		return getOverlayAccess().length > 0;
	}

	public void route(Input event) {
		if (!hasPeers()) return;
		boolean found = false;
		Packet pkt = new Packet();
		pkt.setEvent(new Input());
		pkt.getEvent().setLabel(event.getLabel());
		for (Cache c : GraphMaster.getPeerPatterns()) {
			Matcher matcher = c.getPattern().matcher(event.getLabel());
			if (matcher.find()) {
				if (!found) found = true;
				pkt.setTo(c.getPeer());
				getOverlayAccess()[0].send(pkt); // TODO does this ensure deliver across rings?
			}
		}
		if (!found) {
			for (OverlaySensor access : getOverlayAccess()) {
				access.broadcast(pkt);
			}
		}
	}

	/**
	 * Usage: java [-cp FreePastry-<version>.jar] rice.tutorial.lesson3.DistTutorial localbindport bootIP bootPort example java rice.tutorial.DistTutorial 9001 pokey.cs.almamater.edu 9001
	 */
	public static void main(String[] args) throws Exception {

		if (args.length == 0) Config.get("test");
		else {
			try {
				// build the bootaddress from the command line args
				InetAddress bootaddr = InetAddress.getByName(args[0]);

				// the port to use locally
				int bindport = (args.length > 1) ? Integer.parseInt(args[1]) : Router.PORT_DEFAULT;

				int bootport = (args.length > 2) ? Integer.parseInt(args[2]) : Router.PORT_DEFAULT;
				InetSocketAddress bootaddress = new InetSocketAddress(bootaddr, bootport);

				// launch our node!
				Router dt = new Router(bindport, new InetSocketAddress[] { bootaddress });

			}
			catch (Exception e) {
				// remind user how to use
				logger.error("Usage\n" + //
						"java [-cp FreePastry-<version>.jar] rice.tutorial.lesson3.DistTutorial localbindport bootIP bootPort\n" + //
						"example java rice.tutorial.DistTutorial 9001 pokey.cs.almamater.edu 9001");
				throw e;
			}
		}

	}

	public static List<InetSocketAddress> getPeers() {
		return peers;
	}

	public static void add(InetSocketAddress peer) {
		getPeers().add(peer);
	}

	public OverlaySensor[] getOverlayAccess() {
		return overlayAccess;
	}

	public void setOverlayAccess(OverlaySensor[] overlayAccess) {
		this.overlayAccess = overlayAccess;
	}

}
