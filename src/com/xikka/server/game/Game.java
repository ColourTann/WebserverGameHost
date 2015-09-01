package com.xikka.server.game;

import java.util.*;

import com.xikka.server.Client;
import com.xikka.server.handler.*;

public abstract class Game {
	private static Map<String, Game> games = new HashMap<String, Game>();
	
	public static void initialise() {
	    initialise(Dominion.class);
	}
	
	protected abstract Handler getInitialHandler();
	public abstract String getVersion();
	public void add(Client client) {
        client.setGame(this);
		client.setHandler(getInitialHandler());
	}
	
	public static Game find(String name) {
        String lcname = name.toLowerCase();
		return games.get(name);
	}
	private static void initialise(Class<? extends Game> game) {
		try {
			Game g = game.getConstructor().newInstance();
			games.put(game.getSimpleName().toLowerCase(), g);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
