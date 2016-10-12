/*******************************************************************************

"FreePastry" Peer-to-Peer Application Development Substrate

Copyright 2002-2007, Rice University. Copyright 2006-2007, Max Planck Institute 
for Software Systems.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

- Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

- Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

- Neither the name of Rice  University (RICE), Max Planck Institute for Software 
Systems (MPI-SWS) nor the names of its contributors may be used to endorse or 
promote products derived from this software without specific prior written 
permission.

This software is provided by RICE, MPI-SWS and the contributors on an "as is" 
basis, without any representations or warranties of any kind, express or implied 
including, but not limited to, representations or warranties of 
non-infringement, merchantability or fitness for a particular purpose. In no 
event shall RICE, MPI-SWS or contributors be liable for any direct, indirect, 
incidental, special, exemplary, or consequential damages (including, but not 
limited to, procurement of substitute goods or services; loss of use, data, or 
profits; or business interruption) however caused and on any theory of 
liability, whether in contract, strict liability, or tort (including negligence
or otherwise) arising in any way out of the use of this software, even if 
advised of the possibility of such damage.

*******************************************************************************/
package ai.vacuity.rudi.sensor;

import java.io.IOException;
import java.net.InetSocketAddress;
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
 * This tutorial shows how to setup a FreePastry node using the Socket Protocol.
 * 
 * @author Jeff Hoye
 */
public class Router {
	private final static org.slf4j.Logger logger = LoggerFactory.getLogger(Router.class);

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

	private static InetSocketAddress[] peers = new InetSocketAddress[0];

	// // Loads pastry settings
	private static Environment env = new Environment();

	static {
		// disable the UPnP setting (in case you are testing this on a NATted LAN)
		env.getParameters().setString("nat_search_policy", "never");
	}

	private static OverlaySensor access = null;

	public Router(int bindport, InetSocketAddress bootaddress) throws Exception {

		// Generate the NodeIds Randomly
		NodeIdFactory nidFactory = new RandomNodeIdFactory(env);

		// construct the PastryNodeFactory, this is how we use rice.pastry.socket
		PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory, bindport, env);

		// construct a node
		PastryNode node = factory.newNode();

		// construct a new App
		OverlaySensor access = new OverlaySensor(node);

		node.boot(bootaddress);

		join(nidFactory, node, access);
	}

	private void join(NodeIdFactory nidFactory, PastryNode node, OverlaySensor app) throws InterruptedException, IOException {
		// the node may require sending several messages to fully boot into the ring
		synchronized (node) {
			while (!node.isReady() && !node.joinFailed()) {
				// delay so we don't busy-wait
				node.wait(500);

				// abort if can't join
				if (node.joinFailed()) { throw new IOException("Could not join the FreePastry ring.  Reason:" + node.joinFailedReason()); }
			}
		}

		System.out.println("Finished creating new node " + node);

		// wait 10 seconds
		env.getTimeSource().sleep(10000);

		// route 10 messages
		for (int i = 0; i < 10; i++) {
			// pick a key at random
			Id randId = nidFactory.generateNodeId();

			// send to that key
			Packet pkt = new Packet(null, null);
			pkt.setTo(randId);
			pkt.setEvent(new Input());
			pkt.getEvent().setLabel("Hi.");
			app.send(pkt);

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

				// send the message directly to the node
				app.send(nh);

				// wait a sec
				env.getTimeSource().sleep(1000);
			}
		}
	}

	public static void init(int daemonPort) {
		if (Router.getPeers().length > 0) {
			try {
				for (InetSocketAddress p : Router.getPeers()) {
					new Router(daemonPort, p);
				}
				// new Router(localPort, peers);
			}
			catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	public boolean hasPeers() {
		return Router.peers.length > 0;
	}

	public void route(Input event) {
		if (!hasPeers()) return;
		boolean found = false;
		Packet pkt = new Packet(null, null);
		pkt.setEvent(new Input());
		pkt.getEvent().setLabel(event.getLabel());
		for (Cache c : GraphMaster.getPeerPatterns()) {
			Matcher matcher = c.getPattern().matcher(event.getLabel());
			if (matcher.find()) {
				if (!found) found = true;
				pkt.setTo(c.getPeer());
				getAccess().send(pkt); // TODO does this ensure deliver across rings?
			}
		}
		if (!found) {
			// getAccess().broadcast(pkt);
		}
	}

	/**
	 * Usage: java [-cp FreePastry-<version>.jar] rice.tutorial.lesson3.DistTutorial localbindport bootIP bootPort example java rice.tutorial.DistTutorial 9001 pokey.cs.almamater.edu 9001
	 */
	public static void main(String[] args) throws Exception {
		Config.get("");

		// // Loads pastry settings
		// Environment env = new Environment();
		//
		// // disable the UPnP setting (in case you are testing this on a NATted LAN)
		// env.getParameters().setString("nat_search_policy", "never");
		//
		// try {
		// // the port to use locally
		// int bindport = Integer.parseInt(args[0]);
		//
		// // build the bootaddress from the command line args
		// InetAddress bootaddr = InetAddress.getByName(args[1]);
		// int bootport = Integer.parseInt(args[2]);
		// InetSocketAddress bootaddress = new InetSocketAddress(bootaddr, bootport);
		//
		// // launch our node!
		// DistTutorial dt = new DistTutorial(bindport, bootaddress, env);
		// }
		// catch (Exception e) {
		// // remind user how to use
		// System.out.println("Usage:");
		// System.out.println("java [-cp FreePastry-<version>.jar] rice.tutorial.lesson3.DistTutorial localbindport bootIP bootPort");
		// System.out.println("example java rice.tutorial.DistTutorial 9001 pokey.cs.almamater.edu 9001");
		// throw e;
		// }
	}

	public static void setPeers(InetSocketAddress[] peers) {
		Router.peers = peers;
	}

	public static InetSocketAddress[] getPeers() {
		return Router.peers;
	}

	public static OverlaySensor getAccess() {
		return access;
	}

	public static void setAccess(OverlaySensor access) {
		Router.access = access;
	}
}
