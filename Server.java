import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

@SuppressWarnings("serial")
public class Server extends JFrame {

	/*
	 * Declaring a bunch of things
	 */
	private ServerSocket server;//server host
	private ArrayList<Player> players;//arraylist of "players" or users who are connected to the server
	private ArrayList<Player> playersInChat;//arraylist of players who are connected to the server and have the chatbox open

	private JTextPane main;//shows us text going on in chatbox

	private JTextField announce;//used for server to send a message
	private JPanel northPanel;//used to fit BorderLayout
	private JButton users;//check users who are connected to chatbox

	private boolean running;//running / listening or not?

	private final HTMLEditorKit kit = new HTMLEditorKit();//used to insertHTML

	//used to call constructor
	public static void main(String[] args) {
		new Server(6789);
	}

	/*
	 * is a certain user connected?
	 */
	private boolean connected(String user) {
		for (int i = 0; i < players.size(); i++) {
			if (user.equals(players.get(i).username))
				return true;
		}
		return false;
	}

	/*
	 * PM functionality cause why not? PM from server to User
	 * User to user is included as well later in the code
	 * loops through players and finds the username the server wants to PM, then if they are connected go ahead and send them the PM
	 */
	private void pm(String username, String message) {

		for (int i = 0; i < players.size(); i++) {
			if (players.get(i).username.equalsIgnoreCase(username)) {
				if(!players.get(i).s.isConnected()) {
					players.get(i).close();
					removeSocket(players.get(i));
					return;
				}
				try {
					players.get(i).out.writeObject(message);
					players.get(i).out.flush();
				} catch(IOException e) {
					players.get(i).close();
					removeSocket(players.get(i));
					display("Error sending message to " + players.get(i).username);
					display(e.toString());
					return;
				}
				break;
			}
		}

	}

	/*
	 * Displays the chat text
	 */
	private void display(String text) {
		try {
			kit.insertHTML((HTMLDocument)main.getDocument(), main.getDocument().getLength(), text+"\n", 0, 0, null);
			//main.getDocument().insertString(main.getDocument().getLength(), text+"\n", null);
		} catch (BadLocationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		main.setCaretPosition(main.getDocument().getLength());
	}

	/*
	 * Checks the user so he or she can successfully make it into the server
	 */
	private boolean checkUser(Player p) {
		String user = p.username;//username
		String password = p.password;//password
		File f = new File(user+".txt");//a file that may or may not exist based on the connectee's username

		//if username's account does not exist, create it (a quick way to register, in a way)
		if (!f.exists()) {
			try {
				if (user.length() <= 15) {
					p.out.writeObject("Account created! You may now continue on to the server!");
					p.out.flush();
					save(p);
					return true;
				} else {
					p.out.writeObject("Your name must be 15 characters or less!");
					p.out.flush();
					return false;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (f.exists()) {//on the other hand if it does exist, we do not want them getting through unless they are in fact, actually the owner of the account
			BufferedReader reader = null;
			String realPass = "";
			try {
				reader = new BufferedReader(new FileReader(f));
				reader.readLine();
				realPass = reader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (realPass.equals(password) && !connected(user)) {
				try {
					p.out.writeObject("Successfully connected! You may now continue on to the server!");
					p.out.flush();
					return true;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (realPass.equals(password) && connected(user)) {
				try {
					p.out.writeObject("Account is already logged in! Please try again soon.");
					p.out.flush();
					return false;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (!realPass.equals(password)) {
				try {
					p.out.writeObject("Invalid password. Please try again!");
					p.out.flush();
					return false;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return true;
	}

	/*
	 * Saves the player's / connectee's info in a .txt file (usually it is said to use .dat because it works better, but I have not looked into it)
	 */
	private void save(Player p) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(p.username+".txt"));
			writer.write(p.username, 0, p.username.length());
			writer.newLine();
			writer.write(p.password, 0, p.password.length());
			writer.close();
		} catch (IOException e) {
			p.close();
			removeSocket(p);
			try {
				p.out.writeObject("Unreachable username. Please try again.");
				p.out.flush();
			} catch (IOException er) {
				er.printStackTrace();
			}
			e.printStackTrace();
		}
		
	}

	//array for possible text that can be replaced into smiley faces, matches up with the replaceFaces array
	private final String[] possibleFaces = {";)", ":D", ":)", ":(", ":O", ":o"};

	//SMILIES - http://www.iconki.com/16x16-mega-icons-pack-363-p49.asp

	//using URL images for fun, these smiley faces match up in order with the possibleFaces array possiblesFaces[i] = replacesFaces[i] pretty much
	private final String[] replaceFaces = {
			"http://www.iconki.com/icons/Software-Applications/16x16-Mega-icons-pack/smiley-wink.png",
			"http://www.iconki.com/icons/Software-Applications/16x16-Mega-icons-pack/smiley-grin.png",
			"http://www.iconki.com/icons/Software-Applications/16x16-Mega-icons-pack/smiley.png",
			"http://www.iconki.com/icons/Software-Applications/16x16-Mega-icons-pack/smiley-sad-blue.png",
			"http://www.iconki.com/icons/Software-Applications/16x16-Mega-icons-pack/smiley-surprise.png",
			"http://www.iconki.com/icons/Software-Applications/16x16-Mega-icons-pack/smiley-surprise.png"};
			//"http://www.iconki.com/icons/Software-Applications/16x16-Mega-icons-pack/smiley-lol.png"};

	//html the chat user is unable to enter
	private final String[] invalidHtml = {"<br>", "</br>"};

	/*
	 * Broadcasts or sends a message to all the users connected
	 */
	private synchronized void broadcast(Player p, String message) {
		for (String s: invalidHtml) {
			if (message.contains(s) && p != null) {
				try {
					p.out.writeObject("Please remove "+s.replaceAll("<", "").replaceAll(">", ""));
					p.out.flush();
					return;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		for (int i = 0; i < possibleFaces.length; i++) {
			while (message.contains(possibleFaces[i])) {
				message = message.replace(possibleFaces[i], "<img src = "+replaceFaces[i]+">");
				if (!message.contains(possibleFaces[i]))
					break;
			}
		}

		display(message);
		for (int i = 0; i < players.size(); i++) {
			if (!players.get(i).writeMsg(message)) {
				removeSocket(players.get(i));
			}
		}
	}

	/*
	 * removes a connection as well as player object from the program
	 */
	private synchronized void removeSocket(Player p) {
		for (int i = 0; i < players.size(); i++) {
			if (players.get(i) == p) {
				players.get(i).close();
				players.get(i).t.stop();
				players.remove(i);
				playersInChat.remove(p);
				return;
			}
		}
	}

	/*
	 * Starts listening for data etc
	 */
	private final void start() {
		boolean fail = false;
		while (running) {
			display("Waiting for clients...");
			Socket socket = null;
			try {
				socket = server.accept();
			} catch (IOException e) {
				e.printStackTrace();
			}

			Player p = new Player(socket);
			if (!checkUser(p)) {
				display(p.username+" failed to connect to the server!");
				fail = true;
			} else {
				p.conRunning = true;
				fail = false;
			}
			players.add(p);
			if (fail)
				removeSocket(p);
			else
				p.start();
		}
	}

	/*
	 * Server constructor with port parameter
	 * Super = constructor of parent class
	 * initializing, setting location, adding Listeners, etc
	 * adding components and setting GUI visible
	 */
	private Server(int port) {
		super("Running "+port);
		setLayout(new BorderLayout());
		players = new ArrayList<Player>();
		playersInChat = new ArrayList<Player>();
		northPanel = new JPanel();
		northPanel.setLayout(new FlowLayout());
		main = new JTextPane();
		main.setContentType("text/html");
		main.setEditable(false);
		add(new JScrollPane(main), BorderLayout.CENTER);
		announce = new JTextField(20);
		announce.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (announce.getText().startsWith("pm-")) {
					String allText = announce.getText().replaceFirst("pm-", "");
					String[] allInfo = allText.split("-");
					System.out.println(allInfo[0]+" |||"+allInfo[1]);
					pm(allInfo[0], allInfo[1]);
					return;
				}
				broadcast(null, "[SERVER]: "+announce.getText());
			}
		});
		northPanel.add(announce);
		users = new JButton("Current users");
		users.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				display("Users currently active in chat: \n");
				for (int i = 0; i < playersInChat.size(); i++) {
					display(playersInChat.get(i).username);
				}
			}
		});
		northPanel.add(users);
		add(northPanel, BorderLayout.NORTH);
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}

		setSize(500, 500);
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		running = true;
		start();
	}

	/*
	 * The "Player" class or in other words "User of our program"'s class
	 */
	class Player extends Thread {

		Socket s;//separate connection with the server of a user
		ObjectOutputStream out;//output stream to send to user
		ObjectInputStream in;//inputstream to read message from that user

		//this user's username and password
		String username;
		String password;

		boolean conRunning;//connection active with this user or not
		Timer t;//used to time how quickly a user can send another message

		/*
		 * Constructor for a player/user in our program
		 */
		Player(Socket s) {
			this.s = s;
			try {
				out = new ObjectOutputStream(s.getOutputStream());
				in = new ObjectInputStream(s.getInputStream());
				try {
					username = in.readObject().toString();
					password = in.readObject().toString();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			t = new Timer(1000, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					t.stop();
				}
			});
		}

		private boolean privateMessage;//is the message a private message?
		private boolean invalidMessage;//invalid message or not

		/*
		 * This runs when this class' run function is called
		 * Run is usually called when you start a new Thread
		 * Thread is parent class of this class
		 * everything here is self-explanatory based on the "   " messages
		 */
		public void run() {
			while (conRunning) {//while active connection, listen for information
				String msg = "";
				try {
					msg = in.readObject().toString();
					privateMessage = false;
					invalidMessage = false;
					if (msg.equals(username+" logged out.") || msg.equals(username+" joined the chatroom!") || msg.equals(username+" closed out the server.") || msg.equals(username+" closed out the chatbox.")) {
						t.stop();
						if (msg.equals(username+" joined the chatroom!"))
							playersInChat.add(this);
						else if (msg.equals(username+" closed out the chatbox."))
							playersInChat.remove(this);
					}
				} catch (IOException e) {
					close();
					removeSocket(this);
					display("Error for ("+username+"): "+e.getMessage());
					break;
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				/*
				 * Here is the user to user PM, to user it simply type "message "+thePersonYouWantToPM+" the message here"
				 */
				String tempMsg = msg.toLowerCase();
				if (msg.startsWith(username+": message ")) {
					System.out.println(username);
					for (Player p: playersInChat) {
						if (tempMsg.startsWith((username+": message "+p.username).toLowerCase())) {
							/*
								a fix that I thought of 1:27 P.M. Feb 6 2018
								create a messageObject that has a username attribute and a message attribute
								will allow you to check username if needed
								Unnecessary, but if you do not want a player messaging him or herself, uncomment this.
								1 glitch however, if a player's name has the same beginning letters of anothers for ex.
								user 1 = murtuza
								user 2 = murtuza2
								murtuza cannot message murtuza2 because it thinks they are the same user
							*/
							/*
							if (p.username.equals(username)) {
								try {
									out.writeObject("<b><font color = red><font size = 5>You can't send a message to yourself!</font></b>");
									out.flush();
									invalidMessage = true;
								} catch (IOException e) {
									close();
									removeSocket(this);
									display("Error for ("+username+"): "+e.getMessage());
								}
								break;
							}*/
							//pm glitch here, if you do not capitalize the first initial of the username, it will glitch the message
							/*
								the fix that I thought of is, replace the first username+": message ";
								after, replaceIndex (0, p.username.length); with nothing (replaceIndex does not exist (strings are immutable) so I have to find an alternative.
								that will get rid of the text perfectly
							 */
							String pm = msg.replaceFirst(username+": message "+p.username, "");
							pm(p.username, "</b><font size = 5><i>[PM]</i> from "+username+": "+pm+"</font></b>");
							try {
								out.writeObject("</b><font size = 5><i>[PM]</i> to "+p.username+": "+pm+"</font></b>");
								out.flush();
							} catch (IOException er) {
								er.printStackTrace();
							}
							privateMessage = true;
							break;
						}
					}
				}
				if (msg.equals(username+" logged out.") || msg.equals(username+" was removed from the server.") || msg.equals(username+" closed out the server.")) {
					close();
					display(username+" disconnected from ip"+s.getInetAddress());
					removeSocket(this);
					break;
				}
				if (msg.equals("WHOISIN")) {
					try {
						out.writeObject("<font size='5'><i><b>Users currently active in chat: \n</b></i></font>");
						for (int i = 0; i < playersInChat.size(); i++)	
							out.writeObject("<font size='4'>"+playersInChat.get(i).username+"</font>");
						out.writeObject("\n");
						out.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (!msg.equals("WHOISIN")) {
					if (!t.isRunning() && !privateMessage && !invalidMessage) {
						broadcast(this, msg);
						t.start();
					} else if (privateMessage && !t.isRunning() && !invalidMessage) {
						t.start();
						System.out.println("Sent a message");
					} else if (!invalidMessage) {
						try {
							out.writeObject("<font size='5'>Please wait 1 second before typing each message.</font>");
							out.flush();
						} catch (IOException e) {
							close();
							removeSocket(this);
							display("Error for ("+username+"): "+e.getMessage());
						}
					}
				}
			}

		}

		/*
		 * Close the connection of this player
		 */
		private void close() {
			try {
				if (out != null)
					out.close();
				if (in != null)
					in.close();
				if (s != null)
					s.close();
			} catch (IOException er) {
				er.printStackTrace();
			}
		}

		//used to write msg back to the user otherwise if not possible, get rid of this connection to prevent further errors
		private boolean writeMsg(String msg) {
			if(!s.isConnected()) {
				close();
				removeSocket(this);
				return false;
			}
			try {
				out.writeObject(msg);
			}
			catch(IOException e) {
				close();
				removeSocket(this);
				display("Error sending message to " + username);
				display(e.toString());
			}
			return true;
		}

	}
}