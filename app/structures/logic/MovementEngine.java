package structures.logic;
 
import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Tile;
import java.util.ArrayList;
 
/**
 * [SC-11, SC-12, SC-24] MovementEngine: Specialist engine for pathfinding,
 * distance validation, and grid highlighting
 */
public class MovementEngine {
   /**
     * [SC-24] Board Boundary & Adjacency Utility
     */
    public static boolean isAdjacent(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) <= 1 && Math.abs(y1 - y2) <= 1 && !(x1 == x2 && y1 == y2);
    }

}