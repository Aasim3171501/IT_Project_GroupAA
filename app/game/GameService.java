package game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;
import structures.basic.EffectAnimation;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.UnitAnimationType;
import utils.BasicObjectBuilders;
import utils.OrderedCardLoader;
import utils.StaticConfFiles;

/**
 * Match orchestration and rules engine.
 */
public final class GameService {

	private static final int AVATAR_ATTACK = 2;
	private static final int AVATAR_HEALTH = 20;
	private static final int WRAITHLING_ATTACK = 1;
	private static final int WRAITHLING_HEALTH = 1;

	private static final int[][] ORTHOGONAL_DIRECTIONS = {
		{1, 0}, {-1, 0}, {0, 1}, {0, -1}
	};

	private static final int[][] ADJACENT_DIRECTIONS = {
		{-1, -1}, {-1, 0}, {-1, 1},
		{0, -1},            {0, 1},
		{1, -1},  {1, 0},   {1, 1}
	};

	private static final String WRAITHLING_SUMMON_EFFECT = "conf/gameconfs/effects/f1_wraithsummon.json";

	private GameService() {}

	public static void initializeGame(ActorRef out, GameState gameState) {
		if (gameState.gameInitalised) {
			return;
		}

		gameState.resetBoard();
		gameState.gameInitalised = true;
		gameState.something = true;

		GamePlayer human = new GamePlayer(1, "Player 1", false, OrderedCardLoader.getPlayer1Cards(2));
		GamePlayer ai = new GamePlayer(2, "Player 2", true, OrderedCardLoader.getPlayer2Cards(2));
		gameState.setPlayer1(human);
		gameState.setPlayer2(ai);
		gameState.setCurrentPlayerId(1);

		GameUnit humanAvatar = createAvatar(gameState, StaticConfFiles.humanAvatar, human.getId(), human.getDisplayName() + " Avatar");
		GameUnit aiAvatar = createAvatar(gameState, StaticConfFiles.aiAvatar, ai.getId(), ai.getDisplayName() + " Avatar");
		human.setAvatar(humanAvatar);
		ai.setAvatar(aiAvatar);
		gameState.addUnit(humanAvatar, gameState.getTile(1, 3));
		gameState.addUnit(aiAvatar, gameState.getTile(9, 3));

		for (int draw = 0; draw < 3; draw++) {
			human.drawCard();
			ai.drawCard();
		}

		startTurn(gameState, human.getId());
		renderFullBoard(out, gameState);
		renderAllUnits(out, gameState);
		renderPlayerPanels(out, gameState);
		renderHumanHand(out, gameState);
		renderTileHighlights(out, gameState);
		BasicCommands.addPlayer1Notification(out, "Your turn", 2);
		BasicCommands.addPlayer2Notification(out, "AI waiting", 2);
	}

	public static void handleCardClicked(ActorRef out, GameState gameState, int handPosition) {
		if (!isHumanTurn(gameState) || gameState.isGameOver()) {
			return;
		}

		GamePlayer player = gameState.getCurrentPlayer();
		Card card = player.getCardAtPosition(handPosition);
		if (card == null) {
			return;
		}
		if (!player.canAfford(card)) {
			BasicCommands.addPlayer1Notification(out, "Not enough mana", 2);
			return;
		}

		if (isImmediateSpell(card)) {
			castCard(out, gameState, handPosition, null);
			return;
		}

		if (gameState.getSelectedCardPosition() != null
				&& gameState.getSelectedCardPosition().intValue() == handPosition) {
			gameState.clearSelection();
		} else {
			gameState.setSelectedCardPosition(Integer.valueOf(handPosition));
			gameState.setSelectedUnitId(null);
		}
		renderHumanHand(out, gameState);
		renderTileHighlights(out, gameState);
	}

	public static void handleTileClicked(ActorRef out, GameState gameState, int tilex, int tiley) {
		if (!gameState.gameInitalised || gameState.isGameOver() || !isHumanTurn(gameState)) {
			return;
		}

		GameUnit clickedUnit = gameState.getUnitAt(tilex, tiley);
		Tile clickedTile = gameState.getTile(tilex, tiley);
		if (clickedTile == null) {
			return;
		}

		if (gameState.getSelectedCardPosition() != null) {
			if (castCard(out, gameState, gameState.getSelectedCardPosition().intValue(), clickedTile)) {
				return;
			}
		}

		if (clickedUnit != null && clickedUnit.getOwnerId() == gameState.getCurrentPlayerId()) {
			selectUnit(out, gameState, clickedUnit.getId());
			return;
		}

		if (gameState.getSelectedUnitId() == null) {
			return;
		}

		GameUnit selectedUnit = gameState.getUnitById(gameState.getSelectedUnitId().intValue());
		if (selectedUnit == null) {
			gameState.clearSelection();
			renderHumanHand(out, gameState);
			renderTileHighlights(out, gameState);
			return;
		}

		BoardPosition clickedPosition = new BoardPosition(tilex, tiley);
		Set<BoardPosition> moveTiles = getValidMoveTiles(gameState, selectedUnit);
		Set<BoardPosition> attackTiles = getValidAttackTargets(gameState, selectedUnit);

		if (clickedUnit == null && moveTiles.contains(clickedPosition)) {
			moveUnit(out, gameState, selectedUnit, clickedTile);
			if (selectedUnit.canAttackNow()) {
				gameState.setSelectedUnitId(Integer.valueOf(selectedUnit.getId()));
			} else {
				gameState.clearSelection();
			}
			renderHumanHand(out, gameState);
			renderTileHighlights(out, gameState);
			return;
		}

		if (clickedUnit != null && clickedUnit.getOwnerId() != selectedUnit.getOwnerId()
				&& attackTiles.contains(clickedPosition)) {
			attackUnit(out, gameState, selectedUnit, clickedUnit);
			gameState.clearSelection();
			renderHumanHand(out, gameState);
			renderTileHighlights(out, gameState);
		}
	}

	public static void handleOtherClicked(ActorRef out, GameState gameState) {
		if (!gameState.gameInitalised || gameState.isGameOver()) {
			return;
		}
		gameState.clearSelection();
		renderHumanHand(out, gameState);
		renderTileHighlights(out, gameState);
	}

	public static void handleEndTurn(ActorRef out, GameState gameState) {
		if (!gameState.gameInitalised || gameState.isGameOver() || !isHumanTurn(gameState)) {
			return;
		}

		finishCurrentTurn(out, gameState);
		if (gameState.isGameOver()) {
			return;
		}

		runAiTurn(out, gameState);
		if (!gameState.isGameOver()) {
			finishCurrentTurn(out, gameState);
			BasicCommands.addPlayer1Notification(out, "Your turn", 2);
			BasicCommands.addPlayer2Notification(out, "AI waiting", 2);
		}
	}

	private static void selectUnit(ActorRef out, GameState gameState, int unitId) {
		gameState.setSelectedUnitId(Integer.valueOf(unitId));
		gameState.setSelectedCardPosition(null);
		renderHumanHand(out, gameState);
		renderTileHighlights(out, gameState);
	}

	private static void finishCurrentTurn(ActorRef out, GameState gameState) {
		GamePlayer endingPlayer = gameState.getCurrentPlayer();
		endingPlayer.drawCard();
		endingPlayer.syncUiPlayer();
		gameState.clearSelection();

		int nextPlayerId = endingPlayer.getId() == 1 ? 2 : 1;
		gameState.setCurrentPlayerId(nextPlayerId);
		startTurn(gameState, nextPlayerId);

		renderPlayerPanels(out, gameState);
		renderHumanHand(out, gameState);
		renderTileHighlights(out, gameState);
	}

	private static void startTurn(GameState gameState, int playerId) {
		GamePlayer player = gameState.getPlayerById(playerId);
		player.startTurn();
		for (GameUnit unit : gameState.getUnits()) {
			unit.setCounterAvailable(true);
			if (unit.getOwnerId() == playerId) {
				unit.refreshForOwnerTurn();
			}
		}
		updateAvatarAttack(player);
		player.syncUiPlayer();
	}

	private static boolean castCard(ActorRef out, GameState gameState, int handPosition, Tile targetTile) {
		GamePlayer player = gameState.getCurrentPlayer();
		Card card = player.getCardAtPosition(handPosition);
		if (card == null || !player.canAfford(card)) {
			return false;
		}

		boolean cast = false;
		if (card.isCreature()) {
			cast = summonCreature(out, gameState, player, card, targetTile);
		} else if ("Wraithling Swarm".equals(card.getCardname())) {
			cast = castWraithlingSwarm(out, gameState, player);
		} else if ("Horn of the Forsaken".equals(card.getCardname())) {
			cast = castHornOfTheForsaken(out, gameState, player);
		} else if ("Beamshock".equals(card.getCardname())) {
			cast = castBeamshock(out, gameState, player, targetTile);
		} else if ("Sundrop Elixir".equals(card.getCardname())) {
			cast = castSundropElixir(out, gameState, player, targetTile);
		} else if ("Dark Terminus".equals(card.getCardname())) {
			cast = castDarkTerminus(out, gameState, player, targetTile);
		} else if ("Truestrike".equals(card.getCardname())) {
			cast = castTruestrike(out, gameState, player, targetTile);
		}

		if (!cast) {
			return false;
		}

		player.spendMana(card.getManacost());
		player.removeCardAtPosition(handPosition);
		player.syncUiPlayer();
		gameState.clearSelection();
		renderPlayerPanels(out, gameState);
		renderHumanHand(out, gameState);
		renderTileHighlights(out, gameState);
		checkForGameOver(out, gameState);
		return true;
	}

	private static boolean summonCreature(ActorRef out, GameState gameState, GamePlayer player, Card card, Tile targetTile) {
		if (targetTile == null) {
			return false;
		}
		BoardPosition targetPosition = BoardPosition.fromTile(targetTile);
		Set<BoardPosition> validSummons = getValidSummonTiles(gameState, card, player);
		if (!validSummons.contains(targetPosition)) {
			BasicCommands.addPlayer1Notification(out, "Invalid summon tile", 2);
			return false;
		}

		GameUnit unit = createUnitFromCard(gameState, card, player.getId());
		unit.markSummonedThisTurn();
		gameState.addUnit(unit, targetTile);
		drawUnit(out, gameState, unit);
		playEffectAtTile(out, StaticConfFiles.f1_summon, targetTile);
		resolveOpeningGambit(out, gameState, unit);
		return true;
	}

	private static boolean castWraithlingSwarm(ActorRef out, GameState gameState, GamePlayer player) {
		List<Tile> summonTiles = getAdjacentFreeTiles(gameState, player.getAvatar().getBoardPosition());
		if (summonTiles.isEmpty()) {
			return false;
		}
		int summons = Math.min(3, summonTiles.size());
		for (int index = 0; index < summons; index++) {
			summonWraithling(out, gameState, player.getId(), summonTiles.get(index), true);
		}
		return true;
	}

	private static boolean castHornOfTheForsaken(ActorRef out, GameState gameState, GamePlayer player) {
		player.equipArtifact(1, 3);
		updateAvatarAttack(player);
		renderPlayerPanels(out, gameState);
		refreshUnitStats(out, player.getAvatar());
		playEffectAtTile(out, StaticConfFiles.f1_buff, player.getAvatarTile(gameState));
		return true;
	}

	private static boolean castBeamshock(ActorRef out, GameState gameState, GamePlayer player, Tile targetTile) {
		GameUnit target = requireTargetUnit(gameState, targetTile);
		if (target == null || target.getOwnerId() == player.getId()) {
			return false;
		}
		target.stun();
		playProjectile(out, player.getAvatarTile(gameState), targetTile);
		playEffectAtTile(out, StaticConfFiles.f1_buff, targetTile);
		return true;
	}

	private static boolean castSundropElixir(ActorRef out, GameState gameState, GamePlayer player, Tile targetTile) {
		GameUnit target = requireTargetUnit(gameState, targetTile);
		if (target == null || target.getHealthValue() >= target.getMaxHealth()) {
			return false;
		}
		target.heal(5);
		refreshUnitStats(out, target);
		syncPlayers(out, gameState);
		playEffectAtTile(out, StaticConfFiles.f1_buff, targetTile);
		return true;
	}

	private static boolean castDarkTerminus(ActorRef out, GameState gameState, GamePlayer player, Tile targetTile) {
		GameUnit target = requireTargetUnit(gameState, targetTile);
		if (target == null || target.getOwnerId() == player.getId() || target.isAvatar()) {
			return false;
		}
		BoardPosition deadPosition = target.getBoardPosition();
		destroyUnit(out, gameState, target);
		summonWraithling(out, gameState, player.getId(), gameState.getTile(deadPosition.getX(), deadPosition.getY()), true);
		playEffectAtTile(out, StaticConfFiles.f1_soulshatter, targetTile);
		return true;
	}

	private static boolean castTruestrike(ActorRef out, GameState gameState, GamePlayer player, Tile targetTile) {
		GameUnit target = requireTargetUnit(gameState, targetTile);
		if (target == null || target.getOwnerId() == player.getId()) {
			return false;
		}
		playProjectile(out, player.getAvatarTile(gameState), targetTile);
		damageUnit(out, gameState, target, 2);
		playEffectAtTile(out, StaticConfFiles.f1_inmolation, targetTile);
		return true;
	}

	private static GameUnit requireTargetUnit(GameState gameState, Tile targetTile) {
		if (targetTile == null) {
			return null;
		}
		return gameState.getUnitAt(targetTile.getTilex(), targetTile.getTiley());
	}

	private static void moveUnit(ActorRef out, GameState gameState, GameUnit unit, Tile targetTile) {
		BoardPosition startPosition = unit.getBoardPosition();
		gameState.moveUnit(unit, targetTile);
		unit.markMoved();
		BasicCommands.moveUnitToTile(out, unit, targetTile, preferYFirst(startPosition, BoardPosition.fromTile(targetTile)));
	}

	private static boolean preferYFirst(BoardPosition start, BoardPosition end) {
		return Math.abs(start.getY() - end.getY()) > Math.abs(start.getX() - end.getX());
	}

	private static void attackUnit(ActorRef out, GameState gameState, GameUnit attacker, GameUnit defender) {
		BoardPosition attackTile = defender.getBoardPosition();
		BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);
		damageUnit(out, gameState, defender, attacker.getAttackValue());
		boolean defenderAliveAfterHit = !defender.isDead();

		if (attacker.isAvatar()) {
			triggerAvatarArtifact(out, gameState, attacker);
		}

		if (defenderAliveAfterHit && defender.isCounterAvailable() && !attacker.isDead()) {
			defender.setCounterAvailable(false);
			BasicCommands.playUnitAnimation(out, defender, UnitAnimationType.hit);
			damageUnit(out, gameState, attacker, defender.getAttackValue());
		}

		attacker.markAttacked();
		if (attacker.isAvatar()) {
			GamePlayer owner = gameState.getPlayerById(attacker.getOwnerId());
			if (owner.hasArtifact()) {
				owner.consumeArtifactCharge();
				updateAvatarAttack(owner);
				refreshUnitStats(out, attacker);
			}
		}

		playEffectAtTile(out, StaticConfFiles.f1_martyrdom, gameState.getTile(attackTile.getX(), attackTile.getY()));
		checkForGameOver(out, gameState);
	}

	private static void triggerAvatarArtifact(ActorRef out, GameState gameState, GameUnit avatar) {
		GamePlayer owner = gameState.getPlayerById(avatar.getOwnerId());
		if (!owner.hasArtifact()) {
			return;
		}
		List<Tile> summonTiles = getAdjacentFreeTiles(gameState, avatar.getBoardPosition());
		if (summonTiles.isEmpty()) {
			return;
		}
		summonWraithling(out, gameState, owner.getId(), summonTiles.get(0), true);
	}

	private static void damageUnit(ActorRef out, GameState gameState, GameUnit target, int damage) {
		if (target == null || damage <= 0 || target.isDead()) {
			return;
		}
		target.takeDamage(damage);
		if (target.isAvatar()) {
			onAvatarDamaged(out, gameState, target);
		}
		if (target.isDead()) {
			destroyUnit(out, gameState, target);
		} else {
			BasicCommands.playUnitAnimation(out, target, UnitAnimationType.hit);
			refreshUnitStats(out, target);
			syncPlayers(out, gameState);
		}
	}

	private static void onAvatarDamaged(ActorRef out, GameState gameState, GameUnit avatar) {
		GamePlayer owner = gameState.getPlayerById(avatar.getOwnerId());
		owner.syncUiPlayer();
		syncPlayers(out, gameState);
		for (GameUnit ally : gameState.getUnitsForPlayer(owner.getId())) {
			if ("Silverguard Knight".equals(ally.getName()) && !ally.isDead()) {
				ally.adjustAttack(2);
				refreshUnitStats(out, ally);
			}
		}
	}

	private static void destroyUnit(ActorRef out, GameState gameState, GameUnit unit) {
		BoardPosition deathPosition = unit.getBoardPosition();
		int ownerId = unit.getOwnerId();

		gameState.removeUnit(unit);
		BasicCommands.deleteUnit(out, unit);
		if (unit.isAvatar()) {
			GamePlayer defeated = gameState.getPlayerById(ownerId);
			defeated.syncUiPlayer();
			checkForGameOver(out, gameState);
			return;
		}

		handleDeathwatch(out, gameState, deathPosition);
		syncPlayers(out, gameState);
		checkForGameOver(out, gameState);
	}

	private static void handleDeathwatch(ActorRef out, GameState gameState, BoardPosition deathPosition) {
		List<GameUnit> watchers = new ArrayList<GameUnit>(gameState.getUnits());
		for (GameUnit watcher : watchers) {
			if (watcher.isDead()) {
				continue;
			}
			if ("Bad Omen".equals(watcher.getName())) {
				watcher.adjustAttack(1);
				refreshUnitStats(out, watcher);
			}
			if ("Shadow Watcher".equals(watcher.getName())) {
				watcher.adjustAttack(1);
				watcher.adjustHealthAndMaxHealth(1);
				refreshUnitStats(out, watcher);
			}
			if ("Bloodmoon Priestess".equals(watcher.getName())) {
				List<Tile> summonTiles = getAdjacentFreeTiles(gameState, watcher.getBoardPosition());
				if (!summonTiles.isEmpty()) {
					Tile chosenTile = summonTiles.get(gameState.getRandom().nextInt(summonTiles.size()));
					summonWraithling(out, gameState, watcher.getOwnerId(), chosenTile, true);
				}
			}
			if ("Shadowdancer".equals(watcher.getName())) {
				GamePlayer owner = gameState.getPlayerById(watcher.getOwnerId());
				GamePlayer enemy = gameState.getPlayerById(watcher.getOwnerId() == 1 ? 2 : 1);
				damageUnit(out, gameState, enemy.getAvatar(), 1);
				if (!owner.getAvatar().isDead()) {
					owner.getAvatar().heal(1);
					refreshUnitStats(out, owner.getAvatar());
					owner.syncUiPlayer();
					syncPlayers(out, gameState);
				}
			}
		}
		playEffectAtTile(out, StaticConfFiles.f1_soulshatter, gameState.getTile(deathPosition.getX(), deathPosition.getY()));
	}

	private static void resolveOpeningGambit(ActorRef out, GameState gameState, GameUnit unit) {
		if ("Gloom Chaser".equals(unit.getName())) {
			Tile behindTile = getTileBehindUnit(gameState, unit);
			if (behindTile != null && !gameState.isOccupied(behindTile.getTilex(), behindTile.getTiley())) {
				summonWraithling(out, gameState, unit.getOwnerId(), behindTile, false);
			}
		}

		if ("Nightsorrow Assassin".equals(unit.getName())) {
			List<GameUnit> adjacentEnemies = getAdjacentEnemies(gameState, unit);
			for (GameUnit enemy : adjacentEnemies) {
				if (enemy.getHealthValue() < enemy.getMaxHealth() && !enemy.isAvatar()) {
					destroyUnit(out, gameState, enemy);
					break;
				}
			}
		}

		if ("Silverguard Squire".equals(unit.getName())) {
			Tile frontTile = getDirectionalTile(gameState, unit, true);
			Tile backTile = getDirectionalTile(gameState, unit, false);
			applySquireBuff(out, gameState, unit, frontTile);
			applySquireBuff(out, gameState, unit, backTile);
		}
	}

	private static void applySquireBuff(ActorRef out, GameState gameState, GameUnit squire, Tile tile) {
		if (tile == null) {
			return;
		}
		GameUnit ally = gameState.getUnitAt(tile.getTilex(), tile.getTiley());
		if (ally != null && ally.getOwnerId() == squire.getOwnerId()) {
			ally.adjustAttack(1);
			ally.adjustHealthAndMaxHealth(1);
			refreshUnitStats(out, ally);
		}
	}

	private static Tile getTileBehindUnit(GameState gameState, GameUnit unit) {
		int direction = unit.getOwnerId() == 1 ? -1 : 1;
		int targetX = unit.getBoardPosition().getX() + direction;
		int targetY = unit.getBoardPosition().getY();
		return gameState.getTile(targetX, targetY);
	}

	private static Tile getDirectionalTile(GameState gameState, GameUnit unit, boolean front) {
		int direction = unit.getOwnerId() == 1 ? 1 : -1;
		if (!front) {
			direction = -direction;
		}
		int targetX = unit.getBoardPosition().getX() + direction;
		return gameState.getTile(targetX, unit.getBoardPosition().getY());
	}

	private static void summonWraithling(ActorRef out, GameState gameState, int ownerId, Tile targetTile, boolean withEffect) {
		if (targetTile == null || gameState.isOccupied(targetTile.getTilex(), targetTile.getTiley())) {
			return;
		}
		GameUnit wraithling = createTokenUnit(gameState, ownerId, "Wraithling", StaticConfFiles.wraithling,
				WRAITHLING_ATTACK, WRAITHLING_HEALTH, EnumSet.noneOf(UnitAbility.class));
		wraithling.markSummonedThisTurn();
		gameState.addUnit(wraithling, targetTile);
		drawUnit(out, gameState, wraithling);
		if (withEffect) {
			playEffectAtTile(out, WRAITHLING_SUMMON_EFFECT, targetTile);
		}
	}

	private static GameUnit createAvatar(GameState gameState, String configFile, int ownerId, String name) {
		Unit base = BasicObjectBuilders.loadUnit(configFile, gameState.nextUnitId(), Unit.class);
		return new GameUnit(base, name, ownerId, AVATAR_ATTACK, AVATAR_HEALTH, true,
				EnumSet.of(UnitAbility.AVATAR));
	}

	private static GameUnit createUnitFromCard(GameState gameState, Card card, int ownerId) {
		Unit base = BasicObjectBuilders.loadUnit(card.getUnitConfig(), gameState.nextUnitId(), Unit.class);
		return new GameUnit(base, card.getCardname(), ownerId, card.getBigCard().getAttack(),
				card.getBigCard().getHealth(), false, abilitiesFor(card.getCardname()));
	}

	private static GameUnit createTokenUnit(GameState gameState, int ownerId, String name, String configFile,
			int attack, int health, EnumSet<UnitAbility> abilities) {
		Unit base = BasicObjectBuilders.loadUnit(configFile, gameState.nextUnitId(), Unit.class);
		return new GameUnit(base, name, ownerId, attack, health, false, abilities);
	}

	private static EnumSet<UnitAbility> abilitiesFor(String cardName) {
		EnumSet<UnitAbility> abilities = EnumSet.noneOf(UnitAbility.class);
		if ("Rock Pulveriser".equals(cardName) || "Swamp Entangler".equals(cardName)
				|| "Silverguard Knight".equals(cardName) || "Ironcliff Guardian".equals(cardName)) {
			abilities.add(UnitAbility.PROVOKE);
		}
		if ("Saberspine Tiger".equals(cardName)) {
			abilities.add(UnitAbility.RUSH);
		}
		if ("Young Flamewing".equals(cardName)) {
			abilities.add(UnitAbility.FLYING);
		}
		if ("Ironcliff Guardian".equals(cardName)) {
			abilities.add(UnitAbility.AIRDROP);
		}
		return abilities;
	}

	private static boolean isImmediateSpell(Card card) {
		return "Wraithling Swarm".equals(card.getCardname()) || "Horn of the Forsaken".equals(card.getCardname());
	}

	private static Set<BoardPosition> getValidSummonTiles(GameState gameState, Card card, GamePlayer player) {
		Set<BoardPosition> validTiles = new HashSet<BoardPosition>();
		if (!card.isCreature()) {
			return validTiles;
		}
		if ("Ironcliff Guardian".equals(card.getCardname())) {
			for (int x = 1; x <= GameState.BOARD_WIDTH; x++) {
				for (int y = 1; y <= GameState.BOARD_HEIGHT; y++) {
					if (!gameState.isOccupied(x, y)) {
						validTiles.add(new BoardPosition(x, y));
					}
				}
			}
			return validTiles;
		}
		for (Tile tile : getAdjacentFreeTiles(gameState, player.getAvatar().getBoardPosition())) {
			validTiles.add(BoardPosition.fromTile(tile));
		}
		return validTiles;
	}

	private static Set<BoardPosition> getValidSpellTargets(GameState gameState, Card card, GamePlayer player) {
		Set<BoardPosition> validTargets = new HashSet<BoardPosition>();
		Collection<GameUnit> units = gameState.getUnits();
		for (GameUnit unit : units) {
			if ("Beamshock".equals(card.getCardname()) || "Truestrike".equals(card.getCardname())) {
				if (unit.getOwnerId() != player.getId()) {
					validTargets.add(unit.getBoardPosition());
				}
			}
			if ("Sundrop Elixir".equals(card.getCardname()) && unit.getHealthValue() < unit.getMaxHealth()) {
				validTargets.add(unit.getBoardPosition());
			}
			if ("Dark Terminus".equals(card.getCardname()) && unit.getOwnerId() != player.getId() && !unit.isAvatar()) {
				validTargets.add(unit.getBoardPosition());
			}
		}
		return validTargets;
	}

	private static Set<BoardPosition> getValidMoveTiles(GameState gameState, GameUnit unit) {
		if (unit == null || !unit.canMove()) {
			return Collections.emptySet();
		}
		if (!unit.hasAbility(UnitAbility.FLYING) && hasAdjacentEnemyProvoke(gameState, unit)) {
			return Collections.emptySet();
		}

		Set<BoardPosition> reachable = new HashSet<BoardPosition>();
		if (unit.hasAbility(UnitAbility.FLYING)) {
			for (int x = 1; x <= GameState.BOARD_WIDTH; x++) {
				for (int y = 1; y <= GameState.BOARD_HEIGHT; y++) {
					BoardPosition target = new BoardPosition(x, y);
					if (!gameState.isOccupied(x, y) && unit.getBoardPosition().manhattanDistance(target) <= 2) {
						reachable.add(target);
					}
				}
			}
			return reachable;
		}

		Deque<BoardPosition> queue = new ArrayDeque<BoardPosition>();
		Map<String, Integer> distance = new HashMap<String, Integer>();
		queue.add(unit.getBoardPosition());
		distance.put(unit.getBoardPosition().toKey(), Integer.valueOf(0));

		while (!queue.isEmpty()) {
			BoardPosition current = queue.removeFirst();
			int currentDistance = distance.get(current.toKey()).intValue();
			if (currentDistance == 2) {
				continue;
			}
			for (int[] direction : ORTHOGONAL_DIRECTIONS) {
				BoardPosition next = current.offset(direction[0], direction[1]);
				if (!next.isInsideBoard(GameState.BOARD_WIDTH, GameState.BOARD_HEIGHT)) {
					continue;
				}
				if (distance.containsKey(next.toKey())) {
					continue;
				}
				if (gameState.isOccupied(next.getX(), next.getY())) {
					continue;
				}
				distance.put(next.toKey(), Integer.valueOf(currentDistance + 1));
				reachable.add(next);
				queue.addLast(next);
			}
		}

		return reachable;
	}

	private static Set<BoardPosition> getValidAttackTargets(GameState gameState, GameUnit unit) {
		if (unit == null || !unit.canAttackNow()) {
			return Collections.emptySet();
		}
		Set<BoardPosition> targets = new HashSet<BoardPosition>();
		List<GameUnit> provokingEnemies = new ArrayList<GameUnit>();

		for (int[] direction : ADJACENT_DIRECTIONS) {
			BoardPosition targetPosition = unit.getBoardPosition().offset(direction[0], direction[1]);
			if (!targetPosition.isInsideBoard(GameState.BOARD_WIDTH, GameState.BOARD_HEIGHT)) {
				continue;
			}
			GameUnit occupant = gameState.getUnitAt(targetPosition.getX(), targetPosition.getY());
			if (occupant == null || occupant.getOwnerId() == unit.getOwnerId()) {
				continue;
			}
			if (occupant.hasAbility(UnitAbility.PROVOKE)) {
				provokingEnemies.add(occupant);
			}
			targets.add(targetPosition);
		}

		if (!unit.hasAbility(UnitAbility.FLYING) && !provokingEnemies.isEmpty()) {
			Set<BoardPosition> restrictedTargets = new HashSet<BoardPosition>();
			for (GameUnit provokingEnemy : provokingEnemies) {
				restrictedTargets.add(provokingEnemy.getBoardPosition());
			}
			return restrictedTargets;
		}
		return targets;
	}

	private static boolean hasAdjacentEnemyProvoke(GameState gameState, GameUnit unit) {
		for (int[] direction : ADJACENT_DIRECTIONS) {
			BoardPosition targetPosition = unit.getBoardPosition().offset(direction[0], direction[1]);
			if (!targetPosition.isInsideBoard(GameState.BOARD_WIDTH, GameState.BOARD_HEIGHT)) {
				continue;
			}
			GameUnit occupant = gameState.getUnitAt(targetPosition.getX(), targetPosition.getY());
			if (occupant != null && occupant.getOwnerId() != unit.getOwnerId()
					&& occupant.hasAbility(UnitAbility.PROVOKE)) {
				return true;
			}
		}
		return false;
	}

	private static List<GameUnit> getAdjacentEnemies(GameState gameState, GameUnit unit) {
		List<GameUnit> enemies = new ArrayList<GameUnit>();
		for (int[] direction : ADJACENT_DIRECTIONS) {
			BoardPosition targetPosition = unit.getBoardPosition().offset(direction[0], direction[1]);
			if (!targetPosition.isInsideBoard(GameState.BOARD_WIDTH, GameState.BOARD_HEIGHT)) {
				continue;
			}
			GameUnit occupant = gameState.getUnitAt(targetPosition.getX(), targetPosition.getY());
			if (occupant != null && occupant.getOwnerId() != unit.getOwnerId()) {
				enemies.add(occupant);
			}
		}
		return enemies;
	}

	private static List<Tile> getAdjacentFreeTiles(GameState gameState, BoardPosition centre) {
		List<Tile> tiles = new ArrayList<Tile>();
		for (int[] direction : ADJACENT_DIRECTIONS) {
			BoardPosition targetPosition = centre.offset(direction[0], direction[1]);
			if (!targetPosition.isInsideBoard(GameState.BOARD_WIDTH, GameState.BOARD_HEIGHT)) {
				continue;
			}
			if (!gameState.isOccupied(targetPosition.getX(), targetPosition.getY())) {
				tiles.add(gameState.getTile(targetPosition.getX(), targetPosition.getY()));
			}
		}
		return tiles;
	}

	private static void runAiTurn(ActorRef out, GameState gameState) {
		if (gameState.isGameOver()) {
			return;
		}
		BasicCommands.addPlayer2Notification(out, "AI turn", 2);

		boolean playedCard = true;
		while (playedCard && !gameState.isGameOver()) {
			playedCard = tryPlayAiCard(out, gameState);
		}

		List<GameUnit> aiUnits = new ArrayList<GameUnit>(gameState.getUnitsForPlayer(gameState.getCurrentPlayerId()));
		for (GameUnit unit : aiUnits) {
			if (gameState.isGameOver()) {
				return;
			}
			if (gameState.getUnitById(unit.getId()) == null) {
				continue;
			}
			executeAiUnitTurn(out, gameState, unit);
		}
	}

	private static boolean tryPlayAiCard(ActorRef out, GameState gameState) {
		GamePlayer ai = gameState.getCurrentPlayer();
		for (int handPosition = 1; handPosition <= ai.getHand().size(); handPosition++) {
			Card card = ai.getCardAtPosition(handPosition);
			if (card == null || !ai.canAfford(card)) {
				continue;
			}

			if (card.isCreature()) {
				Set<BoardPosition> summonTiles = getValidSummonTiles(gameState, card, ai);
				if (!summonTiles.isEmpty()) {
					Tile chosenTile = chooseBestAiSummonTile(gameState, summonTiles);
					return castCard(out, gameState, handPosition, chosenTile);
				}
				continue;
			}

			if (isImmediateSpell(card)) {
				return castCard(out, gameState, handPosition, null);
			}

			Set<BoardPosition> targets = getValidSpellTargets(gameState, card, ai);
			if (!targets.isEmpty()) {
				Tile chosenTile = chooseBestAiSpellTarget(gameState, card, targets);
				return castCard(out, gameState, handPosition, chosenTile);
			}
		}
		return false;
	}

	private static Tile chooseBestAiSummonTile(GameState gameState, Set<BoardPosition> positions) {
		GameUnit enemyAvatar = gameState.getOpponentPlayer().getAvatar();
		BoardPosition best = Collections.min(new ArrayList<BoardPosition>(positions),
				Comparator.comparingInt(position -> position.manhattanDistance(enemyAvatar.getBoardPosition())));
		return gameState.getTile(best.getX(), best.getY());
	}

	private static Tile chooseBestAiSpellTarget(GameState gameState, Card card, Set<BoardPosition> positions) {
		GamePlayer ai = gameState.getCurrentPlayer();
		BoardPosition best = null;
		int bestScore = Integer.MIN_VALUE;
		for (BoardPosition position : positions) {
			GameUnit target = gameState.getUnitAt(position.getX(), position.getY());
			if (target == null) {
				continue;
			}
			int score = 0;
			if ("Dark Terminus".equals(card.getCardname())) {
				score = (target.getAttackValue() * 3) + target.getHealthValue();
			}
			if ("Truestrike".equals(card.getCardname())) {
				score = target.getHealthValue() <= 2 ? 100 : target.getAttackValue();
				if (target.isAvatar()) {
					score += 5;
				}
			}
			if ("Beamshock".equals(card.getCardname())) {
				score = target.getAttackValue() * 2;
			}
			if ("Sundrop Elixir".equals(card.getCardname())) {
				score = target.getOwnerId() == ai.getId() ? (target.getMaxHealth() - target.getHealthValue()) : -100;
				if (target.isAvatar()) {
					score += 10;
				}
			}
			if (score > bestScore) {
				bestScore = score;
				best = position;
			}
		}
		if (best == null) {
			best = positions.iterator().next();
		}
		return gameState.getTile(best.getX(), best.getY());
	}

	private static void executeAiUnitTurn(ActorRef out, GameState gameState, GameUnit unit) {
		Set<BoardPosition> immediateTargets = getValidAttackTargets(gameState, unit);
		if (!immediateTargets.isEmpty()) {
			GameUnit target = chooseBestAiAttackTarget(gameState, immediateTargets);
			if (target != null) {
				attackUnit(out, gameState, unit, target);
			}
			return;
		}

		Set<BoardPosition> moveTiles = getValidMoveTiles(gameState, unit);
		if (!moveTiles.isEmpty()) {
			Tile moveTile = chooseBestAiMoveTile(gameState, unit, moveTiles);
			if (moveTile != null) {
				moveUnit(out, gameState, unit, moveTile);
			}
		}

		immediateTargets = getValidAttackTargets(gameState, unit);
		if (!immediateTargets.isEmpty()) {
			GameUnit target = chooseBestAiAttackTarget(gameState, immediateTargets);
			if (target != null) {
				attackUnit(out, gameState, unit, target);
			}
		}
	}

	private static Tile chooseBestAiMoveTile(GameState gameState, GameUnit unit, Set<BoardPosition> moveTiles) {
		GameUnit enemyAvatar = gameState.getOpponentPlayer().getAvatar();
		BoardPosition best = null;
		int bestScore = Integer.MIN_VALUE;
		for (BoardPosition position : moveTiles) {
			int score = -position.manhattanDistance(enemyAvatar.getBoardPosition());
			if (wouldHaveAttackAfterMove(gameState, unit, position)) {
				score += 50;
			}
			if (score > bestScore) {
				bestScore = score;
				best = position;
			}
		}
		if (best == null) {
			return null;
		}
		return gameState.getTile(best.getX(), best.getY());
	}

	private static boolean wouldHaveAttackAfterMove(GameState gameState, GameUnit unit, BoardPosition movePosition) {
		for (int[] direction : ADJACENT_DIRECTIONS) {
			BoardPosition targetPosition = movePosition.offset(direction[0], direction[1]);
			if (!targetPosition.isInsideBoard(GameState.BOARD_WIDTH, GameState.BOARD_HEIGHT)) {
				continue;
			}
			GameUnit occupant = gameState.getUnitAt(targetPosition.getX(), targetPosition.getY());
			if (occupant != null && occupant.getOwnerId() != unit.getOwnerId()) {
				return true;
			}
		}
		return false;
	}

	private static GameUnit chooseBestAiAttackTarget(GameState gameState, Set<BoardPosition> targetPositions) {
		GameUnit bestTarget = null;
		int bestScore = Integer.MIN_VALUE;
		for (BoardPosition position : targetPositions) {
			GameUnit target = gameState.getUnitAt(position.getX(), position.getY());
			if (target == null) {
				continue;
			}
			int score = target.isAvatar() ? 100 : 50;
			score += (20 - target.getHealthValue());
			score += target.getAttackValue();
			if (score > bestScore) {
				bestScore = score;
				bestTarget = target;
			}
		}
		return bestTarget;
	}

	private static void renderFullBoard(ActorRef out, GameState gameState) {
		for (Tile tile : gameState.getTiles()) {
			BasicCommands.drawTile(out, tile, 0);
		}
	}

	private static void renderAllUnits(ActorRef out, GameState gameState) {
		for (GameUnit unit : gameState.getUnits()) {
			drawUnit(out, gameState, unit);
		}
	}

	private static void drawUnit(ActorRef out, GameState gameState, GameUnit unit) {
		Tile tile = gameState.getTile(unit.getBoardPosition().getX(), unit.getBoardPosition().getY());
		BasicCommands.drawUnit(out, unit, tile);
		refreshUnitStats(out, unit);
	}

	private static void refreshUnitStats(ActorRef out, GameUnit unit) {
		BasicCommands.setUnitAttack(out, unit, unit.getAttackValue());
		BasicCommands.setUnitHealth(out, unit, Math.max(0, unit.getHealthValue()));
	}

	private static void renderPlayerPanels(ActorRef out, GameState gameState) {
		syncPlayers(out, gameState);
	}

	private static void syncPlayers(ActorRef out, GameState gameState) {
		GamePlayer player1 = gameState.getPlayer1();
		GamePlayer player2 = gameState.getPlayer2();
		player1.syncUiPlayer();
		player2.syncUiPlayer();
		BasicCommands.setPlayer1Health(out, player1.getUiPlayer());
		BasicCommands.setPlayer2Health(out, player2.getUiPlayer());
		BasicCommands.setPlayer1Mana(out, player1.getUiPlayer());
		BasicCommands.setPlayer2Mana(out, player2.getUiPlayer());
	}

	private static void renderHumanHand(ActorRef out, GameState gameState) {
		GamePlayer human = gameState.getPlayer1();
		for (int position = 1; position <= 6; position++) {
			Card card = human.getCardAtPosition(position);
			if (card == null) {
				BasicCommands.deleteCard(out, position);
			} else {
				int mode = 0;
				if (gameState.getSelectedCardPosition() != null
						&& gameState.getSelectedCardPosition().intValue() == position) {
					mode = 1;
				}
				BasicCommands.drawCard(out, card, position, mode);
			}
		}
	}

	private static void renderTileHighlights(ActorRef out, GameState gameState) {
		Map<String, Integer> tileModes = new HashMap<String, Integer>();
		for (Tile tile : gameState.getTiles()) {
			tileModes.put(GameState.key(tile.getTilex(), tile.getTiley()), Integer.valueOf(0));
		}

		if (gameState.getSelectedCardPosition() != null) {
			Card selectedCard = gameState.getCurrentPlayer().getCardAtPosition(gameState.getSelectedCardPosition().intValue());
			if (selectedCard != null) {
				if (selectedCard.isCreature()) {
					for (BoardPosition position : getValidSummonTiles(gameState, selectedCard, gameState.getCurrentPlayer())) {
						tileModes.put(position.toKey(), Integer.valueOf(1));
					}
				} else {
					for (BoardPosition position : getValidSpellTargets(gameState, selectedCard, gameState.getCurrentPlayer())) {
						tileModes.put(position.toKey(), Integer.valueOf(2));
					}
				}
			}
		}

		if (gameState.getSelectedUnitId() != null) {
			GameUnit selectedUnit = gameState.getUnitById(gameState.getSelectedUnitId().intValue());
			if (selectedUnit != null) {
				tileModes.put(selectedUnit.getBoardPosition().toKey(), Integer.valueOf(1));
				for (BoardPosition position : getValidMoveTiles(gameState, selectedUnit)) {
					tileModes.put(position.toKey(), Integer.valueOf(1));
				}
				for (BoardPosition position : getValidAttackTargets(gameState, selectedUnit)) {
					tileModes.put(position.toKey(), Integer.valueOf(2));
				}
			}
		}

		for (Tile tile : gameState.getTiles()) {
			int mode = tileModes.get(GameState.key(tile.getTilex(), tile.getTiley())).intValue();
			BasicCommands.drawTile(out, tile, mode);
		}
	}

	private static void playEffectAtTile(ActorRef out, String effectConfig, Tile tile) {
		EffectAnimation effect = BasicObjectBuilders.loadEffect(effectConfig);
		if (effect != null && tile != null) {
			BasicCommands.playEffectAnimation(out, effect, tile);
		}
	}

	private static void playProjectile(ActorRef out, Tile startTile, Tile targetTile) {
		EffectAnimation effect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_projectiles);
		if (effect != null && startTile != null && targetTile != null) {
			BasicCommands.playProjectileAnimation(out, effect, 4, startTile, targetTile);
		}
	}

	private static void updateAvatarAttack(GamePlayer player) {
		int totalAttack = AVATAR_ATTACK + player.getArtifactAttackBonus();
		player.getAvatar().setAttackValue(totalAttack);
		player.syncUiPlayer();
	}

	private static boolean isHumanTurn(GameState gameState) {
		return gameState.getCurrentPlayerId() == 1;
	}

	private static void checkForGameOver(ActorRef out, GameState gameState) {
		if (gameState.isGameOver()) {
			return;
		}
		if (gameState.getPlayer1().getAvatar().isDead()) {
			gameState.setGameOver(true);
			gameState.setWinnerText("AI wins");
			BasicCommands.addPlayer1Notification(out, "AI wins", 4);
			BasicCommands.addPlayer2Notification(out, "AI wins", 4);
		} else if (gameState.getPlayer2().getAvatar().isDead()) {
			gameState.setGameOver(true);
			gameState.setWinnerText("Player 1 wins");
			BasicCommands.addPlayer1Notification(out, "You win", 4);
			BasicCommands.addPlayer2Notification(out, "Player 1 wins", 4);
		}
	}
}
