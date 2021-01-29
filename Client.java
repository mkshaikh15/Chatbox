import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

@SuppressWarnings("serial")
public class Client implements ActionListener {

	private JTextField usernameField;//where you enter your username
	private JPasswordField passwordField;//where you enter your password

	private JButton login;//login button

	//the username and password you type, sends these two to server to verify acc
	private String username;
	private String password;

	private boolean chatVisible = false;//whether or not chat is visible

	//our data streams used to communicate with the server via output and input
	private ObjectOutputStream out;
	private ObjectInputStream in;

	private Socket con;//our connection with the server

	private String server;//server address we are connecting to
	private int port;//port we are connecting on

	private JButton chatbox;//launches the chatbox
	private JButton newCommand;//any additional command you want to add to the login client

	private boolean running;//do we still have a connection with the server? if so chat is currently "running"

	private Chatbox chat;//the actual Chatbox window


	/*
	 * Just returns a properly capitalized String, for example murtuza -> Murtuza
	 * Totally un-needed, but incase I had to capitalize other messages, also made it simpler, etc.
	 */
	public String capitalize(String s) {
		if (s.length() == 0) return s;
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}

	/*
	 * Shut down the connections and output/input streams
	 */
	private void close() {
		try {
			out.close();
			in.close();
			con.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Logs you out and enables the components so you may login again if you desire
	 */
	private void logout() {
		close();
		login.setText("Login");
		usernameField.setEditable(true);
		passwordField.setEditable(true);
	}

	private boolean connected;// whether or not you are connected to the server

	public static JFrame frame;//Main GUI

	private String message;//the message you will be sending to the server

	/*
	 * Login method
	 * Establishes a connection with the server
	 * Sends initial data. Specifically username and password because that is necessary info to login
	 * Server then reads that info and responds whether or not you were able to successfully login
	 * If server is offline, it will let you know
	 */
	private void login() {
		username = capitalize(usernameField.getText().trim());
		password = new String(passwordField.getPassword());

		try {
			con = new Socket(server, port);
			out = new ObjectOutputStream(con.getOutputStream());
			in = new ObjectInputStream(con.getInputStream());
			out.writeObject(username);
			out.writeObject(password);
			out.flush();
			try {
				message = in.readObject().toString();
				System.out.println(message);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			JOptionPane.showMessageDialog(frame, message, "Connection", JOptionPane.INFORMATION_MESSAGE);
			if (message.startsWith("Invalid") || message.startsWith("Account is already") || message.startsWith("Your name")) {
				out.writeObject(username+" was removed from the server.");
				out.flush();
				connected = false;
				close();
				return;
			} else {
				out.writeObject(username+" has connected to the server");
				out.flush();
				connected = true;
				running = true;
			}
			login.setText("Logout");
			newCommand.setEnabled(true);
			usernameField.setEditable(false);
			passwordField.setEditable(false);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(frame, "Server is offline. Please try again later.", "Server", JOptionPane.INFORMATION_MESSAGE);
		}
		new ListenFromServer().start();
	}

	/*
	 * Main method, launches our main program
	 */
	public static void main(String[] args) {
		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);
		new Client("127.0.0.1", 6789);
		SwingUtilities.updateComponentTreeUI(frame);
	}

	/*
	 * Goes with the ActionListener
	 * In our case listens for whether or not a button is clicked on.
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Login")) {
			if (usernameField.getText().trim().length() > 0 && new String(passwordField.getPassword()).length() > 0 && !connected) {
				login();
				if (connected)
					chatbox.setEnabled(true);
			}
			return;
		} else if (e.getActionCommand().equals("Logout")) {
			if (connected) {
				try {
					out.writeObject(username+" logged out.");
					out.flush();
				} catch (IOException er) {
					er.printStackTrace();
				}
				if (chatVisible) {
					chat.dispose();
					chatVisible = false;
				}
				connected = false;
				logout();
			}
			return;
		} else if (e.getSource() == chatbox) {
			if (!chatVisible) {
				chat = new Chatbox();
				chat.setVisible(true);
				chat.setSize(565, 485);
				chat.setResizable(false);
				chat.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				chatVisible = true;
				try {
					out.writeObject(username+" joined the chatroom!");
				} catch (IOException er) {
					er.printStackTrace();
				}
			} else if (chatVisible) {
				chat.setVisible(true);
			}
			return;
		}
	}

	/*
	 * Client constructor
	 * Here we initialize many variables, strings, objects, etc so we can run our program smoothly and efficiently
	 * Once everything is initialized properly, we add components to the GUI
	 */
	public Client(String server, int port) {
		frame = new JFrame("Running: "+server+": "+port);

		this.server = server;
		this.port = port;
		frame.setLayout(null);

		JLabel commandsLabel = new JLabel("Commands");

		commandsLabel.setFont(new Font("Times New Roman", Font.BOLD+Font.ITALIC, 50));
		commandsLabel.setBounds(40, 150, 250, 50);
		frame.add(commandsLabel);

		JLabel usernameLabel = new JLabel("Username:");
		JLabel passwordLabel = new JLabel("Password:");

		usernameLabel.setBounds(5, 10, 100, 30);
		usernameLabel.setFont(new Font("Times New Roman", Font.ITALIC, 20));
		frame.add(usernameLabel);

		usernameField = new JTextField(15);
		usernameField.setBounds(115, 10, 200, 30);
		frame.add(usernameField);

		passwordLabel.setBounds(5, 60, 100, 30);
		passwordLabel.setFont(new Font("Times New Roman", Font.ITALIC, 20));
		frame.add(passwordLabel);

		passwordField = new JPasswordField(15);
		passwordField.setBounds(115, 60, 200, 30);
		frame.add(passwordField);

		login = new JButton("Login");
		login.addActionListener(this);
		login.setFont(new Font("Times New Roman", Font.ROMAN_BASELINE, 22));
		login.setBounds(65, 110, 200, 30);
		frame.add(login);

		chatbox = new JButton("Chatbox");
		chatbox.addActionListener(this);
		chatbox.setBounds(10, 215, 130, 40);
		chatbox.setEnabled(false);
		frame.add(chatbox);

		newCommand = new JButton("Command");
		newCommand.setEnabled(false);
		newCommand.addActionListener(this);
		newCommand.setBounds(180, 215, 130, 40);
		frame.add(newCommand);

		frame.setSize(335, 500);
		frame.setVisible(true);

		//WindowListener, pretty much using it to send info if the user closes out of the client
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				try {
					out.writeObject(username+" closed out the server.");
					out.flush();
					close();
				} catch (Exception er) {
					System.out.println("Connection hadn't been set.");
				}
				System.exit(0);
			}
		});

	}

	/*
	 * Name is just like it sounds
	 * This class listens from the server for any incoming data and acts appropriately based on it
	 */
	class ListenFromServer extends Thread {

		public void run() {
			while(running) {
				try {
					message = in.readObject().toString();
					if (message.equals("Unreachable username. Please try again."))
						JOptionPane.showMessageDialog(chat, message);
					else if (!message.equals("Unreachable username. Please try again.")) {
						if (chat != null)
							chat.append(message);
						else
							System.out.println(message);
					}
				} catch(IOException e) {
					logout();
					chatbox.setEnabled(false);
					newCommand.setEnabled(false);
					if (chat != null) {
						chat.dispose();
						chatVisible = false;
					}
					connected = false;
					running = false;
					JOptionPane.showMessageDialog(frame, "Disconnected.", "Connection", JOptionPane.INFORMATION_MESSAGE);
					break;
				} catch(ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}

	}

	/*
	 * The Chatbox class
	 * This is the actual chatbox we launch through the LoginClient
	 */
	class Chatbox extends JFrame {

		private boolean enterHeld, controlHeld;//enter key held or not, control key held or not. Both are used as a shortcut to send a message in the chatbox, instead of clicking send
		private final JTextPane messageField, chat;//the message text area and the chat text area
		private final JButton send, usersActive;
		private final JTextArea temporaryTextArea = new JTextArea();

		private final HTMLEditorKit kit = new HTMLEditorKit();


		/*
		 * Appends a message to the chat text pane and since Java text components can support HTML, we use that to support fun things like smileys
		 * Such as :) :O :( etc to liven up our chatbox
		 */
		public void append(String message) {
			try {
				kit.insertHTML((HTMLDocument)chat.getDocument(), chat.getDocument().getLength(), message, 0, 0, null);
				chat.setCaretPosition(chat.getDocument().getLength());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/*
		 * Returns the line count of the specified JTextPane
		 */
		private int getLineCount(JTextPane pane) {
			String text = "";
			try {
				text = pane.getDocument().getText(0, pane.getDocument().getLength());
			} catch (BadLocationException e) {
				e.printStackTrace();
			};
			temporaryTextArea.setText(text);
			temporaryTextArea.setFont(chat.getFont());
			int lineCount = temporaryTextArea.getLineCount();
			return lineCount;
		}

		/*
		 * Retrieves the text of a JTextPane from the very beginning
		 */
		private String getText(JTextPane pane) throws BadLocationException {
			return pane.getDocument().getText(0, pane.getDocument().getLength());
		}

		/*
		 * Sends the actual message to the server based on certain conditions that must be met.
		 */
		private void sendMessage() {
			messageField.requestFocus();//once message is sent we want focus to go back to the message field to make it easier for the user to send another message
			if (messageField.getText().trim().length() > 0) {//if the is no text, why send a message
				try {
					if (getText(messageField).length() <= 150) {//too long message, we don't want spam.
						if (getLineCount(messageField) <= 4) {//too many lines (this could be removed, because in our chat lines act as spaces. In other forms they may not, however.
							try {
								if (out != null) {
									out.writeObject(username+": "+messageField.getText());//send message
									out.flush();//flush out everything in the output stream
								}
							} catch (IOException er) {
								er.printStackTrace();
							}
							messageField.setText("");
						} else
							JOptionPane.showMessageDialog(Chatbox.this, "You are limited to 4 lines only!", "Error", JOptionPane.ERROR_MESSAGE);
					} else
						JOptionPane.showMessageDialog(Chatbox.this, "Message is too long! 150 characters or less only!", "Error", JOptionPane.ERROR_MESSAGE);
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
			} else
				JOptionPane.showMessageDialog(Chatbox.this, "Enter a message to send!", "Error", JOptionPane.ERROR_MESSAGE);
			enterHeld = false;
			controlHeld = false;
		}

		/*
		 * Titling the chatbox using the constructor of JFrame through the super() function built into Java
		 * Initializing several objects, setting their location, etc and adding them to the visual
		 * 
		 */
		public Chatbox() {
			super("Chatbox");

			setLayout(null);

			setLayout(null);

			usersActive = new JButton("Users active");
			usersActive.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						out.writeObject("WHOISIN");
						out.flush();
					} catch (IOException er) {
						er.printStackTrace();
					}
				}
			});
			usersActive.setBounds(10, 300, 95, 30);
			add(usersActive);

			send = new JButton("Send");
			send.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					sendMessage();
				}
			});
			send.setBounds(420, 340, 120, 100);
			add(send);

			chat = new JTextPane();
			chat.setContentType("text/html");
			chat.setEditable(false);
			JScrollPane chatScroll = new JScrollPane(chat);
			chatScroll.setBounds(10, 10, 530, 280);
			add(chatScroll);

			messageField = new JTextPane();
			/*
			 * KeyListener
			 * Used to check whether or not ctrl+enter is held in the messageField
			 * If it is, go ahead and call the sendMessage function
			 * This makes it easier to send a message instead of clicking the send button each time
			 */
			messageField.addKeyListener(new KeyAdapter() {
				public void keyReleased(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						enterHeld = false;
					}
					if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
						controlHeld = false;
					}
				}
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						enterHeld = true;
					}
					if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
						controlHeld = true;
					}
					if (enterHeld && controlHeld) {
						sendMessage();
					}
				}
			});

			JScrollPane messageFieldScroll = new JScrollPane(messageField);
			messageFieldScroll.setBounds(10, 340, 400, 100);
			add(messageFieldScroll);

			/*
			 * WindowListener used here to tell the server if a user closed out of the chatbox
			 * Remember, the chatbox is a part of our LoginClient
			 * If we close the chatbox we are still connected to the server.
			 */
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					chatVisible = false;
					try {
						out.writeObject(username+" closed out the chatbox.");
						out.flush();
					} catch (IOException er) {
						er.printStackTrace();
					}
				}
			});

		}

	}
}