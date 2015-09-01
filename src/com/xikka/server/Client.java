package com.xikka.server;


import java.io.*;
import java.net.*;
import java.util.*;

import com.xikka.server.game.Game;
import com.xikka.server.handler.*;

public final class Client {
	private static int ID;
	
    public String username = "";
	int id = 0;
	final Socket socket;
    final OutputStream out;
    final InputStream in;
	private final ClientThread thread;
	private Handler handler;
    
	private Game game;
    private boolean framing = false;
    private int type;
    // 0 = standard TCP
    // 1 = WebSocket
    
    Map<String, String> handshake = null;
	
	Client (Socket socket, Handler handler) throws IOException {
		id = Client.ID++;
		
		setHandler(handler);
		
        this.socket = socket;
        this.out = socket.getOutputStream();
        this.in = socket.getInputStream();
		
		// Create thread for connections regarding this client
		thread = new ClientThread();
		
		new Thread(thread).start();
	}
	public String getJSONUsername() {
		// Escape as necessary.
		return username;
	}
    public void setWebSocket() {
        framing = true;
    }
	public void stop() {
		thread.running = false;
	}
    public void setGame(Game g) {
        game = g;
    }
    public Game getGame() {
        return game;
    }
	
	private void close() {
		try {
			in.close();
            out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    public void send(String message) {
        if (framing) {
            frame(message);
        } else {
            try {
                out.write(message.getBytes());
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void frame(String rawmessage) {
        byte[] rawData = rawmessage.getBytes();

        int frameCount  = 0;
        byte[] frame = new byte[10];

        frame[0] = (byte) 129;

        if(rawData.length <= 125) {
            frame[1] = (byte) rawData.length;
            frameCount = 2;
        } else if(rawData.length >= 126 && rawData.length <= 65535) {
            frame[1] = (byte) 126;
            int len = rawData.length;
            frame[2] = (byte)((len >> 8 ) & (byte)255);
            frame[3] = (byte)(len & (byte)255); 
            frameCount = 4;
        } else {
            frame[1] = (byte) 127;
            int len = rawData.length;
            frame[2] = (byte)((len >> 56 ) & (byte)255);
            frame[3] = (byte)((len >> 48 ) & (byte)255);
            frame[4] = (byte)((len >> 40 ) & (byte)255);
            frame[5] = (byte)((len >> 32 ) & (byte)255);
            frame[6] = (byte)((len >> 24 ) & (byte)255);
            frame[7] = (byte)((len >> 16 ) & (byte)255);
            frame[8] = (byte)((len >> 8 ) & (byte)255);
            frame[9] = (byte)(len & (byte)255);
            frameCount = 10;
        }

        int bLength = frameCount + rawData.length;

        byte[] reply = new byte[bLength];

        int bLim = 0;
        for(int i=0; i<frameCount;i++){
            reply[bLim] = frame[i];
            bLim++;
        }
        for(int i=0; i<rawData.length;i++){
            reply[bLim] = rawData[i];
            bLim++;
        }
        
        try {
            out.write(reply);
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to send message.");
        }
    }
    
    public String deframe(String rawmessage) {
        return rawmessage;
    }
    
	public void setHandler(Handler handler) {
		this.handler = handler;
        handler.onSet(this);
	}
	
	private Handler getHandler() {
		return handler;
	}
	
	class ClientThread implements Runnable {
		boolean running = true;
		int buffLenth = 2048;
		public void run() {
            int len = 0;
            // TODO :: Allow messages to span packets? Or to exceed buffer length.
            // Also, use same buffer
            byte[] b = new byte[buffLenth];
			while (running) {
				try {
                    len = in.read(b);
                    if (len >= 0) {
                        if (framing) {
				 byte rLength = 0;
                           	 int rMaskIndex = 2;
                           	 int rDataStart = 0;
                           	 //b[0] is always text in my case so no need to check;
                           	 byte data = b[1];
                           	 byte op = (byte) 127;
                           	 rLength = (byte) (data & op);

                           	 if(rLength==(byte)126) rMaskIndex=4;
                           	 if(rLength==(byte)127) rMaskIndex=10;

                           	 byte[] masks = new byte[4];

                           	 int j=0;
                           	 int i=0;
                           	 for(i=rMaskIndex;i<(rMaskIndex+4);i++){
                           	     masks[j] = b[i];
                           	     j++;
                           	 }

                           	 rDataStart = rMaskIndex + 4;

                           	 int messLen = len - rDataStart;

                           	 byte[] message = new byte[messLen];

                           	 for(i=rDataStart, j=0; i<len; i++, j++){
                           	     message[j] = (byte) (b[i] ^ masks[j % 4]);
                           	 }
                           	
				 if (message.length == 2 && message[0] == 3 && message[1] == -23) {
				    System.out.println("Closing connection by packet request");
				    getHandler().onClose(Client.this, true);
				    running = false;
				 } else {
				    String line = new String(message);
				  
				    	getHandler().onReceive(Client.this, line);
				   
				 }
                           	 b = new byte[buffLenth];
                        } else {
                            // Read until newline
                            int start = 0;
                            for (int i=0; i < len; i++) {
                                if (b[i] == '\r' || b[i] == '\n') {
                                    // Stop!
                                    String line = new String(b, start, i - start);
                                    
										getHandler().onReceive(Client.this, line);
									
                                    // Gobble one more, if it's there
                                    if (b[i] == '\r' && i + 1 < len && b[i + 1] == '\n')
                                        i++;
                                    start = i + 1;
                                }
                            }
                        }
                    } else {
                        // Client has closed the connection
                        System.out.println("Closing connection");
						getHandler().onClose(Client.this, true);
						running = false;
                    }
				} catch (IOException e) {
					// Usually booted our end, I suspect
					System.out.println("Closing connection");
					getHandler().onClose(Client.this, false);
					e.printStackTrace();
					running = false;
				}
			}
			close();
		}
	}
}
