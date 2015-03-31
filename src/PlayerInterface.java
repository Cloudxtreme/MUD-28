
package src;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;
import java.io.IOException;

public interface PlayerInterface extends Remote
{
  public Vector<String> inventory()
  throws RemoteException;

  public String username()
  throws RemoteException;

  public void saveGame()
  throws RemoteException;

  public void addInventory(String item)
  throws RemoteException;

  public void removeInventory(String item)
  throws RemoteException;

  public void clearInventory()
  throws RemoteException;

  public String curLocation()
  throws RemoteException;

  public PlayerState status()
  throws RemoteException;

  public void setStatus(PlayerState _status)
  throws RemoteException;

  public void setLocation(String loc)
  throws RemoteException;

  public void printOut(String msg)
  throws RemoteException;
  public void printOut(String msg, boolean newLine)
  throws RemoteException;

  public void clearOutput()
  throws RemoteException;

  public void addMessage(String msg, String sender, PlayerMessage.MessageType type)
  throws RemoteException;

  public void printMessages()
  throws RemoteException;

  public String promptMUD(Vector<MUD> mudList, int maxMUDCount)
  throws RemoteException;

  public int promptConfig(String[] mudTypes)
  throws RemoteException;
}
