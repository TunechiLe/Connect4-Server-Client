package core;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.io.*;
import java.net.*;


public class Connect4Client extends JApplet implements Runnable{
	
	public int player1 = 1;
	public int player2 = 2;
	public int player1_won = 1;
	public int player2_won = 2;
	public int draw = 3;
	private int rowSelected;
	private int columnSelected;
	
	private boolean myTurn = false;
	private boolean continueToPlay = true;
	private boolean waiting = true;
	private boolean isStandAlone = false;
	
	private char myToken = ' ';
	private char otherToken = ' ';

	private Cell[][] cell =  new Cell[6][7];
	private JLabel jlblTitle = new JLabel();
	private JLabel jlblStatus = new JLabel();
	
	private DataInputStream fromServer;
	private DataOutputStream toServer;
	
	private String host = "localhost";

  /** Initialize UI */
  public void init() {
    JPanel p = new JPanel();
    p.setLayout(new GridLayout(6, 7, 0, 0));
    for (int i = 0; i < 6; i++)
      for (int j = 0; j < 7; j++)
        p.add(cell[i][j] = new Cell(i, j, cell));

    // Set properties for labels and borders for labels and panel
    p.setBorder(new LineBorder(Color.black, 1));
    jlblTitle.setHorizontalAlignment(JLabel.CENTER);
    jlblTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
    jlblTitle.setBorder(new LineBorder(Color.black, 1));
    jlblStatus.setBorder(new LineBorder(Color.black, 1));

    // Place the panel and the labels to the applet
    add(jlblTitle, BorderLayout.NORTH);
    add(p, BorderLayout.CENTER);
    add(jlblStatus, BorderLayout.SOUTH);

    // Connect to the server
    connectToServer();
  }

  private void connectToServer() {
    try {
      // Create a socket to connect to the server
      Socket socket;
      if (isStandAlone)
        socket = new Socket(host, 8000);
      else
        socket = new Socket(getCodeBase().getHost(), 8000);
      // Create an input stream to receive data from the server
      fromServer = new DataInputStream(socket.getInputStream());
      // Create an output stream to send data to the server
      toServer = new DataOutputStream(socket.getOutputStream());
    }
    catch (Exception ex) {
      System.err.println(ex);
    }
    // Control the game on a separate thread
    Thread thread = new Thread(this);
    thread.start();
  }
  
  public void run() {
    try {
      // Get notification from the server
      int player = fromServer.readInt();
      // Am I player 1 or 2?
      if (player == player1) {
        myToken = 'r';
        otherToken = 'y';
        jlblTitle.setText("Player 1 with color red");
        jlblStatus.setText("Waiting for player 2 to join");
        
        // Receive startup notification from the server
        fromServer.readInt(); // Whatever read is ignored

        // The other player has joined
        jlblStatus.setText("Player 2 has joined. I start first");

        // It is my turn
        myTurn = true;
      }
      else if (player == player2) {
        myToken = 'y';
        otherToken = 'r';
        jlblTitle.setText("Player 2 with color yellow");
        jlblStatus.setText("Waiting for player 1 to move");
      }

      // Continue to play
      while (continueToPlay) {
        if (player == player1) {
          waitForPlayerAction(); // Wait for player 1 to move
          sendMove(); // Send the move to the server
          receiveInfoFromServer(); // Receive info from the server
        }
        else if (player == player2) {
          receiveInfoFromServer(); // Receive info from the server
          waitForPlayerAction(); // Wait for player 2 to move
          sendMove(); // Send player 2's move to the server
        }
      }
    }
    catch (Exception ex) {
    }
  }

  /** Wait for the player to mark a cell
    @throws InterruptedException Wait Time  */
  private void waitForPlayerAction() throws InterruptedException {
    while (waiting) {
      Thread.sleep(100);
    }

    waiting = true;
  }

  /** Send this player's move to the server
   @throws IOException Checks if move has been sent */
  private void sendMove() throws IOException {
    toServer.writeInt(rowSelected); // Send the selected row
    toServer.writeInt(columnSelected); // Send the selected column
  }

  /** Receive info from the server
   @throws IOException Checks if info has been receieved */
  private void receiveInfoFromServer() throws IOException {
    // Receive game status
    int status = fromServer.readInt();

    if (status == player1_won) {
      // Player 1 won, stop playing
      continueToPlay = false;
      if (myToken == 'r') {
        jlblStatus.setText("I won! (red)");
      }
      else if (myToken == 'y') {
        jlblStatus.setText("Player 1 (red) has won!");
        receiveMove();
      }
    }
    else if (status == player2_won) {
      // Player 2 won, stop playing
      continueToPlay = false;
      if (myToken == 'y') {
        jlblStatus.setText("I won! (yellow)");
      }
      else if (myToken == 'r') {
        jlblStatus.setText("Player 2 (yellow) has won!");
        receiveMove();
      }
    }
    else if (status == draw) {
      // No winner, game is over
      continueToPlay = false;
      jlblStatus.setText("Game is over, no winner!");

      if (myToken == 'y') {
        receiveMove();
      }
    }
    else {
      receiveMove();
      jlblStatus.setText("My turn");
      myTurn = true; // It is my turn
    }
  }

  private void receiveMove() throws IOException {
    // Get the other player's move
    int row = fromServer.readInt();
    int column = fromServer.readInt();
    cell[row][column].setToken(otherToken);
  }

  // An inner class for a cell
  public class Cell extends JPanel {
    // Indicate the row and column of this cell in the board
    private int row;
    private int column;
    private Cell[][] cell;

    // Token used for this cell
    private char token = ' ';

    public Cell(int row, int column, Cell[][] cell) {
      this.row = row;
      this.cell = cell;
      this.column = column;
      setBorder(new LineBorder(Color.black, 1)); // Set cell's border
      addMouseListener(new ClickListener());  // Register listener
    }

    /** Return token
     @return Gets the token */
    public char getToken() {
      return token;
    }

    /** Set a new token
     @param c Creates a token as a character */
    public void setToken(char c) {
      token = c;
      repaint();
    }

    /** Paint the cell */
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (token == 'r') {
        g.drawOval(9, 9, getWidth() - 20, getHeight() - 20);
        g.setColor(Color.red);
        g.fillOval(9 ,9,  getWidth() - 20, getHeight() - 20);
      }
      else if (token == 'y') {
        g.drawOval(10, 10, getWidth() - 20, getHeight() - 20);
        g.setColor(Color.yellow);
        g.fillOval(9 ,9,  getWidth() - 20, getHeight() - 20);
      }
    }

    /** Handle mouse click on a cell */
    private class ClickListener extends MouseAdapter {
      public void mouseClicked(MouseEvent e) {
      	int r= -1;
      	for(int x =5; x>= 0; x--){
      		if(cell[x][column].getToken() == ' '){
      			r = x;
      			break;
      		}
      	}
        // If cell is not occupied and the player has the turn
        if ((r != -1) && myTurn) {
          cell[r][column].setToken(myToken);  // Set the player's token in the cell
          myTurn = false;
          rowSelected = r;
          columnSelected = column;
          jlblStatus.setText("Waiting for the other player to move");
          waiting = false; // Just completed a successful move
        }
      }
    }
  }

  /** This main method enables the applet to run as an application 
   @param args main method*/
  public static void main(String[] args) {
    // Create a frame
    JFrame frame = new JFrame("Connect Four Client");

    // Create an instance of the applet
    Connect4Client applet = new Connect4Client();
    applet.isStandAlone = true;

    // Get host
    if (args.length == 1) applet.host = args[0];

    // Add the applet instance to the frame
    frame.getContentPane().add(applet, BorderLayout.CENTER);

    // Invoke init() and start()
    applet.init();
    applet.start();

    // Display the frame
    frame.setSize(640, 600);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
  }
}