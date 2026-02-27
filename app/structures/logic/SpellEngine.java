package structures.logic;
 
import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;
import structures.basic.Tile;
import structures.basic.Unit;
 
/**
 * [SC-16, SC-17] SpellEngine: Specialized logic for Human Player 1 spells:
 * Horn of the Forsaken, Wraithling Swarm, and Dark Terminus.
 */
public class SpellEngine {
 
    /**
     * [SC-16] Highlights valid targets based on the specific Player 1 spell.
     */
    public static void highlightSpellTargets(ActorRef out, GameState gs, Card spell) {
        gs.clearHighlights(out);
        String name = spell.getCardname();
 
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 5; j++) {
                Tile t = gs.tiles[i][j];
                Unit targetUnit = t.getTileUnit();
 
                switch (name) {
                    case "Horn of the Forsaken":
                        // Target: Your Avatar (to equip the artifact)
                        if (targetUnit == gs.human_unit) {
                            highlightTile(out, gs, t, 1); // White highlight
                        }
                        break;
 
                    case "Wraithling Swarm":
                        // Target: Any empty space on the board
                        if (targetUnit == null) {
                            highlightTile(out, gs, t, 1);
                        }
                        break;
 
                    case "Dark Terminus":
                        // Target: Any non-avatar enemy unit
                        if (targetUnit != null && targetUnit.getOwner() != gs.human_player && targetUnit != gs.ai_unit) {
                            highlightTile(out, gs, t, 2); // Red highlight
                        }
                        break;
                }
            }
        }
    }
 
    private static void highlightTile(ActorRef out, GameState gs, Tile t, int mode) {
        BasicCommands.drawTile(out, t, mode);
        gs.highlightedTiles.add(t);
    }
}