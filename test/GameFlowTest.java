import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import commands.BasicCommands;
import commands.CheckMessageIsNotNullOnTell;
import game.GameService;
import game.GameUnit;
import structures.GameState;

public class GameFlowTest {

	private GameState gameState;

	@Before
	public void setUp() {
		BasicCommands.altTell = new CheckMessageIsNotNullOnTell();
		gameState = new GameState();
		GameService.initializeGame(null, gameState);
	}

	@Test
	public void summonAndArtifactChangeBoardState() {
		GameService.handleCardClicked(null, gameState, 1);
		GameService.handleTileClicked(null, gameState, 2, 3);

		GameUnit summoned = gameState.getUnitAt(2, 3);
		assertNotNull(summoned);
		assertEquals("Bad Omen", summoned.getName());
		assertEquals(3, gameState.getUnits().size());
		assertEquals(2, gameState.getPlayer1().getHand().size());

		GameService.handleCardClicked(null, gameState, 1);
		assertEquals(1, gameState.getPlayer1().getHand().size());
		assertEquals(3, gameState.getPlayer1().getAvatar().getAttackValue());
	}

	@Test
	public void endingTurnRunsAiAndReturnsToHuman() {
		GameService.handleEndTurn(null, gameState);

		assertFalse(gameState.isGameOver());
		assertEquals(1, gameState.getCurrentPlayerId());
		assertEquals(3, gameState.getPlayer1().getCurrentMana());
	}
}
