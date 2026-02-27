package events;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import akka.actor.ActorRef;
import commands.BasicCommands;
import demo.CommandDemo;
import demo.Loaders_2024_Check;
import structures.GameState;
import structures.basic.Card;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;
import utils.BasicObjectBuilders;
import utils.OrderedCardLoader;
import utils.StaticConfFiles;

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

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		
		// // [SC-01] Backend Initialization: Set game state to initialized and define starting turn
		gameState.gameInitialised = true;
		gameState.turn = 1;
		
		// [SC-01] Board Generation: Iterate through a 9x5 loop to populate the Tile array
		
		for(int i=0;i<9;i++)
		{
			for(int j=0;j<5;j++)
			{
				Tile tile = BasicObjectBuilders.loadTile(i, j);
				gameState.tiles[i][j] = tile;
				BasicCommands.drawTile(out, tile, 0);
				try {Thread.sleep(20);} catch (InterruptedException e) {e.printStackTrace();}
			}
		}

		// 	// ---------------------------------------------------------------------------------------------------------------------

// 	// [SC-03] Avatar Deployment: Place Human and AI units at mirrored board positions
//         // Human avatar placed at [1,2], AI avatar placed at [7,2]
// 		// Set starting visual health and attack for Human and Ai Avatar

// 		// lOAD HUMAN UNIT
		Unit human = BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar, gameState.UnitId++, Unit.class);
		gameState.human_unit = human;
		gameState.placeUnit(gameState.human_unit, gameState.tiles[1][2]); 
		BasicCommands.drawUnit(out, gameState.human_unit, gameState.tiles[1][2]);
		try {Thread.sleep(5);} catch (InterruptedException e) {e.printStackTrace();}

// 		//	Show human's health
		BasicCommands.setUnitHealth(out, gameState.human_unit, 20);
		try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}
		
// 		//	Show human's attack
		BasicCommands.setUnitAttack(out, gameState.human_unit, 2);
		try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}

// 		//	Load ai unit
		Unit ai = BasicObjectBuilders.loadUnit(StaticConfFiles.aiAvatar, gameState.UnitId++, Unit.class);
		gameState.ai_unit = ai;
		gameState.placeUnit(gameState.ai_unit, gameState.tiles[7][2]); 
		BasicCommands.drawUnit(out, gameState.ai_unit, gameState.tiles[7][2]);
		try {Thread.sleep(5);} catch (InterruptedException e) {e.printStackTrace();}
		
		
// 		//	Show ai's health
		BasicCommands.setUnitHealth(out, gameState.ai_unit, 20);
		try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}
		
// 		//	Show ai's attack
		BasicCommands.setUnitAttack(out, gameState.ai_unit, 2);
		try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}
		
		
// // ---------------------------------------------------------------------------------------------------------------------

// // [SC-04] Resource Display: Initialize Health and Mana for both players
//         // Mana follows the (turn + 1) logic; Turn 1 = 2 Mana
 
    //set human_avatar health
        Player humanPlayer = new Player(20, 0);
        gameState.human_player = humanPlayer;
        BasicCommands.setPlayer1Health(out, humanPlayer);
        try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
       
    // set human_avatar mana
            humanPlayer.setMana(gameState.turn+1);
            BasicCommands.setPlayer1Mana(out, humanPlayer);
            try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
           
    // set ai avatar health
        Player aiPlayer = new Player(20, 0);
        gameState.ai_player = aiPlayer;
        BasicCommands.setPlayer2Health(out, aiPlayer);
        try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
       
    // The AI's mana will be refreshed to (Turn + 1) only when its turn starts (same goes for human).
            aiPlayer.setMana(0);
            BasicCommands.setPlayer2Mana(out, aiPlayer);
            try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}

	  // Setting the owner of human and ai unit as these 2 players
            human.setOwner(humanPlayer);
            ai.setOwner(aiPlayer);
           
// // ---------------------------------------------------------------------------------------------------------------------
// // [SC-05] Starting Hand: Draw 3 cards for each player using OrderedCardLoader
//         // Populates the hand positions 1-3 in the UI            
//      // Load human cards (backend plus ui)
 
            int handPosition = 1;
            for (Card card : OrderedCardLoader.getPlayer1Cards(1)) {
                gameState.human_cards.add(card);
                BasicCommands.drawCard(out, card, handPosition, 0);
                try {Thread.sleep(5);} catch (InterruptedException e) {e.printStackTrace();}
               
                handPosition++;
               
                if (handPosition>3) break;
            }
           
//         // Load ai cards (only backend)
            gameState.ai_deck = OrderedCardLoader.getPlayer2Cards(1);
            for (int i = 0; i < 3; i++) {
 
                if (!gameState.ai_deck.isEmpty()) {
                    Card drawn = gameState.ai_deck.remove(0);
                    gameState.ai_hand.add(drawn);
                }
            }
	}

}


