
package src;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MUDServerInterface extends Remote
{

  public boolean joinServer(PlayerInterface p)
  throws RemoteException;

  public boolean joinMUD(int mudNo, PlayerInterface p)
  throws RemoteException;

  public void makeMove(PlayerInterface pl, String dir)
  throws RemoteException;

  public void playerDisconnect(PlayerInterface pl)
  throws RemoteException;
}
