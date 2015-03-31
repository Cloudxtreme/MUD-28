/***********************************************************************
 * cs3524.solutions.mud.Edge
 ***********************************************************************/

package src;

import java.io.Serializable;

// Represents an path in the MUD (an edge in a graph).
class Edge implements Serializable
{
    public Vertex dest;   // Your destination if you walk down this path
    public String view;   // What you see if you look down this path

    public Edge( Vertex d, String v )
    {
      dest = d;
      view = v;
    }
}
