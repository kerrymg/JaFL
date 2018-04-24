package flands;


import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

/**
 * The set of curses on the character. Includes methods for adding, matching and
 * removing curses from the list.
 * 
 * @see Curse
 * @author Jonathan Mann
 */
public class CurseList extends AbstractListModel implements MouseListener, ActionListener, XMLOutput {
	private List<Curse> curses;
	private Adventurer owner;

	CurseList(Adventurer owner) {
		this.owner = owner;
		curses = new ArrayList<>();
	}

	private Adventurer getAdventurer() { return owner; }
	private EffectSet getEffects() { return getAdventurer().getEffects(); }

	@Override
	public int getSize() { return curses.size(); }
	private Curse getCurse(int i) { return curses.get(i); }
	@Override
	public Object getElementAt(int i) { return getCurse(i); }

	void addCurse(Curse c) {
		if (!curses.contains(c)) {
			if ((c.getType() == Curse.DISEASE_TYPE || c.getType() == Curse.POISON_TYPE) &&
				getAdventurer().getBlessings().hasBlessing(Blessing.DISEASE)) {
				// Don't add the curse, but remove the blessing
				System.out.println("Immunity cancels this curse");
				getAdventurer().getBlessings().removeBlessing(Blessing.DISEASE);
				return;
			}
			curses.add(c);
			Effect effect = c.getEffects();
			while (effect != null) {
				if (effect instanceof AbilityEffect) {
					AbilityEffect ae = (AbilityEffect)effect;
					getEffects().addStatRelated(ae.getAbility(), c, effect);
				}
				effect = effect.nextEffect();
			}

			getEffects().notifyOwner();
			fireIntervalAdded(this, getSize() - 1, getSize() - 1);
		}
		else {
			System.out.println("Trying for cumulative curse");
			Curse thisc = curses.get(curses.indexOf(c));
			if (thisc.isCumulative()) {
				thisc.addCurse(c);
				getEffects().notifyEffectsUpdated(thisc);
			}
		}
	}

	void removeCurse(int index) {
		if (index < getSize()) {
			Curse c = curses.remove(index);
			if (c.getItem() != null)
				getAdventurer().getItems().removeItem(c.getItem());
			Effect effect = c.getEffects();
			while (effect != null) {
				if (effect instanceof AbilityEffect) {
					AbilityEffect ae = (AbilityEffect)effect;
					getEffects().removeStatRelated(ae.getAbility(), c, effect);
				}
				effect = effect.nextEffect();
			}

			getEffects().notifyOwner();
			fireIntervalRemoved(this, index, index);
		}
	}

	void removeAll() {
		for (int i = getSize() - 1; i >= 0; i--)
			removeCurse(i);
	}

	public int[] findMatches(Curse match) {
		int[] matches = new int[getSize()];
		int count = 0;
		for (int i = 0; i < getSize(); i++) {
			Curse c = getCurse(i);
			if (match.matches(c)) {
				System.out.println("Curse " + i + "=" + c.getName() + " is matched");
				matches[count++] = i;
			}
		}

		if (count > 1 && configuredList != null && (match.getName() == null || !match.getName().equals("*"))) {
			// See if we can trim this down using selections
			int[] selections = configuredList.getSelectedIndices();
			int[] newMatches = new int[count];
			int newCount = 0;
			for (int i = 0; i < count; i++)
				if (Arrays.binarySearch(selections, matches[i]) >= 0)
					newMatches[newCount++] = matches[i];

			if (newCount > 0) {
				count = newCount;
				matches = newMatches;
			}
		}

		if (count < matches.length) {
			int[] temp = matches;
			matches = new int[count];
			System.arraycopy(temp, 0, matches, 0, count);
		}
		return matches;
	}

	private JList configuredList = null;
	void configureList(JList list) {
		if (list.getModel() instanceof CurseList) {
			CurseList oldModel = (CurseList)list.getModel();
			list.removeMouseListener(oldModel);
			oldModel.configuredList = null;
		}

		configuredList = list;
		list.setFont(SectionDocument.getPreferredFont());
		//list.setCellRenderer(new DocumentCellRenderer());
		list.setModel(this);
		list.addMouseListener(this);
	}

	@Override
	public void mouseEntered(MouseEvent evt) {}
	@Override
	public void mouseExited(MouseEvent evt) {}
	@Override
	public void mousePressed(MouseEvent evt) {
		if (evt.isPopupTrigger())
			handlePopup(evt);
	}
	@Override
	public void mouseReleased(MouseEvent evt) {
		if (evt.isPopupTrigger())
			handlePopup(evt);
	}
	private int getIndex(Point p) {
		int index = configuredList.locationToIndex(p);
		if (index >= 0 &&
			configuredList.getCellBounds(index, index).contains(p))
			return index;
		return -1;
	}
	@Override
	public void mouseClicked(MouseEvent evt) {
		if (evt.getClickCount() == 2) {
			int index = getIndex(evt.getPoint());
			if (index >= 0) {
				Curse c = getCurse(index);
				if (c.getLiftQuestion() != null)
					askLiftQuestion(index, c);
			}
		}
	}
	private int currentIndex = -1;
	private Curse currentCurse = null;
	private void handlePopup(MouseEvent evt) {
		int index = getIndex(evt.getPoint());
		if (index >= 0) {
			Curse c = getCurse(index);
			if (c != null && c.getLiftQuestion() != null) {
				currentIndex = index;
				currentCurse = c;
				JPopupMenu menu = new JPopupMenu();
				JMenuItem liftItem = new JMenuItem("Lift curse...");
				liftItem.addActionListener(this);
				menu.add(liftItem);
				menu.show(configuredList, evt.getX(), evt.getY());
			}
		}
	}
	@Override
	public void actionPerformed(ActionEvent evt) {
		askLiftQuestion(currentIndex, currentCurse);
		currentIndex = -1;
		currentCurse = null;
	}
	private void askLiftQuestion(int index, Curse curse) {
		int result = JOptionPane.showConfirmDialog(configuredList, curse.getLiftQuestion(), "Lift Curse?", JOptionPane.YES_NO_OPTION);
		if (result == JOptionPane.YES_OPTION)
			removeCurse(index);
	}

	@Override
	public String getXMLTag() {
		return "curses";
	}

	@Override
	public void storeAttributes(Properties atts, int flags) {}

	@Override
	public Iterator<XMLOutput> getOutputChildren() {
		LinkedList<XMLOutput> l = new LinkedList<>();
		for (int i = 0; i < getSize(); i++) {
			Curse c = getCurse(i);
			if (c.getItem() == null)
				l.add(c);
		}
		return l.iterator();
	}

	@Override
	public void outputTo(PrintStream out, String indent, int flags) throws IOException {
		Node.output(this, out, indent, flags);
	}
}
