package com.WordFinder;

import android.util.Log;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Observable;

import com.WordFinder.Tile.State;

/**
 *  Square grid of Tiles representing the game board
 *
 * @author John Mooring (jmooring)
 * @author Christopher Buck (cmbuck)
 * @author Bryan Malyn (bmalyn)
 * @version 2011.11.29
 */
public class LetterGrid extends Observable {

	private Tile[][] grid;
	private ArrayList<Tile> path;
	private TreeSet<String> foundWords;
	private boolean wordSubmitted;

	/**
	 * Builds a LetterGrid
	 */
	public LetterGrid() {
		path = new ArrayList<Tile>();
		wordSubmitted = false;
		foundWords = new TreeSet<String>();
	}

	/**
	 * Generates and loads a square grid of characters with a side length of the
	 * passed integer
	 *
	 * @param size
	 *            the side length of the square character grid
	 */
	public void loadRandom(int size) {
		char[][] lettersGrid = new char[size][size];
		String letters = "";
		int length = size * size;
		int hardLetters = ((int) (Math.random() * length * .1875 + .5));
		int vowles = (int) (Math.random() * (length * .4375 - length / 4.0 + 1) + length / 4.0);
		int regLetters = length - vowles - hardLetters;
		String[] letterTypes = { "bcdfghklmnprstwy", "aeiou", "jqkzxv", };
		int numOfLetterTypes[] = { regLetters, vowles, hardLetters };

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < numOfLetterTypes[i]; j++) {
				int insertHere = (int) (letters.length() * Math.random());
				letters = letters.substring(0, insertHere)
						+ letterTypes[i]
								.charAt(((int) (letterTypes[i].length() * Math
										.random())))
						+ letters.substring(insertHere);
			}
		}
		int loopCount = 0;
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				lettersGrid[i][j] = letters.charAt(loopCount);
				loopCount++;
			}
		}
		load(lettersGrid);
	}

	/**
	 * Creates the grid from the passed array of letters. Characters are
	 * transformed into Tiles, and the tiles are given an array of all adjacent
	 * tiles.
	 *
	 * @precondition letters is square
	 * @param letters
	 *            the letters to load
	 */
	public void load(char[][] letters) {
		this.grid = new Tile[letters.length][letters[0].length];
		for (int i = 0; i < letters.length; i++) {
			for (int j = 0; j < letters[i].length; j++) {
				this.grid[i][j] = new Tile(i, j, letters[i][j]);
			}
		}
		for (int i = 0; i < grid.length; i++) {
			for (int j = 0; j < grid[i].length; j++) {
				for (int di = i - 1; di <= i + 1; di++) {
					for (int dj = j - 1; dj <= j + 1; dj++) {
						if (di >= 0 && di < grid.length && dj >= 0
								&& dj < grid[i].length && !(di == i && dj == j)) {
							grid[i][j].addAdjascent(grid[di][dj]);
						}
					}
				}
			}
		}
		deselectPath();
		foundWords.clear();
		WordSolver.getInstance().solve(this);
		updateAll();
	}

	/**
	 * If the tile is valid, (based on the last selected tile) the tile is added
	 * to the current path list and has its state changed to DOWN
	 * @param t the tile to select
	 */
	public void setSelected(Tile t) {
		if (wordSubmitted) {
			deselectPath();
			wordSubmitted = false;
		}
		if (path.isEmpty()
				|| (path.get(path.size() - 1).getAdjascent().contains(t) && !path
						.contains(t))) {
			path.add(t);
			t.setState(State.DOWN);
			setChanged();
			notifyObservers();

		} else if (path.size() >= 2 && path.get(path.size() - 2).equals(t)) {

			path.get(path.size() - 1).setState(State.UP);
			path.remove(path.size() - 1);
			setChanged();
			notifyObservers();
			String pathString = "";
			for (Tile t2 : path) {
				pathString += t2.getLetter() + "";
			}
			Log.d("error", pathString);
		}
	}

	/**
	 * Returns the words found by the user.
	 * @return set of words found by the user
	 */
	public Set<String> getFoundWords() {
		return foundWords;
	}

	/**
	 * Returns the calculated points based on words found by the user
	 *
	 * @return the users points
	 */
	public int getPoints() {
		int points = 0;
		for (String s : getFoundWords()) {
			points += s.length() - 2;
		}
		return points;
	}

	/**
	 * Returns the sum of the point values of all words on the grid
	 *
	 * @return possible points
	 */
	public int getPossiblePoints() {
		int points = 0;
		for (String s : WordSolver.getInstance().getWords()) {
			points += s.length() - 2;
		}
		return points;
	}

	/**
	 * Returns the currently selected path
	 * @return ArrayList containing the path
	 */
	public ArrayList<Tile> getPath() {
		return path;
	}

	/**
	 * Called when tile selection is done and the word is submitted. Uses
	 * WordSolver to check if the word contained in the current path is valid
	 * Sets the tiles in the path as good or bad depending on whether the word
	 * was good or bad. Notifies Observers.
	 *
	 * @precondition the contained in the current path is valid
	 */
	public void submitWord() {
		String s = "";
		for (Tile t : path) {
			s += t.getLetter();
		}
		s = s.replace("q", "qu");
		Log.d("word", s);
		if (WordSolver.getInstance().isWord(s)) {
			if (foundWords.contains(s)) {
				for (Tile t : path) {
					t.setState(State.DUPE);
				}
			} else {
				foundWords.add(s);
				for (Tile t : path) {
					t.setState(State.GOOD);
				}
			}
		} else {
			for (Tile t : path) {
				t.setState(State.BAD);
			}
		}
		wordSubmitted = true;
		setChanged();
		notifyObservers();
	}


	/**
	 * Place a description of your method here.
	 *
	 * @param x
	 * @param y
	 * @return tile at the passed coordinates
	 */
	public Tile getTile(int x, int y) {
		return grid[x][y];
	}

	/**
	 * Clears the current path, sets all tiles in the path to their default
	 * color. Notifies Observers.
	 */
	public void deselectPath() {
		for (Tile t : path) {
			t.setState(State.UP);
		}
		path.clear();
		setChanged();
		notifyObservers();
	}

	/**
	 * Returns the side length of the square grid
	 * @return side length of square grid
	 */
	public int size() {
		return grid.length;
	}

	/**
	 * Updates observers.
	 */
	public void updateAll() {
		setChanged();
		notifyObservers();
	}

}
