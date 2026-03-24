package game;

import java.util.ArrayList;
import java.util.List;

import structures.GameState;
import structures.basic.Card;
import structures.basic.Player;
import structures.basic.Tile;

/**
 * Runtime player state.
 */
public class GamePlayer {

	private static final int MAX_HAND_SIZE = 6;

	private final int id;
	private final String displayName;
	private final boolean ai;
	private final List<Card> deck;
	private final List<Card> hand;
	private final Player uiPlayer;

	private GameUnit avatar;
	private int maxMana;
	private int turnsStarted;
	private int artifactAttackBonus;
	private int artifactCharges;

	public GamePlayer(int id, String displayName, boolean ai, List<Card> deck) {
		this.id = id;
		this.displayName = displayName;
		this.ai = ai;
		this.deck = new ArrayList<Card>(deck);
		this.hand = new ArrayList<Card>();
		this.uiPlayer = new Player(20, 2);
		this.maxMana = 2;
		this.turnsStarted = 0;
		this.artifactAttackBonus = 0;
		this.artifactCharges = 0;
	}

	public int getId() {
		return id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public boolean isAi() {
		return ai;
	}

	public List<Card> getDeck() {
		return deck;
	}

	public List<Card> getHand() {
		return hand;
	}

	public Player getUiPlayer() {
		return uiPlayer;
	}

	public GameUnit getAvatar() {
		return avatar;
	}

	public void setAvatar(GameUnit avatar) {
		this.avatar = avatar;
		syncUiPlayer();
	}

	public Tile getAvatarTile(GameState gameState) {
		if (avatar == null) {
			return null;
		}
		return gameState.getTile(avatar.getBoardPosition().getX(), avatar.getBoardPosition().getY());
	}

	public int getCurrentMana() {
		return uiPlayer.getMana();
	}

	public int getMaxMana() {
		return maxMana;
	}

	public boolean canAfford(Card card) {
		return card != null && getCurrentMana() >= card.getManacost();
	}

	public void spendMana(int amount) {
		uiPlayer.setMana(Math.max(0, uiPlayer.getMana() - amount));
	}

	public void startTurn() {
		if (turnsStarted > 0) {
			maxMana = Math.min(9, maxMana + 1);
		}
		uiPlayer.setMana(maxMana);
		turnsStarted++;
		syncUiPlayer();
	}

	public Card getCardAtPosition(int position) {
		if (position < 1 || position > hand.size()) {
			return null;
		}
		return hand.get(position - 1);
	}

	public Card removeCardAtPosition(int position) {
		if (position < 1 || position > hand.size()) {
			return null;
		}
		return hand.remove(position - 1);
	}

	public Card drawCard() {
		if (deck.isEmpty()) {
			return null;
		}
		Card drawn = deck.remove(0);
		if (hand.size() < MAX_HAND_SIZE) {
			hand.add(drawn);
		}
		return drawn;
	}

	public void syncUiPlayer() {
		if (avatar != null) {
			uiPlayer.setHealth(avatar.getHealthValue());
		}
	}

	public void equipArtifact(int attackBonus, int charges) {
		this.artifactAttackBonus = attackBonus;
		this.artifactCharges = charges;
	}

	public boolean hasArtifact() {
		return artifactCharges > 0 && artifactAttackBonus > 0;
	}

	public int getArtifactAttackBonus() {
		return artifactAttackBonus;
	}

	public int getArtifactCharges() {
		return artifactCharges;
	}

	public void consumeArtifactCharge() {
		if (artifactCharges > 0) {
			artifactCharges--;
		}
		if (artifactCharges <= 0) {
			artifactAttackBonus = 0;
			artifactCharges = 0;
		}
	}
}
