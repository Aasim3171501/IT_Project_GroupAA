package structures.logic;
 
import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Tile;
import structures.basic.Unit;
 
import java.util.ArrayList;
import java.util.List;
 
/**
 * [SC-07] SummoningEngine: Logic for determining valid deployment zones
 * based on the collective adjacency of all friendly units.
 */
public class SummoningEngine {
 
    /**
     * [SC-07] Highlights every empty tile adjacent to ANY friendly unit.
     */
    public static void highlightAllSummonableTiles(ActorRef out, GameState gs) {
        gs.clearHighlights(out); // Ensure we don't stack highlights
        try {Thread.sleep(500);} catch (Exception e) {}
 
        // 1. Create a list of all friendly units to check adjacency
        List<Unit> friendlyUnits = new ArrayList<>();
        if (gs.human_unit != null) friendlyUnits.add(gs.human_unit); // Add Avatar
        friendlyUnits.addAll(gs.human_units); // Add all summoned creatures
 
        // 2. Iterate the 9x5 grid
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 5; j++) {
                Tile targetTile = gs.tiles[i][j];
 
                // [SC-07] Requirement: Tile must be empty to summon
                if (targetTile.getTileUnit() == null) {
                    for (Unit u : friendlyUnits) {
                        // Use MovementEngine utility to check 1-tile radius
                        if (MovementEngine.isAdjacent(i, j, u.getUnitTile().getTilex(), u.getUnitTile().getTiley())) {
                            BasicCommands.drawTile(out, targetTile, 1); // White highlight
                            gs.highlightedTiles.add(targetTile);
                            break; // Once a tile is found valid for one unit, move to next tile
                        }
                    }
                }
            }
        }
    }
}
 