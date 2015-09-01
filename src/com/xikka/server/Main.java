package com.xikka.server;

import com.xikka.server.handler.*;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.bind.DatatypeConverter;

import java.security.*; 

import com.xikka.server.game.Game;

public class Main {
	final static int SERVER_PORT = 7981;
	private final List<Client> allClients = new ArrayList<Client>();
	
    private MessageDigest messageDigest = null;
    
	private final String POLICY_FILE_REQUEST = "<policy-file-request/>";
	private final String POLICY_FILE = "<?xml version=\"1.0\"?><!DOCTYPE cross-domain-policy SYSTEM \"http://www.adobe.com/xml/dtds/cross-domain-policy.dtd\"><cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"*\" /> </cross-domain-policy>\0";
	
	private final Handler INITIAL = new Handler() {
		@Override
		public void onSet(Client client) {
		}
		
		@Override
		public void onReceive(Client client, String message) {
			if (client.handshake != null || message.indexOf("GET") == 0) {
                // Handshake request
                if (message.isEmpty()) {
                    // This is the end of the handshake!
                    // Prepare response!
                    if (sendHandshakeResponse(client)) {
                        // So, we know this is a web socket!
                        client.setWebSocket();
                        // Find the game and join it.
                        String get = client.handshake.get("GET");
                        if (!get.isEmpty()) {
                            int space = get.indexOf(" ");
                            int start = 0;
                            if (get.charAt(0) == '/' && get.length() > 1) {
                                start++;
                            }
                            if (space < 0) {
                                space = get.length();
                            }

                            String game = get.substring(start, space);

                            Game g = Game.find(game);
                            if (g == null) {
                                // Game not found!
                                System.err.println("Game not found (fatal): "+message);
                                client.send("Game not found...");
                                client.send("{\"success\":-1,\"error\":\"Game "+message+" not found.\"}");
                                client.stop();
                            } else {
                                // This is a WebSocket request to /game
                                g.add(client);
                                client.send("{\"success\":1,\"method\":\"websocket\"}");
                            }
                        } else {
                            // Invalid GET
                            System.err.println("Invalid GET no URL (fatal)");
                            client.stop();
                        }
                    }
                } else {
                    if (client.handshake == null) {
                        client.handshake = new HashMap<String, String>();
                    }
                    
                    int space = message.indexOf(" ");
                    
                    int field = space+1;
                    if (field > message.length()) field = space;
                    String value = message.substring(field);
                    
                    int keyLength = space;
                    
                    if (space > 0 && message.charAt(space-1) == ':') keyLength--;
                    if (keyLength < 0) keyLength = 0;
                    
                    String key = message.substring(0, keyLength);
                    client.handshake.put(key, value);
                }
            } else if (message.indexOf(POLICY_FILE_REQUEST) >= 0) {
				client.send(POLICY_FILE);
			} else if (message.charAt(0) == '.') {
                // Game select for non-WebSockets
                String game = message.substring(1);
                Game g = Game.find(game);
                if (g == null) {
                    // Game not found!
                    System.err.println("Game not found (fatal): "+message);
                    client.send("{\"success\":-1,\"error\":\"Game "+message+" not found.\"}");
                    client.stop();
                } else {
                    // Join game!
                    g.add(client);
                    client.send("{\"success\":1,\"method\":\"legacy\"}");
                }
            } else {
                // Bad message
                System.err.println("Unexpected message (warn): "+message);
            }
			// TODO : If they specify which game they wish to join, then join that server.
			// TODO : Serve up policy file if that's being asked for
		}
		
		@Override
		public void onClose(Client client, boolean clientClosed) {
			allClients.remove(client);
		}
        
        private boolean sendHandshakeResponse(Client client) {
            String key = client.handshake.get("Sec-WebSocket-Key");
            if (key != null) {
                try {
                    String magic = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
                    byte[] keyBytes = magic.getBytes("ASCII");
                    byte[] tokenBytes = messageDigest.digest(keyBytes);
                    String token = DatatypeConverter.printBase64Binary(tokenBytes);
                    String response = 
                        "HTTP/1.1 101 Switching Protocols\r\n" +
                        "Upgrade: websocket\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Sec-WebSocket-Accept: "+token+"\r\n\r\n";
                    client.send(response);
                    return true;
                } catch (Exception e) {
                    // Failure to generate hash
                    System.err.println("Failure to generate token (fatal).");
                    client.stop();
                }
            } else {
                // Ws key not found!
                System.err.println("Ws key not found (fatal).");
                client.stop();
            }
            return false;
        }
	};
	
	public static void main(String[] args) {
		new Main();
	}
	@SuppressWarnings("resource")
	Main() {
		Game.initialise();
        // Initialise
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            System.exit(1);
        }
		// Start listening.
        ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(SERVER_PORT);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println("Server running on port "+SERVER_PORT+".");
		while (true) {
			try {
				Socket clientSocket = serverSocket.accept();
			    Client client = new Client(clientSocket, INITIAL);
			    allClients.add(client);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
