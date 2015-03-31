package src;

import java.util.Vector;
import java.util.HashMap;
import java.util.Arrays;
import java.rmi.RemoteException;
import java.io.IOException;
import java.io.EOFException;

public class MUDServer implements MUDServerInterface
{
  public static final int SHORT_DELAY = 1000; // times used by Thread.sleep.
  public static final int LONG_DELAY = 2000;
  public static final int MAX_SERVER_PLAYERS = 15; // Max players in server.
  public static final int MAX_MUD_PLAYERS = 5; // Max players per MUD.
  public static final int MAX_MUD_COUNT = 4; // Max no. of MUD's allowed on server.

  public Vector<MUD> dungeonList; // MUD's available on this server.
  public Vector<PlayerInterface> playerList; // Players connected to server.
  public HashMap<MUD, Integer> playerCounts; // No. of players in each MUD.
  public String[] edgesfiles, messagesfiles, thingsfiles, mudTypes; // Different MUD configurations.

  /* Initialise server, and create some default MUDs for users to join. */
  public MUDServer( int serverport,
                    String[] _edgesfiles, String[] _messagesfiles, String[] _thingsfiles, String[] _mudTypes) {
      System.out.println("Initialising MUD Server...");
      dungeonList = new Vector<>();
      playerList = new Vector<>();
      playerCounts = new HashMap<>();
      edgesfiles = _edgesfiles;
      messagesfiles = _messagesfiles;
      thingsfiles = _thingsfiles;
      mudTypes = _mudTypes;

      if(!(edgesfiles.length == messagesfiles.length && messagesfiles.length == thingsfiles.length)) {
        System.out.println("Please use an equal number of edge(.edg), message(.msg) and thing(.thg) files.");
        System.exit(0);
      }

      // Create default MUD's for users to join. Create one of every type.
      for(int i = 0; i < mudTypes.length; i++) {
        System.out.println("\n### Creating MUD " + i + " ###");
        MUD mud = Saver.loadState(i);
        if(mud != null) {
          System.out.println("Save file found for MUD " + i);
          dungeonList.add(mud);
        }
        else {
          System.out.println("No save file found for MUD " + i + ". Creating new MUD...");
          dungeonList.add(new MUD(edgesfiles[i], messagesfiles[i], thingsfiles[i]));
        }
        playerCounts.put(dungeonList.get(i), 0);
      }
      System.out.println("\nServer running...");
  }

  /* Register player with the server, and ask them to choose a MUD to join. */
  public boolean joinServer(PlayerInterface pl)
  throws RemoteException
  {
    try {
      if(playerList.size() >= MAX_SERVER_PLAYERS) {
        pl.printOut(ColourPrinter.red("Server is full! Please try again later."));
        return false;
      }

      // Check if unique username, and prompt for which MUD to join.
      if(checkUnique(pl.username())) {
        System.out.println("Player " + pl.username() + " connecting.");
        playerList.add(pl);
        System.out.println(playerList.size() + " players on server.");
        pl.clearOutput();
        pl.printOut(ColourPrinter.green("Connection successful!"));
        int choice = Integer.parseInt(pl.promptMUD(dungeonList, MAX_MUD_COUNT)); // promptMUD() sets player's status to CHOOSINGMUD.

        if(choice == dungeonList.size()) { // User chose to create new MUD.
          if(choice >= MAX_MUD_COUNT) {
            pl.printOut(ColourPrinter.red("Server maximum MUD count reached. Please choose one of the existing MUD's."));
            joinServer(pl);
            return false;
          }
          // Create and join a new MUD.
          pl.printOut(ColourPrinter.blue("Creating new MUD."));
          int mudConfig = pl.promptConfig(mudTypes);
          dungeonList.add(new MUD(edgesfiles[mudConfig], messagesfiles[mudConfig], thingsfiles[mudConfig]));
          playerCounts.put(dungeonList.get(choice), 0);
          refreshPrompts(pl);
          joinMUD(choice, pl);
          return true;
        }
        // Join default MUD if invalid choice.
        else if(choice < 0 || choice > dungeonList.size() || playerCounts.get(dungeonList.get(choice)) >= MAX_MUD_PLAYERS) {
          int free = firstFreeMUD();
          if(free >= 0) {
            if(playerCounts.get(dungeonList.get(choice)) >= MAX_MUD_PLAYERS) {
              pl.printOut(ColourPrinter.red("MUD " + choice + " is full. Joining first free MUD (" + free + ")"));
            }
            else {
              pl.printOut(ColourPrinter.red("Invalid choice. Joining first free MUD (" + free + ")"));
            }
            Thread.sleep(LONG_DELAY);
            joinMUD(free, pl);
            return true;
          }
          else {
            pl.printOut(ColourPrinter.red("All MUDs on server are full, please try again later."));
            return false;
          }
        }
        // Join chosen MUD.
        else {
          joinMUD(choice, pl);
          return true;
        }
      }
      else {
        pl.printOut(ColourPrinter.red("Sorry, a player with that username is already connected."));
        return false;
      }
    }
    catch(RemoteException e) {
      System.err.println(ColourPrinter.red("RemoteException in joinServer():"));
      System.err.println(ColourPrinter.red(e.getMessage()));
      return false;
    }
    catch(NumberFormatException e) { // When user enters a non-integer as a choice.
      int free = firstFreeMUD();
      if(free >= 0) {
        pl.printOut(ColourPrinter.red("Invalid choice. Joining first free MUD (" + free + ")"));
        joinMUD(0, pl);
        return true;
      }
      else {
        pl.printOut(ColourPrinter.red("All MUDs on this server are full, please try again later."));
        return false;
      }
    }
    catch(InterruptedException e) {
      System.err.println(ColourPrinter.red("InterruptedException in joinServer():"));
      System.err.println(ColourPrinter.red(e.getMessage()));
      return false;
    }
  }

  /* Add a user to a MUD. */
  public boolean joinMUD(int mudNo, PlayerInterface pl)
  throws RemoteException
  {
    // Join the mud with ID mudNo.
    MUD m = dungeonList.get(mudNo);
    System.out.println("Player " + pl.username() + " is joining MUD " + mudNo);
    pl.setStatus(PlayerState.PLAYING);
    pl.clearOutput();
    int count = playerCounts.get(m);
    playerCounts.put(m, count+1);
    m.addThing(m.startLocation(), pl.username()); // add to list of players.
    pl.setLocation(m.startLocation()); // set player's location to mud's start location.
    String info = m.locationInfo(m.startLocation());
    pl.printOut(ColourPrinter.green("You have joined MUD " + mudNo));
    pl.printOut(ColourPrinter.blue(info.replace(pl.username(), ""))); // print info about that location.
    refreshViews(pl, false, false, false);
    return true;
  }

  /* Process a user's command, and print result to user's output. */
  public void makeMove(PlayerInterface pl, String action)
  {
    try {
      String[] command = action.split("\\s+");
      int mudNo;
      MUD mud;
      String item;
      String message;
      switch(command[0]) {
        case "move":
          String dir = command[1];
          mud = dungeonList.get(findMUD(pl.username()));
          if(!mud.locationInfo(pl.curLocation()).contains(dir)) {
            pl.printOut(ColourPrinter.red("There is no path in that direction!"));
            return;
          }
          System.out.println(pl.username() + " is moving " + dir);
          // Move in MUD.
          String newLoc = mud.moveThing(pl.curLocation(), dir, pl.username());
          refreshViews(pl, false, false, false); // update views of players in previous location.
          pl.setLocation(newLoc);
          refreshViews(pl, true, false, true); // update views of players in new location.
          return;

        case "pick":
          item = command[1];
          mudNo = findMUD(pl.username());
          boolean picked = false;
          // check if item is visibile.
          synchronized(dungeonList.get(mudNo)) {
            if(dungeonList.get(mudNo).itemExists(pl.curLocation(), playerList, item)) {
              System.out.println(pl.username() + " is picking up " + item);
              dungeonList.get(mudNo).delThing(pl.curLocation(), item);
              picked = true;
            }
            else {
              pl.printOut(ColourPrinter.red("There is no " + item + " here."));
            }
          }
          if(picked) {
            pl.addInventory(item);
            pl.printOut(ColourPrinter.green(item + " added to inventory."));
            Thread.sleep(SHORT_DELAY);
            refreshViews(pl, true, false, true);
          }
          return;

        case "drop":
          item = command[1];
          mudNo = findMUD(pl.username());
          // check if player is carrying item.
          if(pl.inventory().contains(item)) {
            System.out.println(pl.username() + " is dropping " + item);
            dungeonList.get(mudNo).addThing(pl.curLocation(), item);
            pl.removeInventory(item);
            pl.printOut(ColourPrinter.green(item + " removed from inventory."));
            Thread.sleep(SHORT_DELAY);
            refreshViews(pl, true, false, true);
          }
          else {
            pl.printOut(ColourPrinter.red("You're not carrying " + item + "!"));
          }
          return;

        case "look":
          mudNo = findMUD(pl.username());
          System.out.println(pl.username() + " is looking around " + pl.curLocation());
          String info = dungeonList.get(mudNo).locationInfo(pl.curLocation());
          info = info.replace(pl.username(), "");
          pl.clearOutput();
          if(info.equals(""))
            pl.printOut("You see nothing of interest here.");
          else
            pl.printOut(info.replace(pl.username(), ""));
          return;

        case "inventory":
          System.out.println(pl.username() + " is checking their inventory");
          pl.clearOutput();
          pl.printOut(ColourPrinter.blue(dungeonList.get(findMUD(pl.username())).locationInfo(pl.curLocation()).replace(pl.username(), "")));
          pl.printOut(ColourPrinter.blue("Your inventory:"));
          for(String thing : pl.inventory()) {
            pl.printOut(ColourPrinter.blue("-" + thing));
          }
          return;

        case "shout":
          message = "";
          for(String s : Arrays.copyOfRange(command, 1, command.length))
            message += " " + s;
          System.out.println(pl.username() + " is shouting at " + pl.curLocation());
          broadcastPlayerMessage(pl, message);
          return;

        case "whisper":
          message = "";
          for(String s : Arrays.copyOfRange(command, 2, command.length))
            message += " " + s;
          System.out.println(pl.username() + " is whispering to " + command[1]);
          sendPlayerMessage(pl.username(), command[1], message);
          return;

        case "leave":
          mudNo = findMUD(pl.username());
          if(mudNo >= 0) { // Player may not be in any MUD.
            int count;
            // Leave current MUD.
            System.out.println(pl.username() + " is leaving MUD " + mudNo);
            pl.setStatus(PlayerState.NONE);
            mud = dungeonList.get(mudNo);
            synchronized(playerCounts.get(mud)) { // update MUD player count.
              count = playerCounts.get(mud);
              playerCounts.put(mud, count-1);
            }
            refreshViews(pl, false, true, false);
            mud.delThing(pl.curLocation(), pl.username());
            pl.setLocation("");
            pl.clearInventory();

            // Join another MUD.
            mudNo = Integer.parseInt(pl.promptMUD(dungeonList, MAX_MUD_COUNT));
            System.out.println(pl.username() + " is joining MUD " + mudNo);
            mud = dungeonList.get(mudNo);
            synchronized(playerCounts.get(mud)) {
              count = playerCounts.get(mud);
              playerCounts.put(mud, count+1);
            }
            mud.addThing(mud.startLocation(), pl.username());
            pl.setLocation(mud.startLocation());
            pl.addInventory("map");
            pl.setStatus(PlayerState.PLAYING);
            pl.printOut(ColourPrinter.blue(mud.locationInfo(pl.curLocation()).replace(pl.username(), "")));
            refreshViews(pl, true, false, true);
          }
          return;

        case "quit":
          System.out.println(pl.username() + " is leaving the server.");
          playerDisconnect(pl);
          pl.printOut(ColourPrinter.green("Left server."));
          return;

        default:
          pl.printOut(ColourPrinter.red("Please enter command:\n\t-help\n\t-move <north, south, east, west>\n\t-pick <item>\n\t-drop\n\t-look\n\t-inventory\n\t-shout <message>\n\t-whisper <user> <message>\n\t-leave"));
          return;
      }
    }
    catch(RemoteException e) {
      System.err.println(ColourPrinter.red("RemoteException in makeMove():"));
      System.err.println(ColourPrinter.red(e.getMessage()));
    }
    catch(InterruptedException e) {
      System.err.println(ColourPrinter.red("InterruptedException in makeMove():"));
      System.err.println(ColourPrinter.red(e.getMessage()));
    }
  }

  /* When a player quits or aborts (CTRL+C or "quit command")
     this method removes the user from the MUD, and the server player list,
     and decrements the relevant counters.
   */
  public void playerDisconnect(PlayerInterface pl)
  throws RemoteException
  {
    System.out.println("Player " + pl.username() + " disconnecting.");
    int mudNo = findMUD(pl.username());
    if(mudNo >= 0) { // Player may not be in any MUD.
      MUD mud = dungeonList.get(mudNo);
      synchronized(playerCounts.get(mud)) {
        int count = playerCounts.get(mud);
        playerCounts.put(mud, count-1);
      }
      refreshViews(pl, false, true, false);
      mud.delThing(pl.curLocation(), pl.username());
    }
    playerList.remove(pl);
    pl.setStatus(PlayerState.NONE);
    System.out.println(playerList.size() + " players on server.");
  }


  /* PRIVATE METHODS */

  /* Check if a username is unique, i.e. no connected player has
     that username already. This works because the connecting player
     will not be registered on the server yet, and so won't be a candidate
     for getPlayerByName(). */
  private boolean checkUnique(String name)
  {
    if(getPlayerByName(name) != null) {
      return false;
    }
    else {
      return true;
    }
  }

  /* Find which MUD a given user is in (-1 if none). */
  private int findMUD(String name)
  {
    for(MUD m : dungeonList) {
      if(m.containsItem(name))
        return dungeonList.indexOf(m);
    }
    return -1;
  }

  /* Returns index in dungeonList of first MUD with free space left,
     or -1 if there are none.
  */
  private int firstFreeMUD()
  {
    for(int i = 0; i<dungeonList.size(); i++) {
      if(playerCounts.get(dungeonList.get(i)) < MAX_MUD_PLAYERS)
        return i;
    }
    return -1;
  }

  /* Checks if two players are in the same MUD, and the same location within that MUD. */
  private boolean sameLocation(PlayerInterface playerOne, PlayerInterface playerTwo)
  {
    try {
      return ((findMUD(playerOne.username()) == findMUD(playerTwo.username())) && playerOne.curLocation().equals(playerTwo.curLocation()));
    }
    catch(RemoteException e) {
      System.err.println(ColourPrinter.red("RemoteException in sameLocation():"));
      System.err.println(ColourPrinter.red(e.getMessage()));
      return false;
    }
  }

  /* Sends a player's message to all other players in the same location within
     that MUD. */
  private void broadcastPlayerMessage(PlayerInterface player, String message)
  {
    try {
      for(PlayerInterface pl : playerList) {
        if(pl.curLocation().equals(player.curLocation())) {
          pl.addMessage(player.username(), message, PlayerMessage.MessageType.SHOUT);
        }
      }
      refreshViews(player, true, false, true);
    }
    catch(RemoteException e) {
      System.err.println(ColourPrinter.red("RemoteException in broadcastPlayerMessage():"));
      System.err.println(ColourPrinter.red(e.getMessage()));
    }
  }

  /* Sends a message to one specific player (anywhere within the same MUD). */
  private void sendPlayerMessage(String sending_player, String receiver, String message)
  {
    PlayerInterface target = getPlayerByName(receiver);
    PlayerInterface sender = getPlayerByName(sending_player);
    try {
      if(target != null) {
          int mudOne = findMUD(sender.username());
          int mudTwo = findMUD(target.username());
          if(mudOne == mudTwo) {
            target.addMessage(sending_player, message, PlayerMessage.MessageType.WHISPER);
            refreshViews(target, true, false, false);
          }
          else
            sender.printOut(ColourPrinter.red("That player isn't in your MUD!"));
      }
      else {
        sender.printOut(ColourPrinter.red("That username does not exist!"));
      }
    }
    catch(RemoteException e) {
      System.err.println(ColourPrinter.red("RemoteException in sendPlayerMessage():"));
      System.err.println(ColourPrinter.red(e.getMessage()));
    }
  }

  /* Returns the PlayerInterface of user with given name. */
  private PlayerInterface getPlayerByName(String name)
  {
    try {
      for(PlayerInterface pl : playerList) {
        if(pl.username().equals(name))
          return pl;
      }
      return null;
    }
    catch(RemoteException e) {
      System.err.println(ColourPrinter.red("RemoteException in sendPlayerMessage():"));
      System.err.println(ColourPrinter.red(e.getMessage()));
      return null;
    }
  }

  /* Refreshes players' views of an area, when a particular player in
     the same area makes a move (such as moving, picking items, or joining/leaving
     the MUD).

     If includingCurrent is true, the current player's view
     is also refreshed (the player carrying out the action).

     If leaveAction is true, then currentPlayer is assumed to be leaving the server/MUD
     and so their username is hidden in the output from locationInfo(). This is so
     sameLocation() still returns true, as they haven't actually been removed from the MUD yet.
     This only works if refreshViews() is called BEFORE the player is removed from the MUD.

     The noPrompt flag disables the "what do you do" prompt from being printed. */
  private void refreshViews(PlayerInterface currentPlayer, boolean includingCurrent, boolean leaveAction, boolean noPrompt)
  {
    try {
      for(PlayerInterface player : playerList) {
        if(!currentPlayer.equals(player) && sameLocation(currentPlayer, player) && player.status() == PlayerState.PLAYING) {
          player.clearOutput();
          if(leaveAction) {
            player.printOut(ColourPrinter.blue(dungeonList.get(findMUD(player.username())).locationInfo(player.curLocation()).replace(player.username(), "").replace(currentPlayer.username(), "")));
          }
          else {
            player.printOut(ColourPrinter.blue(dungeonList.get(findMUD(player.username())).locationInfo(player.curLocation()).replace(player.username(), "")));
          }
          player.printMessages();
          player.printOut(ColourPrinter.blue("What do you do?"));
          player.printOut(ColourPrinter.blue(">"), false);
        }
      }
      if(includingCurrent) {
        currentPlayer.clearOutput();
        currentPlayer.printOut(ColourPrinter.blue(dungeonList.get(findMUD(currentPlayer.username())).locationInfo(currentPlayer.curLocation()).replace(currentPlayer.username(), "")));
        currentPlayer.printMessages();
        if(!noPrompt) {
          currentPlayer.printOut(ColourPrinter.blue("What do you do?"));
          currentPlayer.printOut(ColourPrinter.blue(">"), false);
        }
      }
    }
    catch(RemoteException e) {
      System.err.println(ColourPrinter.red("RemoteException in refreshViews():"));
      System.err.println(ColourPrinter.red(e.getMessage()));
    }
  }

  /* Refreshes the MUD-selection prompt. This ensures the players'
     list of options remains up-to-date (e.g. when another player
     creates a new MUD, the list of available MUD's will update
     for any other player stil within that screen). */
  private void refreshPrompts(PlayerInterface currentPlayer)
  {
    try {
      for(PlayerInterface player : playerList) {
        if(!currentPlayer.equals(player) && player.status() == PlayerState.CHOOSINGMUD) {
          player.clearOutput();
          player.printOut(ColourPrinter.blue("\nWhich MUD to join?"));
          player.printOut(ColourPrinter.blue("Options are: "));
          for(int i = 0; i < dungeonList.size(); i++) {
            player.printOut(ColourPrinter.blue(i + ") Join MUD " + i));
          }
          if(dungeonList.size() < MAX_MUD_COUNT) // Only let users create new MUD if there's space on server.
            player.printOut(ColourPrinter.blue(dungeonList.size() + ") New MUD"));
        }
      }
      return;
    }
    catch(RemoteException e) {
      System.err.println(ColourPrinter.red("RemoteException in refreshViews():"));
      System.err.println(ColourPrinter.red(e.getMessage()));
    }
  }
}
