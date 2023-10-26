/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package interfaces;

import java.util.*;
import core.*;
import movement.MovementEngine;
import movement.MovementModel;

/**
 * <P>
 * Overlay grid of the world where each interface is put on a cell depending
 * of its location. This is used in cell-based optimization of connecting
 * the interfaces.</P>
 *
 * <P>The idea in short:<BR>
 * Instead of checking for every interface if some of the other interfaces are close
 * enough (this approach obviously doesn't scale) we check only interfaces that
 * are "close enough" to be possibly connected. Being close enough is
 * determined by keeping track of the approximate location of the interfaces
 * by storing them in overlay grid's cells and updating the cell information
 * every time the interfaces move. If two interfaces are in the same cell or in
 * neighboring cells, they have a chance of being close enough for
 * connection. Then only that subset of interfaces is checked for possible
 * connectivity.
 * </P>
 * <P>
 * <strong>Note:</strong> this class does NOT support negative
 * coordinates. Also, it makes sense to normalize the coordinates to start
 * from zero to conserve memory.
 */
public class ConnectivityGrid extends ConnectivityOptimizer {

	/**
	 * Cell based optimization cell size multiplier -setting id ({@value}).
	 * Used in {@link World#OPTIMIZATION_SETTINGS_NS} name space.
	 * Single ConnectivityCell's size is the biggest radio range times this.
	 * Larger values save memory and decrease startup time but may result in
	 * slower simulation.
	 * Default value is {@link #DEF_CON_CELL_SIZE_MULT}.
	 * Smallest accepted value is 1.
	 */
	public static final String CELL_SIZE_MULT_S = "cellSizeMult";
	/** default value for cell size multiplier ({@value}) */
	public static final int DEF_CON_CELL_SIZE_MULT = 5;

	private GridCell[][] cells;
	private List<NetworkInterface> interfaces;
	private HashMap<NetworkInterface, GridCell> ginterfaces;
	private int cellSize;
	private int rows;
	private int cols;
	private int worldSizeX;
	private int worldSizeY;
	private int cellSizeMultiplier;

	/**
	 * Creates a new ConnectivityGrid
	 * @param interfaces The interfaces to be managed by this grid
	 */
	public ConnectivityGrid(List<NetworkInterface> interfaces) {
		Settings s = new Settings(MovementModel.MOVEMENT_MODEL_NS);
		int [] worldSize = s.getCsvInts(MovementModel.WORLD_SIZE,2);
		worldSizeX = worldSize[0];
		worldSizeY = worldSize[1];

		s.setNameSpace(World.OPTIMIZATION_SETTINGS_NS);
		if (s.contains(CELL_SIZE_MULT_S)) {
			cellSizeMultiplier = s.getInt(CELL_SIZE_MULT_S);
		}
		else {
			cellSizeMultiplier = DEF_CON_CELL_SIZE_MULT;
		}
		if (cellSizeMultiplier < 1) {
			throw new SettingsError("Too small value (" + cellSizeMultiplier +
					") for " + World.OPTIMIZATION_SETTINGS_NS +
					"." + CELL_SIZE_MULT_S);
		}

		double maxRange = Double.MIN_VALUE;
		for (NetworkInterface ni : interfaces) {
			maxRange = Math.max(maxRange, ni.getTransmitRange());
		}

		this.interfaces = interfaces;
		this.cellSize = (int)Math.ceil(maxRange * cellSizeMultiplier);
		this.rows = worldSizeY/cellSize + 1;
		this.cols = worldSizeX/cellSize + 1;
		// leave empty cells on both sides to make neighbor search easier
		this.cells = new GridCell[rows+2][cols+2];

		for (int i=0; i<rows+2; i++) {
			for (int j=0; j<cols+2; j++) {
				this.cells[i][j] = new GridCell();
			}
		}
		this.ginterfaces = new HashMap<>();
	}

	/**
	 * Detects interfaces which are in range/no longer in range of each other
	 * Issues LinkUp/LinkDown events to the corresponding interfaces
	 */
	@Override
	public void detectConnectivity() {
		// Update all interface positions
		long time_default_opt_updateInterfaceLocations = System.nanoTime();
		for (NetworkInterface ni : interfaces) {
			updateLocation(ni);
		}
		System.out.printf(" %d:  default_opt.updateInterfaceLocations = %d\n", MovementEngine.getCurrentTick(), System.nanoTime() - time_default_opt_updateInterfaceLocations);

		// Detect link events
		long time_default_opt_detectLinkEvents = System.nanoTime();
		for (NetworkInterface ni : interfaces) {
			// Issue LinkDown Events
			List<Connection> connections = ni.getConnections();
			for (int i = 0; i < connections.size(); ) {
				Connection con = connections.get(i);
				NetworkInterface other = con.getOtherInterface(ni);

				// all connections should be up at this stage
				assert con.isUp() : "Connection " + con + " was down!";

				if (!areWithinRange(ni, other)) {
					ni.linkDown(other);
				}
				else {
					i++;
				}
			}

			// Issue LinkUp Events
			for (NetworkInterface i : getInterfacesInRange(ni)) {
				if (ni != i && !ni.isConnected(i)) {
					ni.linkUp(i);
				}
			}
		}
		System.out.printf(" %d:  default_opt.detectLinkEvents = %d\n", MovementEngine.getCurrentTick(), System.nanoTime() - time_default_opt_detectLinkEvents);
	}

	/**
	 * Checks and updates (if necessary) interface's position in the grid
	 * @param ni The interface to update
	 */
	private void updateLocation(NetworkInterface ni) {
		GridCell oldCell = ginterfaces.get(ni);
		GridCell newCell = cellFromCoord(ni.getLocation());

		if (oldCell == null) { // This interface is new
			newCell.addInterface(ni);
			ginterfaces.put(ni, newCell);
		} else if (newCell != oldCell) {
			oldCell.moveInterface(ni, newCell);
			ginterfaces.put(ni,newCell);
		}
	}

	/**
	 * Finds all neighboring cells and the cell itself based on the coordinates
	 * @param c The coordinates
	 * @return Array of neighboring cells
	 */
	private GridCell[] getNeighborCellsByCoord(Coord c) {
		// +1 due empty cells on both sides of the matrix
		int row = (int)(c.getY()/cellSize) + 1;
		int col = (int)(c.getX()/cellSize) + 1;
		return getNeighborCells(row,col);
	}

	/**
	 * Returns an array of Cells that contains the neighbors of a certain
	 * cell and the cell itself.
	 * @param row Row index of the cell
	 * @param col Column index of the cell
	 * @return Array of neighboring Cells
	 */
	private GridCell[] getNeighborCells(int row, int col) {
		return new GridCell[] {
			cells[row-1][col-1],cells[row-1][col],cells[row-1][col+1],//1st row
			cells[row][col-1],cells[row][col],cells[row][col+1],//2nd row
			cells[row+1][col-1],cells[row+1][col],cells[row+1][col+1]//3rd row
		};
	}

	/**
	 * Get the cell having the specific coordinates
	 * @param c Coordinates
	 * @return The cell
	 */
	private GridCell cellFromCoord(Coord c) {
		// +1 due empty cells on both sides of the matrix
		int row = (int)(c.getY()/cellSize) + 1;
		int col = (int)(c.getX()/cellSize) + 1;

		assert row > 0 && row <= rows && col > 0 && col <= cols : "Location " +
		c + " is out of world's bounds";

		return this.cells[row][col];
	}

	/**
	 * Returns all interfaces that are in range (i.e., in neighboring grid cells)
	 * and use the same technology and channel as the given interface
	 * @param ni The interface whose neighboring interfaces are returned
	 * @return Set of in range interfaces
	 */
	private Collection<NetworkInterface> getInterfacesInRange(NetworkInterface ni) {
		updateLocation(ni);

		List<NetworkInterface> niList = new ArrayList<>();
		GridCell loc = ginterfaces.get(ni);

		if (loc != null) {
			GridCell[] neighbors = getNeighborCellsByCoord(ni.getLocation());
            for (GridCell neighbor : neighbors) {
				for (NetworkInterface nni : neighbor.getInterfaces()) {
					if (areWithinRange(ni, nni)) {
						niList.add(nni);
					}
				}
            }
		}

		return niList;
	}

	/**
	 * Returns a string representation of the ConnectivityCells object
	 * @return a string representation of the ConnectivityCells object
	 */
	public String toString() {
		return getClass().getSimpleName() + " of size " +
			this.cols + "x" + this.rows + ", cell size=" + this.cellSize;
	}

	/**
	 * A single cell in the cell grid. Contains the interfaces that are
	 * currently in that part of the grid.
	 */
	public class GridCell {
		// how large array is initially chosen
		private static final int EXPECTED_INTERFACE_COUNT = 5;
		private ArrayList<NetworkInterface> interfaces;

		private GridCell() {
			this.interfaces = new ArrayList<NetworkInterface>(
					EXPECTED_INTERFACE_COUNT);
		}

		/**
		 * Returns a list of of interfaces in this cell
		 * @return a list of of interfaces in this cell
		 */
		public ArrayList<NetworkInterface> getInterfaces() {
			return this.interfaces;
		}

		/**
		 * Adds an interface to this cell
		 * @param ni The interface to add
		 */
		public void addInterface(NetworkInterface ni) {
			this.interfaces.add(ni);
		}

		/**
		 * Removes an interface from this cell
		 * @param ni The interface to remove
		 */
		public void removeInterface(NetworkInterface ni) {
			this.interfaces.remove(ni);
		}

		/**
		 * Moves a interface in a Cell to another Cell
		 * @param ni The interface to move
		 * @param to The cell where the interface should be moved to
		 */
		public void moveInterface(NetworkInterface ni, GridCell to) {
			to.addInterface(ni);
			boolean removeOk = this.interfaces.remove(ni);
			assert removeOk : "interface " + ni +
				" not found from cell with " + interfaces.toString();
		}

		/**
		 * Returns a string representation of the cell
		 * @return a string representation of the cell
		 */
		public String toString() {
			return getClass().getSimpleName() + " with " +
				this.interfaces.size() + " interfaces :" + this.interfaces;
		}
	}

}
