package structures.logic;
 
import structures.GameState;
import structures.basic.Card;
import structures.basic.Player;
import structures.basic.Unit;
 
/**
 * [SC-09, SC-23, SC-24] RuleValidationEngine: The central "Gatekeeper"
 * for validating all game rules before state changes occur.
 */
public class RuleValidationEngine {
 
    // [SC-09] Validates if the player has enough mana for a card
    public static boolean hasEnoughMana(Player player, Card card) {
        return player.getMana() >= card.getManacost();
    }
}