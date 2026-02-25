package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import demo.CommandDemo;
import structures.GameState;
import structures.basic.Tile;
import utils.BasicObjectBuilders;

/**
 * Indicates that both the core game loop in the browser is starting, meaning
 * that it is ready to recieve commands from the back-end.
 * 
 * { 
 *   messageType = “initalize”
 * }
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class Initalize implements EventProcessor{

	private static final int BOARD_WIDTH = 9;
	private static final int BOARD_HEIGHT = 5;

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		gameState.gameInitalised = true;

		if (gameState.boardTiles == null) {
			Tile[][] tiles = new Tile[BOARD_WIDTH][BOARD_HEIGHT];
			for (int x = 0; x < BOARD_WIDTH; x++) {
				for (int y = 0; y < BOARD_HEIGHT; y++) {
					tiles[x][y] = BasicObjectBuilders.loadTile(x, y);
				}
			}
			gameState.boardTiles = tiles;
		}

		if (out != null) {
			CommandDemo.executeDemo(out); // this executes the command demo, comment out this when implementing your solution
		}
	}

}


