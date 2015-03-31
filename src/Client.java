/*
  This file is based on the code from the Auction practical.
*/

package src;

import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;

public class Client
{
  /* DisconnectHandler:

     Meant to be run as a thread on shutdown. When the client gets
     shut down (e.g. with CTRL+C or "quit" command) this class gets executed,
     and handles a clean exit by informing the server about the player quitting,
     so the server can act accordingly. Also saves the player's current state.
  */
  private static class DisconnectHandler
  implements Runnable
  {
    private PlayerInterface playerstub;
    private Player player;
    private MUDServerInterface mud;

    private DisconnectHandler(PlayerInterface playerstub, Player player, MUDServerInterface md)
    {
      this.player = player;
      this.playerstub = playerstub;
      this.mud = md;
    }

    public void run()
    {
      try {
        mud.playerDisconnect(playerstub); // Notify server of player disconnect.
        Saver.saveState(player); // Save player state.
      }
      catch(RemoteException e) {
        System.err.println(ColourPrinter.red("Error in DisconnectHandler thread:"));
        System.err.println(ColourPrinter.red(e.getMessage()));
      }
    }
  }

  /* Main loop for player interaction with the MUD Server.
     Continuously prompts for input, then displays result of actions,
     until the user enters "quit" or kills the program.
  */
  public static void gameLoop(Player player, PlayerInterface playerstub, MUDServerInterface mudserv)
  {
    try {
      String command = "";
      while(true) {
        command = player.promptMove();
        if(command.equals("quit")) {
          System.out.println(ColourPrinter.blue("Leaving server..."));
          break;
        }
        // Process command.
        mudserv.makeMove(playerstub, command);
      }
      // "quit" entered.
      System.exit(0);
    }
    catch(IOException e) {
      System.err.println(ColourPrinter.red("Error in gameLoop():"));
      System.err.println(ColourPrinter.red(e.getMessage()));
    }
  }

  /* Fetches a reference to the MUD Server and sets up a thread as a Shutdownhook.
     Registers the player with the server then starts the main game loop with a call to gameLoop().
  */
  public static void main(String args[])
  {
    if (args.length < 3) {
      System.err.println(ColourPrinter.red( "Usage:\njava Client <registryhost> <registryport> <callbackport>" ) );
      return;
    }

    try {
      String hostname = args[0];
      int registryport = Integer.parseInt( args[1] ) ;
      int callbackport = Integer.parseInt( args[2] ) ;
      System.setProperty( "java.security.policy", "mud.policy" ) ;
      System.setSecurityManager( new RMISecurityManager() ) ;

      // Either load existing player, or create new one.
      String username = Player.promptUsername();
      Player player = Saver.loadState(new Player(username));
      if(player != null) {
        System.out.println(ColourPrinter.blue("Save file found. Loading game..."));
        Thread.sleep(2000);
      }
      else {
        System.out.println(ColourPrinter.blue("No save file found. Starting new game..."));
        Thread.sleep(2000);
        player = new Player(username);
      }

      // Obtain reference to MUDServer and register player with RMI.
      System.out.println(ColourPrinter.blue("Connecting to server..."));
      PlayerInterface playerstub = (PlayerInterface)UnicastRemoteObject.exportObject( player, callbackport );
      String regURL = "rmi://" + hostname + ":" + registryport + "/MUD";
      MUDServerInterface mudserv = (MUDServerInterface)Naming.lookup( regURL );

      // Register user with server, start game loop and setup shutdown thread.
      Thread hook = new Thread(new DisconnectHandler(playerstub, player, mudserv));
      Runtime.getRuntime().addShutdownHook(hook);
      if(mudserv.joinServer(playerstub)) {
        gameLoop(player, playerstub, mudserv);
        hook.join();
      }
      else {
        System.out.println(ColourPrinter.red("Failed to connect to server. Press CTRL+C to quit."));
      }
    }
    catch(NotBoundException e) {
      System.err.println(ColourPrinter.red( "Can't find the MUD Server in the registry." ));
    }
    catch (IOException e) {
      System.err.println(ColourPrinter.red( "Failed to register." ));
      System.err.println(ColourPrinter.red(e.getMessage()));
    }
    catch(InterruptedException e) {
      System.err.println(ColourPrinter.red("Thread error:"));
      System.err.println(ColourPrinter.red(e.getMessage()));
    }
  }
}
