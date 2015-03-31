package src;

// The player's current state (what they are doing within the server).
//          NONE: they are connected, but haven't joined any MUD.
//   CHOOSINGMUD: they are currently choosing which MUD to join.
//       PLAYING: they are currently in a MUD and playing.
public enum PlayerState {
  NONE, CHOOSINGMUD, PLAYING
}
