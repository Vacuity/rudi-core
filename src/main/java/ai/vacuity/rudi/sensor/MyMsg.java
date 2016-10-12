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
/*
 * Created on Feb 15, 2005
 */
package ai.vacuity.rudi.sensor;

import java.io.Serializable;

import ai.vacuity.rudi.adaptors.bo.p2p.Input;
import ai.vacuity.rudi.adaptors.bo.p2p.Response;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;

/**
 * An example message.
 * 
 * @author Jeff Hoye
 */
public class MyMsg implements Message {
	private static final long serialVersionUID = 265675863194010712L;
	private static final double VERSION_1_0 = 1.0;

	private Input event;
	private Response response;
	private Integer priority = Message.LOW_PRIORITY;

	private double version = MyMsg.VERSION_1_0;
	private Serializable more;
	/**
	 * Where the Message came from.
	 */
	Id from;
	/**
	 * Where the Message is going.
	 */
	Id to;

	/**
	 * Constructor.
	 */
	public MyMsg(Id from, Id to) {
		this.from = from;
		this.to = to;
	}

	public String toString() {
		return "MyMsg from " + from + " to " + to;
	}

	/**
	 * Use low priority to prevent interference with overlay maintenance traffic.
	 */
	public int getPriority() {
		return Message.LOW_PRIORITY;
	}

	public Input getEvent() {
		return event;
	}

	public void setEvent(Input event) {
		this.event = event;
	}

	public Response getResponse() {
		return response;
	}

	public void setResponse(Response response) {
		this.response = response;
	}

	public double getVersion() {
		return version;
	}

	public void setVersion(double version) {
		this.version = version;
	}

	public Serializable getMore() {
		return more;
	}

	public void setMore(Serializable more) {
		this.more = more;
	}

	public Id getFrom() {
		return from;
	}

	public void setFrom(Id from) {
		this.from = from;
	}

	public Id getTo() {
		return to;
	}

	public void setTo(Id to) {
		this.to = to;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}
}
