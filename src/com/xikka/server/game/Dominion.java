package com.xikka.server.game;

import com.xikka.server.*;
import com.xikka.server.handler.*;


import java.util.*;

public class Dominion extends Game {
	public static final String VERSION = "0.0.0";
	HashMap<String, DominionGame> activeGames = new HashMap<String, DominionGame>();
	class DominionGame {
		String gameName;
		Client[] clients = new Client[2];
		boolean started;
		public Client getPartner(Client client) {
			if(client==clients[0])return clients[1]; return clients[0];
		}
		DominionGame(String gameName) {
			this.gameName=gameName;
		}
		void close(){
			activeGames.remove(gameName);
			for(Client c:clients){
				if(c!=null){
					clientToGame.remove(c);
					c.send("x");
				}
			}
		}
		void addClinet(Client l){
			clientToGame.put(l, this);
			if(clients[0]==null){
				clients[0]=l;
				l.send("h");
				return;
			}
			clients[1]=l;
			start();
		}
		void start(){
			System.out.println("starting");
			started=true;
			int kingdomCards=22;
			ArrayList<Integer> ints = new ArrayList<Integer>();
			for(int i=0;i<kingdomCards;i++){
				ints.add(i);
			}
			Collections.shuffle(ints);
			String message = "";
			for(int i=0;i<10;i++){
				message+=ints.remove(0)+(i==9?"":",");
			}
			
			for(Client c:clients){
				c.send("s"+message);
			}
			clients[(int)(Math.random()*clients.length)].send("e");
		}
	}
	static Map<Client, DominionGame> clientToGame = new HashMap<Client, DominionGame>();

	private Handler echo = new Handler() {
		@Override
		public void onSet(Client client) {
			//			if (waiting == null) {
			//				waiting = client;
			//			} else {
			//				client.send("start");
			//				waiting.send("start");
			//				Game g = new Game();
			//				g.p1 = client;
			//				g.p2 = waiting;
			//				pairs.put(client, g);
			//				pairs.put(waiting, g);
			////				g.current = Math.random() > 0.5 ? g.p1 : g.p2;
			////				g.current.send("e");
			//				waiting = null;
			//			}
		}

		@Override
		public void onReceive(Client client, String initialMessage) {
			//sending host request//
			for(String message:initialMessage.split(">")){
				System.out.println("message: "+message);
				if(message.startsWith("h")){
					String gameName=message.substring(1, message.length());
					DominionGame game =activeGames.get(gameName); 
					if(game==null){
						game = new DominionGame(gameName);
						activeGames.put(gameName, game);
					}
					if(game.started){
						client.send("n");
						return;
					}
					else{
						game.addClinet(client);
					}
					return;
				}
				//other message//
				DominionGame g = clientToGame.get(client);
				if (g != null) {
					g.getPartner(client).send(message);
				}
			}
		}

		@Override
		public void onClose(Client client, boolean clientClosed) {
			DominionGame g = clientToGame.get(client);
			if (g != null) {
				g.close();
			}
		}
	};

	@Override
	protected Handler getInitialHandler() {
		return echo;
	}

	@Override
	public String getVersion() {
		return Dominion.VERSION;
	}

}
