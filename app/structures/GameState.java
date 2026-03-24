package structures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import game.BoardPosition;
import game.GamePlayer;
import game.GameUnit;
import structures.basic.Tile;
import utils.BasicObjectBuilders;

/**
 * Runtime game state for the full match.
 */
public class GameState {

	public static final int BOARD_WIDTH = 9;
	public static final int BOARD_HEIGHT = 5;

	public boolean gameInitalised = false;
	public boolean something = false;

	private final Map<String, Tile> tiles;
	private final Map<Integer, GameUnit> unitsById;
	private final Map<String, Integer> occupancy;
	private final Random random;

	private GamePlayer player1;
	private GamePlayer player2;
	private int currentPlayerId;
	private int nextUnitId;
	private Integer selectedCardPosition;
	private Integer selectedUnitId;
	private boolean gameOver;
	private String winnerText;

	public GameState() {
		this.tiles = new LinkedHashMap<String, Tile>();
		this.unitsById = new LinkedHashMap<Integer, GameUnit>();
		this.occupancy = new LinkedHashMap<String, Integer>();
		this.random = new Random(34951000324L);
		resetBoard();
	}

	public void resetBoard() {
		tiles.clear();
		unitsById.clear();
		occupancy.clear();
		for (int x = 1; x <= BOARD_WIDTH; x++) {
			for (int y = 1; y <= BOARD_HEIGHT; y++) {
				Tile tile = BasicObjectBuilders.loadTile(x, y);
				tiles.put(key(x, y), tile);
			}
		}
		currentPlayerId = 1;
		nextUnitId = 1;
		selectedCardPosition = null;
		selectedUnitId = null;
		gameOver = false;
		winnerText = null;
	}

	public static String key(int x, int y) {
		return x + "," + y;
	}

	public Tile getTile(int x, int y) {
		return tiles.get(key(x, y));
	}

	public Collection<Tile> getTiles() {
		return tiles.values();
	}

	public Random getRandom() {
		return random;
	}

	public GamePlayer getPlayer1() {
		return player1;
	}

	public void setPlayer1(GamePlayer player1) {
		this.player1 = player1;
	}

	public GamePlayer getPlayer2() {
		return player2;
	}

	public void setPlayer2(GamePlayer player2) {
		this.player2 = player2;
	}

	public GamePlayer getCurrentPlayer() {
		return currentPlayerId == 1 ? player1 : player2;
	}

	public GamePlayer getOpponentPlayer() {
		return currentPlayerId == 1 ? player2 : player1;
	}

	public GamePlayer getPlayerById(int playerId) {
		return playerId == 1 ? player1 : player2;
	}

	public int getCurrentPlayerId() {
		return currentPlayerId;
	}

	public void setCurrentPlayerId(int currentPlayerId) {
		this.currentPlayerId = currentPlayerId;
	}

	public int nextUnitId() {
		int created = nextUnitId;
		nextUnitId++;
		return created;
	}

	public GameUnit getUnitById(int unitId) {
		return unitsById.get(unitId);
	}

	public GameUnit getUnitAt(int x, int y) {
		Integer unitId = occupancy.get(key(x, y));
		if (unitId == null) {
			return null;
		}
		return unitsById.get(unitId);
	}

	public boolean isOccupied(int x, int y) {
		return occupancy.containsKey(key(x, y));
	}

	public void addUnit(GameUnit unit, Tile tile) {
		unit.moveTo(tile);
		unitsById.put(unit.getId(), unit);
		occupancy.put(key(tile.getTilex(), tile.getTiley()), unit.getId());
	}

	public void moveUnit(GameUnit unit, Tile tile) {
		BoardPosition previous = unit.getBoardPosition();
		occupancy.remove(previous.toKey());
		unit.moveTo(tile);
		occupancy.put(key(tile.getTilex(), tile.getTiley()), unit.getId());
	}

	public void removeUnit(GameUnit unit) {
		occupancy.remove(unit.getBoardPosition().toKey());
		unitsById.remove(unit.getId());
		if (selectedUnitId != null && selectedUnitId.intValue() == unit.getId()) {
			selectedUnitId = null;
		}
	}

	public Collection<GameUnit> getUnits() {
		return unitsById.values();
	}

	public List<GameUnit> getUnitsForPlayer(int playerId) {
		List<GameUnit> units = new ArrayList<GameUnit>();
		for (GameUnit unit : unitsById.values()) {
			if (unit.getOwnerId() == playerId) {
				units.add(unit);
			}
		}
		return units;
	}

	public Integer getSelectedCardPosition() {
		return selectedCardPosition;
	}

	public void setSelectedCardPosition(Integer selectedCardPosition) {
		this.selectedCardPosition = selectedCardPosition;
	}

	public Integer getSelectedUnitId() {
		return selectedUnitId;
	}

	public void setSelectedUnitId(Integer selectedUnitId) {
		this.selectedUnitId = selectedUnitId;
	}

	public void clearSelection() {
		selectedCardPosition = null;
		selectedUnitId = null;
	}

	public boolean isGameOver() {
		return gameOver;
	}

	public void setGameOver(boolean gameOver) {
		this.gameOver = gameOver;
	}

	public String getWinnerText() {
		return winnerText;
	}

	public void setWinnerText(String winnerText) {
		this.winnerText = winnerText;
	}
}
