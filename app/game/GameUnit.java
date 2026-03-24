package game;

import java.util.EnumSet;

import structures.basic.Position;
import structures.basic.Tile;
import structures.basic.Unit;

/**
 * Runtime unit state used by game logic.
 */
public class GameUnit extends Unit {

	private final String name;
	private final int ownerId;
	private final EnumSet<UnitAbility> abilities;
	private final boolean avatar;

	private int attack;
	private int health;
	private int maxHealth;

	private boolean canMove;
	private int remainingAttacks;
	private boolean summonedThisTurn;
	private boolean stunned;
	private boolean counterAvailable;

	public GameUnit(Unit base, String name, int ownerId, int attack, int health, boolean avatar,
			EnumSet<UnitAbility> abilities) {
		super(base.getId(), base.getAnimation(), copyPosition(base.getPosition()), base.getAnimations(), base.getCorrection());
		this.name = name;
		this.ownerId = ownerId;
		this.attack = attack;
		this.health = health;
		this.maxHealth = health;
		this.avatar = avatar;
		this.abilities = abilities.clone();
		this.canMove = false;
		this.remainingAttacks = 0;
		this.summonedThisTurn = false;
		this.stunned = false;
		this.counterAvailable = true;
	}

	private static Position copyPosition(Position position) {
		return new Position(position.getXpos(), position.getYpos(), position.getTilex(), position.getTiley());
	}

	public String getName() {
		return name;
	}

	public int getOwnerId() {
		return ownerId;
	}

	public int getAttackValue() {
		return attack;
	}

	public void setAttackValue(int attack) {
		this.attack = attack;
	}

	public int getHealthValue() {
		return health;
	}

	public void setHealthValue(int health) {
		this.health = health;
	}

	public int getMaxHealth() {
		return maxHealth;
	}

	public void setMaxHealth(int maxHealth) {
		this.maxHealth = maxHealth;
	}

	public boolean isAvatar() {
		return avatar;
	}

	public boolean hasAbility(UnitAbility ability) {
		return abilities.contains(ability);
	}

	public EnumSet<UnitAbility> getAbilities() {
		return abilities.clone();
	}

	public BoardPosition getBoardPosition() {
		return new BoardPosition(getPosition().getTilex(), getPosition().getTiley());
	}

	public void moveTo(Tile tile) {
		setPositionByTile(tile);
	}

	public boolean canMove() {
		return canMove;
	}

	public void setCanMove(boolean canMove) {
		this.canMove = canMove;
	}

	public int getRemainingAttacks() {
		return remainingAttacks;
	}

	public void setRemainingAttacks(int remainingAttacks) {
		this.remainingAttacks = remainingAttacks;
	}

	public boolean canAttackNow() {
		return remainingAttacks > 0;
	}

	public boolean isSummonedThisTurn() {
		return summonedThisTurn;
	}

	public void markSummonedThisTurn() {
		summonedThisTurn = true;
		canMove = false;
		remainingAttacks = hasAbility(UnitAbility.RUSH) ? 1 : 0;
	}

	public boolean isStunned() {
		return stunned;
	}

	public void stun() {
		stunned = true;
		canMove = false;
		remainingAttacks = 0;
	}

	public boolean isCounterAvailable() {
		return counterAvailable;
	}

	public void setCounterAvailable(boolean counterAvailable) {
		this.counterAvailable = counterAvailable;
	}

	public void refreshForOwnerTurn() {
		counterAvailable = true;
		if (stunned) {
			stunned = false;
			summonedThisTurn = false;
			canMove = false;
			remainingAttacks = 0;
			return;
		}
		summonedThisTurn = false;
		canMove = true;
		remainingAttacks = 1;
	}

	public void markMoved() {
		canMove = false;
	}

	public void markAttacked() {
		if (remainingAttacks > 0) {
			remainingAttacks--;
		}
		canMove = false;
	}

	public void adjustAttack(int delta) {
		attack += delta;
	}

	public void adjustHealthAndMaxHealth(int delta) {
		maxHealth += delta;
		health += delta;
	}

	public void takeDamage(int damage) {
		health -= damage;
	}

	public void heal(int amount) {
		health = Math.min(maxHealth, health + amount);
	}

	public boolean isDead() {
		return health <= 0;
	}
}
