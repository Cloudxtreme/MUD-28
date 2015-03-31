package src;

import java.rmi.RemoteException;
import java.util.Vector;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayDeque;

class Player implements PlayerInterface, Serializable
{
    public String username;
    public PlayerState status;
    public String curLocation;
    private Vector<String> inventory;
    public transient BufferedReader inputStream;
    public transient ArrayDeque<PlayerMessage> messageBuffer; // a buffer of chat messages.

    public Player( String name )
    {
      username = name;
      status = PlayerState.NONE;
      inventory = new Vector<>();
      inventory.add("map");
      inputStream = new BufferedReader(new InputStreamReader(System.in));
      messageBuffer = new ArrayDeque<>();
    }

    /* Wrapper for Saver method to be called on a player remotely.
       Allows the player's state to be saved by a call from the server,
       e.g. when the server aborts/shuts down. */
    public void saveGame()
    {
      Saver.saveState(this);
    }

    public Vector<String> inventory()
    {
      return inventory;
    }

    public void addInventory(String item)
    {
      inventory.add(item);
    }

    public void removeInventory(String item)
    {
      inventory.remove(item);
    }

    public void clearInventory()
    {
      inventory.removeAllElements();
    }

    public String username()
    {
      return username;
    }

    public PlayerState status()
    {
      return status;
    }

    public void setStatus(PlayerState _status)
    {
      status = _status;
    }

    public String curLocation()
    {
      return curLocation;
    }

    public void setLocation(String loc)
    {
      curLocation = loc;
    }

    /* Adds a message to the messageBuffer.
       Also ensures buffer remains a certain size. */
    public void addMessage(String sender, String msg, PlayerMessage.MessageType type)
    {
      synchronized(messageBuffer) {
        messageBuffer.add(new PlayerMessage(sender, msg, type));
        if(messageBuffer.size() > 5) { // Remove old messages.
          messageBuffer.removeFirst();
        }
      }
    }

    /* Prints the contents of the messageBuffer. */
    public void printMessages()
    {
      synchronized(messageBuffer) {
        for(PlayerMessage msg : messageBuffer) {
          long time = System.currentTimeMillis() - msg.timestamp;
          if(msg.type == PlayerMessage.MessageType.SHOUT) {
            System.out.println(ColourPrinter.yellow(msg.sender + " says: " + msg.text + " (" + time/1000 + " seconds ago)"));
          }
          else {
            System.out.println(ColourPrinter.purple(msg.sender + " tells you: " + msg.text + " (" + time/1000 + " seconds ago)"));
          }
        }
      }
    }

    /* Method used mainly by server, to show output to users. */
    public void printOut(String msg)
    {
      System.out.println(msg);
    }
    /* Overloaded version that can print on the same line. */
    public void printOut(String msg, boolean newLine)
    {
      if(newLine) {
        System.out.println(msg);
      }
      else {
        System.out.print(msg);
      }
    }

    /* Clears the user's console. */
    public void clearOutput()
    {
      try {
        final String os = System.getProperty("os.name");
        Process p;

        if (os.contains("Windows")) {
          p = Runtime.getRuntime().exec("cls");
        }
        else {
          p = Runtime.getRuntime().exec("clear");
        }

        // Redirect output from forked exec() process.
        BufferedReader in = new BufferedReader( new InputStreamReader(p.getInputStream()) );
        String line;
        while ((line = in.readLine()) != null) {
          System.out.println(line);
        }
        in.close();
      }
      catch (final Exception e) {
        System.err.println(ColourPrinter.red("Exception in clearOutput():"));
        System.err.println(ColourPrinter.red(e.getMessage()));
      }
    }

    /* Primary method for getting input from user.
       This is used in Client for getting the user's next action.
    */
    public String promptMove()
    throws IOException
    {
      System.out.println(ColourPrinter.blue("What do you do?"));
      System.out.print(ColourPrinter.blue(">"));
      String move = inputStream.readLine();
      return move;
    }

    /* Simple helper method for prompting user for a name. */
    public static String promptUsername()
    {
      try {
        System.out.println(ColourPrinter.blue("Enter a username:"));
        BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));
        String name = inputStream.readLine();
        return name;
      }
      catch(IOException e) {
        System.err.println(ColourPrinter.red("IOException in promptUsername():"));
        System.err.println(ColourPrinter.red(e.getMessage()));
        return "Anonymous";
      }
    }

    /* Helper method to prompt user which MUD to join. */
    public String promptMUD(Vector<MUD> mudList, int maxMUDCount)
    throws RemoteException
    {
      try {
        setStatus(PlayerState.CHOOSINGMUD);
        System.out.println("\nWhich MUD to join?");
        System.out.println("Options are: ");
        for(int i = 0; i < mudList.size(); i++) {
          System.out.println(i + ") Join MUD " + i);
        }
        if(mudList.size() < maxMUDCount) // Only let users create new MUD if there's space on server.
          System.out.println(mudList.size() + ") New MUD");
        System.out.print(ColourPrinter.blue(">"));
        String choice = inputStream.readLine();
        setStatus(PlayerState.NONE);
        return choice;
      }
      catch(IOException e) {
        System.err.println(ColourPrinter.red("IOException in promptMUD():"));
        System.err.println(ColourPrinter.red(e.getMessage()));
        setStatus(PlayerState.NONE);
        return "1";
      }
    }

    /* Helper method to ask user what type of MUD they want to create. */
    public int promptConfig(String[] mudTypes)
    {
      try {
        System.out.println("\nWhich MUD to create?");
        System.out.println("Options are: ");
        for(int i = 0; i < mudTypes.length; i++) {
          System.out.println(i + ") " + mudTypes[i]);
        }
        String choice = inputStream.readLine();
        return Integer.parseInt(choice);
      }
      catch(IOException e) {
        System.err.println(ColourPrinter.red("IOException in promptConfig():"));
        System.err.println(ColourPrinter.red(e.getMessage()));
        return 0;
      }
    }
}
