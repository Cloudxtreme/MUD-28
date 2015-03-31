/*
  This file is based on the code from the Factory practical.
*/

package src;

import java.net.InetAddress;

import java.rmi.RMISecurityManager;
import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;
import java.rmi.RemoteException;

public class MUDServerMainline
{
  /* ServerShutdownHandler:

     Meant to be run as a thread on shutdown. When the server gets
     shut down (i.e. with CTRL+C) this class gets executed,
     and handles a clean exit by calling saveState() on all connected players
     to preserve their state.
  */
  private static class ServerShutdownHandler
  implements Runnable
  {
    private Vector<PlayerInterface> players;
    private Vector<MUD> muds;

    private ServerShutdownHandler(Vector<PlayerInterface> players, Vector<MUD> muds)
    {
      this.players = players;
      this.muds = muds;
    }

    public void run()
    {
      try {
        System.out.println("Saving player states...");
        for(PlayerInterface pl : players) {
          pl.saveGame();
        }
        System.out.println("Saving mud states...");
        for(MUD m : muds) {
          Saver.saveState(m, muds.indexOf(m));
        }
      }
      catch(RemoteException e) {
        System.err.println("Error in ServerShutdownHandler thread:");
        System.err.println(e.getMessage());
      }
    }
  }

  /**
   * Generate the MUDServer object and register it with the rmiregistry.
   */
  public static void main(String args[])
  {
    if (args.length < 2) {
      System.err.println( "Usage:\njava solution.ServerMainline <registryport> <serverport>" ) ;
      return;
    }

    try {
      String hostname = (InetAddress.getLocalHost()).getCanonicalHostName();
      int registryport = Integer.parseInt( args[0] );
      int serverport = Integer.parseInt( args[1] );

      System.setProperty( "java.security.policy", "mud.policy" );
      System.setSecurityManager( new RMISecurityManager() );

      String[] edges = {"mud_configs/mud_1/mymud.edg", "mud_configs/mud_2/mymud.edg", "mud_configs/mud_3/mymud.edg"};
      String[] messages = {"mud_configs/mud_1/mymud.msg", "mud_configs/mud_2/mymud.msg", "mud_configs/mud_3/mymud.msg"};
      String[] things = {"mud_configs/mud_1/mymud.thg", "mud_configs/mud_2/mymud.thg", "mud_configs/mud_3/mymud.thg"};
      String[] mudTypes = {"Cliche Fantasy", "Mysterious Forest", "Tropical Island"};

      // Register MUD Server with RMI, and set up the shutdown thread.
      MUDServer serv = new MUDServer( serverport, edges, messages, things, mudTypes );
      MUDServerInterface stub = (MUDServerInterface)UnicastRemoteObject.exportObject( serv, serverport );
      Naming.rebind( "rmi://" + hostname + ":" + registryport + "/MUD", stub );
      Thread hook = new Thread(new ServerShutdownHandler(serv.playerList, serv.dungeonList));
      Runtime.getRuntime().addShutdownHook(hook);
    }
    catch(java.net.UnknownHostException e) {
      System.err.println( e.getMessage() );
    }
    catch (java.io.IOException e) {
      System.err.println( e.getMessage() );
    }
  }
}
