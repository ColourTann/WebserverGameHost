package com.xikka.server.handler;

import com.xikka.server.*;

public interface Handler {
	public void onSet(Client client);
	public void onReceive(Client client, String message);
	public void onClose(Client client, boolean clientClosed);
}
