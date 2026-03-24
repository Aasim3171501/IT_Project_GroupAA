package game;

import java.util.Objects;

import structures.basic.Tile;

/**
 * Immutable board coordinate.
 */
public final class BoardPosition {

	private final int x;
	private final int y;

	public BoardPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public static BoardPosition fromTile(Tile tile) {
		return new BoardPosition(tile.getTilex(), tile.getTiley());
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public BoardPosition offset(int dx, int dy) {
		return new BoardPosition(x + dx, y + dy);
	}

	public boolean isInsideBoard(int width, int height) {
		return x >= 1 && x <= width && y >= 1 && y <= height;
	}

	public int manhattanDistance(BoardPosition other) {
		return Math.abs(x - other.x) + Math.abs(y - other.y);
	}

	public boolean isAdjacent(BoardPosition other) {
		int dx = Math.abs(x - other.x);
		int dy = Math.abs(y - other.y);
		return (dx <= 1 && dy <= 1) && !(dx == 0 && dy == 0);
	}

	public String toKey() {
		return x + "," + y;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof BoardPosition)) {
			return false;
		}
		BoardPosition that = (BoardPosition) other;
		return x == that.x && y == that.y;
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y);
	}

	@Override
	public String toString() {
		return "BoardPosition{" + "x=" + x + ", y=" + y + '}';
	}
}
