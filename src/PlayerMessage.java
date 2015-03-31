package src;

/* Represents a player-to-player chat message.
   The timestamp is used to clear old messages. */
public class PlayerMessage
{
  public static enum MessageType {
    SHOUT, WHISPER
  }

  public String sender;
  public String text;
  public MessageType type;
  public long timestamp;

  public PlayerMessage(String sendername, String msg, MessageType mtype)
  {
    sender = sendername;
    text = msg;
    type = mtype;
    timestamp = System.currentTimeMillis();
  }
}
