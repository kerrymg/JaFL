package flands;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

/**
 * Handles the set of all ships owned by the character. Includes methods for modifying one
 * or more ships, plus mouse event handlers for the list view.
 * 
 * @author Jonathan Mann
 */
public class ShipList extends AbstractTableModel implements Loadable, MouseListener, ActionListener {
	private List<Ship> ships = new ArrayList<>();
	private static JMenuItem transferItem;
	static {
		transferItem = new JMenuItem("Ship Transfer...");
		transferItem.addActionListener(new TransferListener());
		transferItem.setEnabled(false);
	}

	int getShipCount() { return ships.size(); }
	Ship getShip(int i) { return ships.get(i); }

	public void addShip(Ship s) {
		ships.add(s);
		fireTableRowsInserted(ships.size() - 1, ships.size() - 1);
		if (s.getName() == null && table != null)
			table.editCellAt(ships.size() - 1, Ship.NAME_COLUMN);
		notifyShipListeners();

		if (getShipCount() == 1)
			FLApp.getSingle().showShipWindow();
		checkTransferItem(this);
	}
	void removeShip(int index) {
		ships.remove(index);
		fireTableRowsDeleted(index, index);
		notifyShipListeners();
		checkTransferItem(this);
	}

	private String dock = null;
	public void setAtDock(String dock) {
		this.dock = dock;
		System.out.println("Current dock location=" + dock);
		for (int s = 0; s < getShipCount(); s++) {
			if (getShip(s).getDocked() == null)
				// We'll presume the user just sailed this ship into dock
				getShip(s).setDocked(dock);
		}
		fireTableDataChanged();
		notifyShipListeners();
		checkTransferItem(this);
	}
	void setOnLand() {
		setAtDock("*land*");
	}
	void setAtSea() {
		setAtDock(null);
	}
	public boolean isOnLand() { return dock.equals("*land*"); }
	public boolean isAtSea() { return dock == null; }
	public void refresh() {
		fireTableDataChanged();
		checkTransferItem(this);
	}

	private boolean isHere(Ship s) {
		String shipDock = s.getDocked();
		return dock == shipDock ||
				(dock != null && dock.equalsIgnoreCase(shipDock));
	}

	private int[] listToArray(List<Integer> l) {
		if (l.size() > 1)
			// If the user has selected one of these possible ships, return only that one
			if (table != null && l.contains(table.getSelectedRow()))
				return new int[] { table.getSelectedRow() };

		int[] result = new int[l.size()];
		for (int i = 0; i < l.size(); i++)
			result[i] = l.get(i);
		return result;		
	}

	int[] findShipsOfType(int shipType) {
		List<Integer> l = new ArrayList<>();
		for (int s = 0; s < ships.size(); s++) {
			Ship ship = getShip(s);
			if (isHere(ship) && ship.getType() == shipType)
				l.add(s);
		}

		return listToArray(l);
	}

	public int[] findShipsWithSpace() { return findShipsWithCargo(Ship.NO_CARGO); }
	public int[] findShipsWithCargo(int cargoType) {
		List<Integer> l = new ArrayList<>();
		for (int s = 0; s < ships.size(); s++) {
			Ship ship = getShip(s);
			if (isHere(ship) && ship.hasCargo(cargoType))
				l.add(s);
		}

		return listToArray(l);
	}

	public void addCargoTo(int shipIndex, int cargoType) {
		Ship s = getShip(shipIndex);
		if (s.addCargo(cargoType)) {
			fireTableCellUpdated(shipIndex, Ship.CARGO_COLUMN);
			notifyCargoListeners();
		}
	}
	/*
	public boolean addCargo(int cargoType) {
		int[] possibleShips = findShipsWithSpace();
		if (possibleShips.length == 1) {
			getShip(possibleShips[0]).addCargo(cargoType);
			fireTableCellUpdated(possibleShips[0], Ship.CARGO_COLUMN);
			return true;
		}
		else
			return false;
	}
	*/

	public void removeCargoFrom(int shipIndex, int cargoType) {
		Ship s = getShip(shipIndex);
		if (s.removeCargo(cargoType)) {
			fireTableCellUpdated(shipIndex, Ship.CARGO_COLUMN);
			notifyCargoListeners();
		}
	}
/*
	public boolean removeCargo(int cargoType) {
		int[] possibleShips = findShipsWithCargo(cargoType);
		if (possibleShips.length == 1) {
			getShip(possibleShips[0]).removeCargo(cargoType);
			fireTableCellUpdated(possibleShips[0], Ship.CARGO_COLUMN);
			return true;
		}
		else
			return false;
	}
*/
	int[] findShipsWithCrew(int crewType) {
		List<Integer> l = new ArrayList<>();
		for (int s = 0; s < ships.size(); s++) {
			Ship ship = getShip(s);
			if (isHere(ship) && ship.getCrew() == crewType)
				l.add(s);
		}

		return listToArray(l);
	}

	void setCrew(int shipIndex, int toCrewType) {
		Ship s = getShip(shipIndex);
		s.setCrew(toCrewType);
		fireTableCellUpdated(shipIndex, Ship.CREW_COLUMN);
		notifyCrewListeners();
	}
	
	/**
	 * Return the index of the only ship that is here, or the one that is selected.
	 * @return <code>-1</code> if no single ship could be picked.
	 */
	int getSingleShip() {
		int singleIndex = -1;
		for (int i = 0; i < ships.size(); i++) {
			if (isHere(getShip(i))) {
				if (singleIndex < 0)
					singleIndex = i;
				else {
					singleIndex = -1;
					break;
				}
			}
		}

		if (singleIndex < 0 && table != null) {
			singleIndex = table.getSelectedRow();
			if (singleIndex >= 0 && !isHere(getShip(singleIndex)))
				singleIndex = -1;
		}

		return singleIndex;
	}

	int[] findShipsHere() {
		List<Integer> l = new ArrayList<>();
		for (int s = 0; s < ships.size(); s++) {
			Ship ship = getShip(s);
			if (isHere(ship))
				l.add(s);
		}

		return listToArray(l);
	}

	private static List<ChangeListener> addListenerTo(List<ChangeListener> listeners, ChangeListener l) {
		if (listeners == null)
			listeners = new LinkedList<>();
		listeners.add(l);
		return listeners;
	}
	private static void removeListenerFrom(List<ChangeListener> listeners, ChangeListener l) {
		if (listeners != null)
			listeners.remove(l);
		else {
			System.out.println("ShipList.removeListenerFrom called unnecessarily");
			Thread.dumpStack();
		}
	}
	private void notifyListeners(List<ChangeListener> listeners) {
		if (listeners != null && listeners.size() > 0) {
			ChangeEvent evt = new ChangeEvent(this);
			for (ChangeListener listener : listeners)
				listener.stateChanged(evt);
		}
	}

	/******** Listeners ********/
	private List<ChangeListener> crewListeners = null;
	private List<ChangeListener> cargoListeners = null;
	private List<ChangeListener> shipListeners = null;
	void addShipListener(ChangeListener l) { shipListeners = addListenerTo(shipListeners, l); }
	void removeShipListener(ChangeListener l) { removeListenerFrom(shipListeners, l); }
	private void notifyShipListeners() {
		notifyListeners(shipListeners);
		notifyListeners(crewListeners);
		notifyListeners(cargoListeners);
	}
	void addCrewListener(ChangeListener l) { crewListeners = addListenerTo(crewListeners, l); }
	void removeCrewListener(ChangeListener l) { removeListenerFrom(crewListeners, l); }
	private void notifyCrewListeners() { notifyListeners(crewListeners); }
	void addCargoListener(ChangeListener l) { cargoListeners = addListenerTo(cargoListeners, l); }
	void removeCargoListener(ChangeListener l) { removeListenerFrom(cargoListeners, l); }
	private void notifyCargoListeners() { notifyListeners(cargoListeners); }

	/******** TableModel ********/
	@Override
	public int getRowCount() { return getShipCount(); }
	@Override
	public int getColumnCount() { return Ship.getColumnCount(); }
	@Override
	public String getColumnName(int col) { return Ship.getColumnName(col); }
	@Override
	public Class<?> getColumnClass(int col) { return Ship.getColumnClass(col); }
	@Override
	public boolean isCellEditable(int row, int col) {
		return Ship.isCellEditable(col) && isHere(getShip(row));
	}
	@Override
	public Object getValueAt(int row, int col) {
		return getShip(row).getValueAt(col);
	}
	@Override
	public void setValueAt(Object val, int row, int col) {
		if (getShip(row).setValueAt(val, col))
			fireTableCellUpdated(row, col);
	}

	/******** Table methods ********/
	private JTable table = null;
	public JTable getTable() {
		if (table == null) {
			table = new JTable(this) {
				@Override
				public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
					Component c = super.prepareRenderer(renderer, row, col);
					c.setEnabled(isHere(getShip(row)));
					if (getColumnModel().getColumn(col).getModelIndex() == Ship.NAME_COLUMN) {
						Font f = c.getFont();
						c.setFont(new Font(f.getName(), f.getStyle() | Font.ITALIC, f.getSize()));
					}

					return c;
				}
			};
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			//table.getSelectionModel().addListSelectionListener(this);
			table.setShowHorizontalLines(false);
			table.setShowVerticalLines(true);
			table.setPreferredScrollableViewportSize(new Dimension(300, 50));
			table.addMouseListener(this);
		}
		return table;
	}

	/******** Loadable methods ********/
	@Override
	public String getFilename() { return "ships.dat"; }
	@Override
	public boolean loadFrom(InputStream in) throws IOException {
		DataInputStream din = new DataInputStream(in);
		dock = din.readUTF();
		if (dock.length() == 0)
			dock = null;
		int count = din.readInt();
		ships.clear();
		for (int s = 0; s < count; s++) {
			Ship ship = Ship.load(din);
			addShip(ship);
		}
		return true;
	}

	@Override
	public boolean saveTo(OutputStream out) throws IOException {
		DataOutputStream dout = new DataOutputStream(out);
		dout.writeUTF(dock == null ? "" : dock);
		dout.writeInt(getShipCount());
		for (int s = 0; s < getShipCount(); s++)
			getShip(s).saveTo(dout);
		return true;
	}

	static JMenuItem getTransferMenuItem() { return transferItem; }

	private static void checkTransferItem(ShipList ships) {
		transferItem.setEnabled(ships != null && ships.findShipsHere().length > 1);
	}

	private static class TransferListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			ShipFrame window = FLApp.getSingle().showShipWindow();
			new ShipSwapDialog(window, FLApp.getSingle().getAdventurer().getShips()).setVisible(true);
		}
	}

	private int selectedShip = -1;
	private void handlePopup(MouseEvent evt) {
		if (table == null) return;
		int row = table.rowAtPoint(evt.getPoint());
		if (row < 0) return;
		selectedShip = row;
		Ship s = getShip(row);
		JPopupMenu popup = new JPopupMenu(s.getName());
		JMenuItem dumpItem = new JMenuItem("Dump Cargo...");
		if (!isHere(s) || !s.hasCargo(Ship.MATCH_SINGLE_CARGO))
			dumpItem.setEnabled(false);
		dumpItem.addActionListener(this);
		popup.add(dumpItem);
		popup.show(table, evt.getX(), evt.getY());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// Dump Cargo selected
		if (selectedShip >= 0) {
			Ship s = getShip(selectedShip);
			int[] cargoTypes = new int[s.getCapacity() - s.getFreeSpace()];
			int len = 0;
			for (int i = 0; i < s.getCapacity(); i++) {
				int type = s.getCargo(i);
				if (type != Ship.NO_CARGO)
					cargoTypes[len++] = type;
			}
			
			DefaultStyledDocument[] cargoDocs = new DefaultStyledDocument[len];
			for (int i = 0; i < len; i++) {
				cargoDocs[i] = new DefaultStyledDocument();
				try {
					cargoDocs[i].insertString(0, Ship.getCargoName(cargoTypes[i]), null);
				}
				catch (BadLocationException ignored) {}
			}
			
			int[] selected = DocumentChooser.showChooser(SwingUtilities.getWindowAncestor(table), "Dump Cargo", cargoDocs, true);
			if (selected != null && selected.length > 0) {
				for (int i = selected.length - 1; i >= 0; i--)
					removeCargoFrom(selectedShip, cargoTypes[selected[i]]);
			}
		}
		selectedShip = -1;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		if (e.isPopupTrigger())
			handlePopup(e);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.isPopupTrigger())
			handlePopup(e);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.isPopupTrigger())
			handlePopup(e);
	}

	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}
}
