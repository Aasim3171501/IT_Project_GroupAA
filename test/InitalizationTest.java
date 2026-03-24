import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import commands.BasicCommands;
import commands.CheckMessageIsNotNullOnTell;
import game.GameService;
import play.libs.Json;
import structures.GameState;
import structures.basic.Tile;
import utils.BasicObjectBuilders;

/**
 * This is an example of a JUnit test. In this case, we want to be able to test the logic
 * of our system without needing to actually start the web server. We do this by overriding
 * the altTell method in BasicCommands, which means whenever a command would normally be sent
 * to the front-end it is instead discarded. We can manually simulate messages coming from the
 * front-end by calling the processEvent method on the appropriate event processor.
 * @author Richard
 *
 */
public class InitalizationTest {

	/**
	 * This test simply checks that a boolean vairable is set in GameState when we call the
	 * initalize method for illustration.
	 */
	@Test
	public void checkInitalized() {
		
		// First override the alt tell variable so we can issue commands without a running front-end
		CheckMessageIsNotNullOnTell altTell = new CheckMessageIsNotNullOnTell(); // create an alternative tell
		BasicCommands.altTell = altTell; // specify that the alternative tell should be used
		
		// As we are not starting the front-end, we have no GameActor, so lets manually create
		// the components we want to test
		GameState gameState = new GameState(); // create state storage
		
		assertFalse(gameState.gameInitalised); // check we have not initalized
		
		// lets simulate recieveing an initalize message
		ObjectNode eventMessage = Json.newObject(); // create a dummy message
		GameService.initializeGame(null, gameState); // send it to the game service
		
		assertTrue(gameState.gameInitalised); // check that this updated the game state
		assertEquals(45, gameState.getTiles().size());
		assertEquals(3, gameState.getPlayer1().getHand().size());
		assertEquals(3, gameState.getPlayer2().getHand().size());
		assertNotNull(gameState.getUnitAt(1, 3));
		assertNotNull(gameState.getUnitAt(9, 3));
		
		// lets also check that running commands don't actually do anything, since we have no front-end
		Tile tile = BasicObjectBuilders.loadTile(3, 2); // create a tile
		BasicCommands.drawTile(null, tile, 0); // draw tile, but will use altTell, so nothing should happen
		
	}
	
}
