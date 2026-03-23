package es.ubu.lsi.client;

import java.io.*;
import java.net.*;
import java.util.*;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.ChatMessage.MessageType;

/**
 * Client.
 * 
 * @author http://www.dreamincode.net
 * @author Raúl Marticorena
 * @author Joaquin P. Seco
 *
 */
public class ChatClientImpl implements ChatClient {
	
	/** Default port. */
	private static final int DEFAULT_PORT = 1500;
	
	/** Default server. */
	private static final String DEFAULT_SERVER = "localhost";
	
	/** Input stream. */
	private ObjectInputStream sInput; // to read from the socket
	/** Output stream. */
	private ObjectOutputStream sOutput; // to write on the socket
	/** Socket. */
	private Socket socket;

	/** Server name/IP. */
	private String server;
	/** User name. */
	private String username;
	/** Port. */
	private int port;
	
	/** Flag to keep running main thread. */
	private volatile boolean carryOn = true;

	/** Id. */
	private int id;
	
	/** Blocked users. */
	private final Set<String> bannedUsers = Collections.synchronizedSet(new HashSet<String>());

	/**
	 * Constructor.
	 * 
	 * @param server server
	 * @param port port
	 * @param username user name
	 * 
	 */
	public ChatClientImpl(String server, int port, String username) {
		// which calls the common constructor with the GUI set to null
		this.server = server;
		this.port = port;
		this.username = username;
	}

	/**
	 * Starts chat.
	 * 
	 * @return true if everything goes right, false in other case
	 */
	@Override
	public boolean start() {
		// try to connect to the server
		try {
			socket = new Socket(server, port);
			String msg = "Connection accepted " + socket.getInetAddress() + ":"
					+ socket.getPort();
			display(msg);
			sOutput = new ObjectOutputStream(socket.getOutputStream());
			sOutput.flush();
			sInput = new ObjectInputStream(socket.getInputStream());
			
		}
		catch (IOException eIO) {
			display("Exception creating new Input/output Streams: " + eIO);
			return false;
		}
		catch (Exception ec) {
			display("Error connectiong to server:" + ec);
			return false;
		}			
		// Login and receive id
		try {			
			sOutput.writeObject(username);	
			sOutput.flush();
			id = sInput.readInt();
		} catch (IOException eIO) {
			display("Exception doing login : " + eIO);
			disconnect();
			return false;
		}
		// creates the Thread to listen from the server
		new Thread(new ChatClientListener()).start();
		// success we inform the caller that it worked
		return true;
	}

	/**
	 * Displays messages.
	 * 
	 * @param msg text to show in console
	 */
	private void display(String msg) {
		System.out.println(msg); // println in console mode
	}

	/**
	 * Sends a message to the server.
	 * 
	 * @param msg message
	 */
	@Override
	public synchronized void sendMessage(ChatMessage msg) {
		try {
			if (this.carryOn && sOutput != null) {
				sOutput.writeObject(msg);
				sOutput.flush();
			}
		} catch (IOException e) {
			display("Exception writing to server: " + e);
			carryOn = false;
		}
	}

	/**
	 * Disconnect client closing resources.
	 */
	@Override
	public void disconnect() {
		
		carryOn = false;
		try {
			display("Trying to disconnect and close client with username " + username);
			if (sInput != null)  {
				sInput.close();
				sInput = null;
			}
			if (sOutput != null) {
				sOutput.close();
				sOutput = null;
			}
			if (socket != null && !socket.isClosed()) {
				socket.close();
				socket = null;
			}
		} catch (Exception e) {
			
			display("Disconnect with error, closing resources, closed previously.");
		}
		finally{
			display("Bye!");
		}
	}

	/**
	 * Starts the client.
	 * 
	 * To start the Client in console mode use one of the following command >
	 * java Client > java Client username > java Client username portNumber >
	 * java Client username portNumber serverAddress at the console prompt If
	 * the portNumber is not specified 1500 is used If the serverAddress is not
	 * specified "localHost" is used If the username is not specified
	 * "Anonymous" is used > java Client is equivalent to > java Client
	 * Anonymous 1500 localhost are equivalent.
	 * 
	 * In console mode, if an error occurs the program simply stops when a GUI
	 * id used, the GUI is informed of the disconnection
	 * 
	 * @param args arguments
	 */
	public static void main(String[] args) {
		// Adaptado al enunciado actual: [server] username con puerto fijo 1500.
		String serverAddress = DEFAULT_SERVER;
		String userName;

		if (args.length == 1) {
			userName = args[0];
		} else if (args.length == 2) {
			serverAddress = args[0];
			userName = args[1];
		} else {
			System.err.println("Usage is: > java es.ubu.lsi.client.ChatClientImpl [serverAddress] username");
			return;
		}
		// create the Client object
		ChatClient client = new ChatClientImpl(serverAddress, DEFAULT_PORT, userName);
		// test if we can start the connection to the Server
		// if it failed nothing we can do
		if (!client.start()) {
			System.err.println("Error connecting server. Check network and server status.");
			return;
		}

		// wait for messages from user
		ChatClientImpl clientChat = ((ChatClientImpl) client);
		try (Scanner scan = new Scanner(System.in)) {
			// loop forever for message from the user
			while (clientChat.carryOn) {
				System.out.print("> ");
				String userMsg = scan.nextLine().trim();
				if (userMsg.length() == 0) {
					continue;
				}
				if (clientChat.handleLogout(userMsg)) {
					break;
				}
				if (clientChat.handleBan(userMsg) || clientChat.handleUnban(userMsg)) {
					continue;
				}
				client.sendMessage(new ChatMessage(clientChat.id, MessageType.MESSAGE, userMsg));
			}
		}
		// done disconnect by logout
		client.disconnect();		
	}

	/**
	 * Handles logout command.
	 * 
	 * @param userMsg message entered by user
	 * @return true if logout command was received
	 */
	private boolean handleLogout(String userMsg) {
		if (!userMsg.equalsIgnoreCase(MessageType.LOGOUT.toString())) {
			return false;
		}
		sendMessage(new ChatMessage(id, MessageType.LOGOUT, MessageType.LOGOUT.toString()));
		return true;
	}

	/**
	 * Handles ban command.
	 * 
	 * @param userMsg message entered by user
	 * @return true if ban command was received
	 */
	private boolean handleBan(String userMsg) {
		if (!userMsg.toLowerCase().startsWith("ban ")) {
			return false;
		}

		String bannedUser = extractNickname(userMsg);
		if (bannedUser.length() == 0) {
			display("Usage: ban nickname");
			return true;
		}
		bannedUsers.add(bannedUser);
		sendMessage(new ChatMessage(id, MessageType.MESSAGE,
				username + " ha baneado a " + bannedUser));
		return true;
	}

	/**
	 * Handles unban command.
	 * 
	 * @param userMsg message entered by user
	 * @return true if unban command was received
	 */
	private boolean handleUnban(String userMsg) {
		if (!userMsg.toLowerCase().startsWith("unban ")) {
			return false;
		}

		String unbannedUser = extractNickname(userMsg);
		if (unbannedUser.length() == 0) {
			display("Usage: unban nickname");
			return true;
		}
		bannedUsers.remove(unbannedUser);
		return true;
	}

	/**
	 * Extracts nickname from command.
	 * 
	 * @param userMsg message entered by user
	 * @return extracted nickname
	 */
	private String extractNickname(String userMsg) {
		String[] parts = userMsg.split("\\s+", 2);
		if (parts.length < 2) {
			return "";
		}
		return parts[1].trim();
	}

	/**
	 * Checks if message must be shown.
	 * 
	 * @param msg message received from server
	 * @return true if message can be shown
	 */
	private boolean shouldDisplay(ChatMessage msg) {
		if (msg.getId() == id) {
			return false;
		}
		String sender = extractSender(msg.getMessage());
		return sender == null || !bannedUsers.contains(sender);
	}

	/**
	 * Extracts sender nickname from the text received.
	 * 
	 * @param text text received from server
	 * @return sender nickname or null if it cannot be determined
	 */
	private String extractSender(String text) {
		String cleanText = removeTimestamp(text);
		int separator = cleanText.indexOf(":");
		if (separator > 0) {
			return cleanText.substring(0, separator).trim();
		}
		return extractSenderFromEvent(cleanText);
	}

	/**
	 * Extracts sender nickname from server events.
	 * 
	 * @param text event text
	 * @return sender nickname or null if it cannot be determined
	 */
	private String extractSenderFromEvent(String text) {
		String connectToken = " se ha conectado al chat.";
		int connectIndex = text.indexOf(connectToken);
		if (connectIndex > 0) {
			return text.substring(0, connectIndex).trim();
		}

		String disconnectToken = " ha salido del chat.";
		int disconnectIndex = text.indexOf(disconnectToken);
		if (disconnectIndex > 0) {
			return text.substring(0, disconnectIndex).trim();
		}

		String banToken = " ha baneado a ";
		int banIndex = text.indexOf(banToken);
		if (banIndex > 0) {
			return text.substring(0, banIndex).trim();
		}
		return null;
	}

	/**
	 * Removes timestamp from server message.
	 * 
	 * @param text text with timestamp
	 * @return text without timestamp
	 */
	private String removeTimestamp(String text) {
		if (text == null || text.length() < 9) {
			return text;
		}
		if (Character.isDigit(text.charAt(0)) && Character.isDigit(text.charAt(1))
				&& text.charAt(2) == ':' && Character.isDigit(text.charAt(3))
				&& Character.isDigit(text.charAt(4)) && text.charAt(5) == ':'
				&& Character.isDigit(text.charAt(6)) && Character.isDigit(text.charAt(7))
				&& text.charAt(8) == ' ') {
			return text.substring(9);
		}
		return text;
	}

	/**
	 * Client listener for messages from server.
	 * 
	 */
	class ChatClientListener implements Runnable {
		
		/**
		 * Constructor.
		 */
		public ChatClientListener() {
			// Empty constructor.
		}
		
		/**
		 * Run.
		 */
		public void run() {
			while (carryOn) {
				try {
					ChatMessage msg = (ChatMessage) sInput.readObject();
					if (shouldDisplay(msg)) {
						// if console mode print the message and add back the prompt
						System.out.println(msg.getMessage());
						System.out.print("\n> ");
					}
						
				} catch (IOException e) {
					if (carryOn) {
						display("Server has closed the connection. ");
					}
					carryOn = false;
					break;
				} catch (ClassNotFoundException e2) {
					throw new RuntimeException("Wrong message type", e2);
				}
			} // while
		} // run
	} // ChatClientListener

} // ChatClient
