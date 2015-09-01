package com.xikka.server.handler;

import java.util.*;

import com.xikka.server.*;

 /**
  * Traditional Lobby with rooms!
  *
  */
public class LobbyHandler implements Handler {

	List<Room> rooms = new ArrayList<Room>();
	int roomSize = -1;
    
    public LobbyHandler(int roomSize) {
        this.roomSize = roomSize;
    }
    
	@Override
	public void onSet(Client client) {
		// Tell them about all the rooms!
	}

	@Override
	public void onReceive(Client client, String message) {
		// join - join a room
		// make - make a room
		// leave - leave a room
		// start - start a game
	}

	@Override
	public void onClose(Client client, boolean clientClosed) {
		// If player is in a room, leave it!
	}
	
}
