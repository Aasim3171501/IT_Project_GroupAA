package structures;
import java.util.*;
 
import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.Unit;
import structures.basic.UnitAnimationType;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;
import structures.basic.BetterUnit;
import structures.basic.Card;
import structures.basic.EffectAnimation;
import structures.basic.Player;
import structures.basic.Tile;

/**
 * This class can be used to hold information about the on-going game.
 * Its created with the GameActor.
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class GameState {

	
// [SC-01] Global Flags
	public boolean gameInitialised = false;
	public int turn = 0;

// [SC-01] Board Representation
	public Tile[][] tiles = new Tile[9][5];

//	[SC-03] Avatar and Unit Management
	public Unit human_unit = new Unit();
	public Unit ai_unit = new Unit();
	public int UnitId = 0;

	//	connect unit and tile
	public void placeUnit(Unit unit, Tile tile) {

	    // Remove from old tile
	    if (unit.getUnitTile() != null) {
	        unit.getUnitTile().setTileUnit(null);
	    }

	    // Link both sides
	    unit.setUnitTile(tile);
	    tile.setTileUnit(unit);

	    // Sync position
	    unit.setPositionByTile(tile);
	}

	//  // [SC-04] Player Resource Tracking
    public Player human_player = new Player();
    public Player ai_player = new Player();

	//  [SC-05] Card,Hand and deck Management
    public List<Card> human_cards = new ArrayList<>();
    public List<Card> ai_hand = new ArrayList<>();
    // preloaded ai deck
    public List<Card> ai_deck = new ArrayList<>();

	//  [SC-06] Currently selected card (active card)
    public Card ActiveCard = null;
   
	// [SC -06] handposition of the current card
    public int handPosition = -1;

	// [Ref SC -08] reset highlighted tiles
   
    public List<Tile> highlightedTiles = new ArrayList<>();
 
    public void clearHighlights(ActorRef out) {
 
        for (Tile t : highlightedTiles) {
            BasicCommands.drawTile(out, t, 0);
        }
        highlightedTiles.clear();
    }


	//  [SC-07] human_spawns
    public List<Unit> human_units = new ArrayList<>();
 
    //  [SC-07] ai_spawns
    public List<Unit> ai_units = new ArrayList<>();
	
}
