package com.xikka.server.handler;

import java.util.*;

import com.xikka.server.*;

 /**
  * Pair players up (randomly).
  *
  */
public class MatchmakerHandler implements Handler {

	Client waiter = null;
	private final Handler next;
	
	public MatchmakerHandler(Handler next) {
		this.next = next;
	}
    
	@Override
	public void onSet(Client client) {
        client.send("{\"success\":1}");
        if (waiter == null) {
            waiter = client;
        } else {
            // pair them up
            waiter.send("{\"start\":1,\"opponent\":\""+client.getJSONUsername()+"\",\"role\":1}");
            client.send("{\"start\":1,\"opponent\":\""+client.getJSONUsername()+"\",\"role\":2}");
            
            waiter.setHandler(next);
            client.setHandler(next);
            
            waiter = null;
        }
	}

	@Override
	public void onReceive(Client client, String message) {
	}

	@Override
	public void onClose(Client client, boolean clientClosed) {
		if (waiter == client) waiter = null;
	}
	
}
