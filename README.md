# RMI MUD

## To run
- `make mud`
- `rmiregistry <registry port>`
- `java src.MUDServerMainline <registry port> <server port>`

Then for each client:

- `java src.Client <registry host> <registry port> <callback port>`

## How to play
- Start the Client.
- Choose username.
- Choose MUD to join.
- Enter commands ('help' to see list of commands).

## Extra MUD features

- User aborting is handled using a thread that is attached to the Client process using `addShutDownHook()`. This thread is defined as an inner class in `Client.java`.

- Handling of users leaving/joining the server or MUDs, using a `shutdownhook` in `MUDServerMainline` and a server-side method `playerDisconnect()` that decrements counters, removes the player from the list of players, etc.

- Players are able to create any number of new MUDs (limited by `maxMUDCount` server variable).

- Players are able to chat to eachother using either the `whisper` or `shout` commands, and the game stores a buffer of the most recent 5 messages that gets printed out on each refresh along with the rest of the game information.

- User's views are automatically refreshed/udpated when necessary (i.e. when something occurs that changes their world view, or when the options on their menu change due to another players' actions). This is done by using callback methods in the `Player` class, which are called by the `MUDServer`.

- Player's states (their inventory, username, etc.) are automatically saved/serialized to a file when they quit or abort, and restored when they start up their client again (and enter the same username). This is handled by the `Saver` class.

- The server also has a shutdown handler thread that saves state information about it's MUDs and players when the server gets shutdown/aborted. The server can then restore these on startup to allow persistent MUDs. This also means player state information is not lost if the server gets shut down while players are still connected.

- The `MUDServer` by default creates three different MUDs with different configurations (locations, edges, and items).

## Notes

- The program uses ASCII codes to print some messages in different colours. This can easily be disabled by changing the methods in `ColourPrinter.java` so that they do not insert the codes before and after the strings.

- Certain methods or blocks of code are defined with the 'synchronized' keyword to try to ensure thread-safety because of how [how RMI remote method invocation works](http://docs.oracle.com/javase/6/docs/platform/rmi/spec/rmi-arch3.html).

- At some points, such as after you enter a username and the program searches for a save file, `Thread.sleep()` is called with one or two seconds which is the cause of the delay. It's used to make sure the user has time to see the message before the screen is cleared. This also happens when users quit, join MUDs, etc.

- Player's inventories are persistent if they quit and start the game again, however if they leave a MUD and join another, it gets intentionally cleared. This is to prevent players from transferring items between MUDs.

- Save files are stored in the `saves` directory.

## To test server limit parameters

### Maximum players in each MUD

- Change `maxMUDPlayers` on line 29 in `MUDServer.java`.

- To test limit without needing to create lots of clients, change the initial player count for each MUD on line 54 in `MUDServer.java`. (I.e. instead of 0 in `playerCounts.put(dungeonList.get(i), 0);`, enter a higher number).

### Maximum players on server

 - Change `maxServerPlayers` on line 30 in `MUDServer.java`.

### Maximum number of MUDs

- Change `maxMUDCount` on line 31 in `MUDServer.java`.

