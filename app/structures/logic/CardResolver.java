package structures.logic;
 
import akka.actor.ActorRef;
import structures.GameState;
import structures.basic.Card;
import structures.basic.Tile;
import commands.BasicCommands;
import structures.logic.RuleValidationEngine;
import structures.logic.SpellEngine;
import structures.logic.SummoningEngine;
 
/**
 * [SC-06] CardResolver: This central logic hub interprets the metadata
 * of a clicked card to determine its type and required targeting parameters
 */
public class CardResolver {
 
    public static void resolveCardClick(ActorRef out, GameState gameState, Card selected, int handPosition) {
       
        // Unhighlight previously selected card (if there is one)
        if(gameState.ActiveCard != null)
        {
            // drawCard [0] Highlight
                BasicCommands.drawCard(out, gameState.ActiveCard, gameState.handPosition, 0);
                try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
        }
 
        // [Ref SC-31/32] Visual feedback: Highlight selected card in hand
        // drawCard [1] Highlight
                BasicCommands.drawCard(out, selected, handPosition, 1);
                try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
       
            gameState.ActiveCard = selected;
            gameState.handPosition = handPosition;
       
        //  enough mana to play the card ?
        if (!RuleValidationEngine.hasEnoughMana(gameState.human_player, selected)) {
            BasicCommands.addPlayer1Notification(out, "Not enough mana", 2);
            return;
        }
       
        // [SC-06 and SC-07] Summoning Engine Delegation
        if (selected.getIsCreature()) {
            SummoningEngine.highlightAllSummonableTiles(out, gameState);
        }
       
        // [SC-06 AND SC-16] Spell Engine Delegation
        else {
            SpellEngine.highlightSpellTargets(out, gameState, selected);
        }
    }
}