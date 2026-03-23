package es.ubu.lsi.server;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.ChatMessage.MessageType;

/**
 * Chat server. Based on code available at: 
 * http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/
 * 
 * Modified by Raúl Marticorena &amp; Joaquín P- Seco
 * 
 * @author http://www.dreamincode.net
 * @author Raúl Marticorena
 * @author Joaquin P. Seco
 *
 */
public class ChatServerImpl implements ChatServer {

	/** Default port. */
	private static final int DEFAULT_PORT = 1500;
	
	/** Author name for traceability logs. */
	private static final String AUTHOR_NAME = "Marcos Zamorano Lasso";
	
	/** Unique ID for each connection.*/	
	private static int clientId;
	
	/** Client list. */
	private List<ServerThreadForClient> clients;
	
	/** Util class to display time. */
	private static SimpleDateFormat sdf;
	
	/** Port number to listen for connection. */
	private int port;
	
	/** Flag will be turned of to stop the server. */
	private volatile boolean alive;
	
	/** Server socket. */
	private ServerSocket serverSocket; 
	
	static {
		 sdf = new SimpleDateFormat("HH:mm:ss"); // to display hh:mm:ss
	}

	/**
	 * Server constructor that receive the port to listen to for connection.
	 * 
	 * @param port port to listen
	 */
	public ChatServerImpl(int port) {
		this.port = port;
		// List for the Client list
		clients = Collections.synchronizedList(new ArrayList<ServerThreadForClient>());
	}

	/**
	 * Starts the server.
	 */
	@Override
	public void startup() {
		alive = true;
		/* create socket server and wait for connection requests */
		try {
			// the socket used by the server
			serverSocket = new ServerSocket(port);

			// infinite loop to wait for connections
			while (alive) {
				// format message saying we are waiting
				show("Server waiting for Clients on port " + port + ".");

				Socket socket = serverSocket.accept(); // accept connection
				// if I was asked to stop
				if (!alive) {
					break;
				}
				ServerThreadForClient t = new ServerThreadForClient(socket); 
				// make a thread of it
				if (t.isReady()) {
					clients.add(t); // save it in the ArrayList
					t.start();
				}
			}
		}
		// something went bad
		catch (SocketException e) {
			if (alive) {
				String msg = sdf.format(new Date())
						+ " ServerSocket: " + e + "\n";
				show(msg);
			}
		}
		catch (IOException e) {
			String msg = sdf.format(new Date())
					+ " ServerSocket: " + e + "\n";
			show(msg);
		}
		finally {
			shutdown();
		}
	}

	/**
	 * Closes server.
	 *
	 */
	@Override
	public synchronized void shutdown() {
		alive = false;
		closeServerSocket();
		List<ServerThreadForClient> activeClients;
		synchronized (clients) {
			activeClients = new ArrayList<ServerThreadForClient>(clients);
			clients.clear();
		}
		for (ServerThreadForClient tc : activeClients) {
			tc.close();
		}
	}
	

	/**
	 * Shows an event (not a message) to the console.
	 * 
	 * @param event event
	 */
	private void show(String event) {
		String time = sdf.format(new Date()) + " " + event;
		System.out.println(time);
	}

	/**
	 * Broadcasts a message to all clients.
	 * 
	 * @param message message
	 */
	@Override
	public synchronized void broadcast(ChatMessage message) {
		// add HH:mm:ss to the message
		String time = sdf.format(new Date());
		String messageLf = time + " " + message.getMessage();
		message.setMessage(messageLf);
		
		// display message on console
		System.out.println(AUTHOR_NAME + " patrocina el mensaje: " + messageLf);
		
		// we loop in reverse order in case we would have to remove a Client
		// because it has disconnected
		synchronized (clients) {
			for (int i = clients.size(); --i >= 0;) {
				ServerThreadForClient ct = clients.get(i);
				// try to write to the Client if it fails remove it from the list
				if (!ct.sendMessage(message)) {
					clients.remove(i);
					show("Disconnected Client " + ct.username
							+ " removed from list.");
				}
			}
		}
	}

	/**
	 * Removes a logout client.
	 * 
	 * @param id client id
	 */
	@Override
	public synchronized void remove(int id) {
		// scan the array list until we found the Id
		synchronized (clients) {
			for (int i = 0; i < clients.size(); ++i) {
				ServerThreadForClient ct = clients.get(i);
				// found it
				if (ct.id == id) {
					clients.remove(i);
					return;
				}
			}
		}
	}

	/** 
	 * Runs the server with a default port if is not specified as argument.
	 * 
	 * @param args arguments
	 *
	 */
	public static void main(String[] args) {
		// Adaptado al enunciado actual: el servidor no recibe argumentos.
		if (args.length != 0) {
			System.out.println("Usage is: > java es.ubu.lsi.server.ChatServerImpl");
			return;
		}
		// create a server object and start it
		ChatServer server = new ChatServerImpl(DEFAULT_PORT);
		server.startup();
	}

	/**
	 * Closes the server socket.
	 */
	private void closeServerSocket() {
		try {
			if (serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}
		} catch (IOException e) {
			show("Exception closing the server socket: " + e);
		}
	}

	/**
	 * Builds the outgoing chat message.
	 * 
	 * @param chatMessage message received from a client
	 * @param username user name of the sender
	 * @return message ready to be broadcast
	 */
	private ChatMessage buildOutgoingMessage(ChatMessage chatMessage, String username) {
		String text = chatMessage.getMessage();
		if (text != null && text.startsWith(username + " ha baneado a ")) {
			return new ChatMessage(chatMessage.getId(), MessageType.MESSAGE, text);
		}
		return new ChatMessage(chatMessage.getId(), MessageType.MESSAGE,
				username + ": " + text);
	}

	/** 
	 * One instance of this thread will run for each client. 
	 */
	private class ServerThreadForClient extends Thread {
		
		/** Socket where to listen/talk. */
		private Socket socket;
		
		/** Stream input. */
		private ObjectInputStream sInput;
		
		/** Stream output. */
		private ObjectOutputStream sOutput;

		/**
		 * Unique id (easier for disconnection).
		 */
		private int id;
		
		/** Username. */
		private String username;
		
		/** Ready state. */
		private boolean ready;

		/**
		 * Constructor. 
		 * 
		 * @param socket socket
		 */
		private ServerThreadForClient(Socket socket) {
			// a unique id
			id = ++clientId;
			this.socket = socket;
			/* Creating both Data Stream */
			System.out
					.println("Server thread trying to create I/O streams for client");
			initConnection();
		}

		/**
		 * Initializes the client connection.
		 */
		private void initConnection() {
			try {
				// create output first
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sOutput.flush();
				sInput = new ObjectInputStream(socket.getInputStream());
				// read the username
				username = (String) sInput.readObject();
				sOutput.writeInt(id);
				sOutput.flush();
				ready = true;
				show(username + " just connected.");	
				show("connected with id:" +id);	
				ChatMessage chatMessage = new ChatMessage(id, MessageType.MESSAGE,
						username + " se ha conectado al chat.");
				broadcast(chatMessage);
			} catch (IOException e) {
				show("Exception creating new I/O Streams: " + e);
				close();
			} catch (ClassNotFoundException e) {
				close(); // organized panic case...
				throw new RuntimeException("Wrong message type", e);
			}
		}

		/**
		 * Indicates whether the client thread is correctly initialized.
		 * 
		 * @return true if the client is ready
		 */
		private boolean isReady() {
			return ready;
		}

		/**
		 * Run method.
		 */
		public void run() {
			// to loop until LOGOUT
			boolean runningThread = true;
			while (runningThread && alive) {
				ChatMessage chatMessage = readMessage();
				if (chatMessage == null) {
					if (alive && username != null) {
						broadcast(new ChatMessage(id, MessageType.MESSAGE,
								username + " ha salido del chat."));
					}
					break;
				}

				// Switch on the type of message received
				switch (chatMessage.getType()) {
				case SHUTDOWN:
					show(username + " shutdown chat system.");
					runningThread = false;
					alive = false;
					break;
				case MESSAGE:
					broadcast(buildOutgoingMessage(chatMessage, username));
					break;
				case LOGOUT:
					show(username + " disconnected with a LOGOUT message.");
					broadcast(new ChatMessage(id, MessageType.MESSAGE,
							username + " ha salido del chat."));
					runningThread = false;
					break;
				default:
					break;
				} // switch
			}
			// remove myself from the arrayList containing the list of the
			// connected Clients			
			remove(id);
			show("Removing " + username + " with id: " + id);
			close();
			
			if (!alive) { // if was a shutdown close server
				shutdown();
			}
		} 		

		/**
		 * Reads a message from the client stream.
		 * 
		 * @return message read or null if the client disconnected
		 */
		private ChatMessage readMessage() {
			try {
				return (ChatMessage) sInput.readObject();
			} catch (IOException e) {
				show(username + " Exception reading Streams: " + e);
				return null;
			} catch (ClassNotFoundException e2) {
				close(); // organized panic case...
				throw new RuntimeException("Wrong message type", e2);
			}
		}
		

		/**
		 * Write a message to the client output stream.
		 * 
		 * @param msg message
		 * @return true if is correctly sent, false in other case.
		 */
		private boolean sendMessage(ChatMessage msg) {
			// if Client is still connected send the message to it
			if (socket == null || socket.isClosed()) {
				close();
				return false;
			}
			// write the message to the stream
			try {
				sOutput.writeObject(msg);
				sOutput.flush();
			}
			// if an error occurs, do not abort just inform the user
			catch (IOException e) {
				show("Error sending message to " + username);
				show(e.toString());
				return false;
			}
			return true;
		} // writeMsg
		
		/**
		 * Close streams and socket for client.
		 */
		private void close() {
			// try to close the connection
			try {				
				if (sOutput != null) {
					sOutput.close();
					sOutput = null;
				}
				if (sInput != null) {
					sInput.close();
					sInput = null;
				}
				if (socket != null && !socket.isClosed()) {
					socket.close();
				}

			} catch (Exception e) {
				show("Closed streams and socket");
			}
		}

	}
}