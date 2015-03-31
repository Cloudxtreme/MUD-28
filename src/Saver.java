package src;

import java.io.FileNotFoundException;
import java.io.File;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayDeque;

public class Saver {

  public Saver()
  {
  }

  /* Saves the player's current state (position, username, inventory) to a file.
     This method can be run inside a thread when the user quits/aborts, or in
     the event the server crashes/aborts. */
  public static void saveState(Player player)
  {
    try {
      File savefile = new File("saves/players/" + player.username() + ".sav");
      FileOutputStream fout = new FileOutputStream(savefile);
      ObjectOutputStream oos = new ObjectOutputStream(fout);
      oos.writeObject(player);
      System.out.println(ColourPrinter.green("Game saved."));
    }
    catch(FileNotFoundException e) {
      System.err.println(ColourPrinter.red("FileNotFoundException in saveState():"));
      System.err.println(ColourPrinter.red(e.getMessage()));
    }
    catch(IOException e) {
      System.err.println(ColourPrinter.red("IOException in saveState():"));
      System.err.println(ColourPrinter.red(e.getMessage()));
    }
  }

  /* Loads the player's current state (position, username, inventory) from a file,
     provided one exists. Creates a new BufferedReader for the user as these
     are not serialized. */
  public static Player loadState(Player player)
  {
    try {
      File savefile = new File("saves/players/" + player.username() + ".sav");
      if(savefile.exists() && !savefile.isDirectory()) {
        FileInputStream fin = new FileInputStream(savefile);
        ObjectInputStream ois = new ObjectInputStream(fin);
        player = (Player) ois.readObject();
        player.inputStream = new BufferedReader(new InputStreamReader(System.in));
        player.messageBuffer = new ArrayDeque<>();
        System.out.println(ColourPrinter.green("Save game loaded."));
        return player;
      }
      else {
        return null;
      }
    }
    catch(FileNotFoundException e) {
      System.err.println(ColourPrinter.red("FileNotFoundException in loadState():"));
      System.err.println(ColourPrinter.red(e.getMessage()));
      return null;
    }
    catch(IOException e) {
      System.err.println(ColourPrinter.red("IOException in loadState():"));
      System.err.println(ColourPrinter.red(e.getMessage()));
      return null;
    }
    catch(ClassNotFoundException e) {
      System.err.println(ColourPrinter.red("ClassNotFoundException in loadState():"));
      System.err.println(ColourPrinter.red(e.getMessage()));
      return null;
    }
  }

  /* Saves the mud's current state (items etc.) to a file.
     This method can be run inside a thread in
     the event the server crashes/aborts. */
  public static void saveState(MUD mud, int mudNo)
  {
    try {
      File savefile = new File("saves/muds/" + "mud_" + mudNo + ".msav");
      FileOutputStream fout = new FileOutputStream(savefile);
      ObjectOutputStream oos = new ObjectOutputStream(fout);
      oos.writeObject(mud);
      System.out.println(ColourPrinter.green("MUD " + mudNo + " saved."));
    }
    catch(FileNotFoundException e) {
      System.err.println(ColourPrinter.red("FileNotFoundException in saveState():"));
      System.err.println(ColourPrinter.red(e.getMessage()));
    }
    catch(IOException e) {
      System.err.println(ColourPrinter.red("IOException in saveState():"));
      System.err.println(ColourPrinter.red(e.getMessage()));
    }
  }

  /* Loads the mud's current state (position, username, inventory) from a file,
     provided one exists. */
  public static MUD loadState(int mudNo)
  {
    try {
      MUD mud;
      File savefile = new File("saves/muds/" + "mud_" + mudNo + ".msav");
      if(savefile.exists() && !savefile.isDirectory()) {
        FileInputStream fin = new FileInputStream(savefile);
        ObjectInputStream ois = new ObjectInputStream(fin);
        mud = (MUD) ois.readObject();
        System.out.println(ColourPrinter.green("MUD save loaded."));
        return mud;
      }
      else {
        return null;
      }
    }
    catch(FileNotFoundException e) {
      System.err.println(ColourPrinter.red("FileNotFoundException in loadState():"));
      System.err.println(ColourPrinter.red(e.getMessage()));
      return null;
    }
    catch(IOException e) {
      System.err.println(ColourPrinter.red("IOException in loadState():"));
      System.err.println(ColourPrinter.red(e.getMessage()));
      return null;
    }
    catch(ClassNotFoundException e) {
      System.err.println(ColourPrinter.red("ClassNotFoundException in loadState():"));
      System.err.println(ColourPrinter.red(e.getMessage()));
      return null;
    }
  }
}
