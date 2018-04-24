package flands;


import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Element;

import org.xml.sax.Attributes;

/**
 * Limits the number of items, belonging to a named group, that can be taken
 * in one section. Apparently unused.
 * @author Jonathan Mann
 */
public class ItemGroupNode extends Node {
	public static String ElementName = "items";

	private static List<ItemGroupNode> groupNodes;
	private static void addGroupNode(ItemGroupNode node) {
		if (groupNodes == null)
			groupNodes = new LinkedList<>();
		groupNodes.add(node);
	}

	static ItemGroupNode getGroupNode(String name) {
		if (groupNodes != null) {
			for (ItemGroupNode n : groupNodes) {
				if (n.name.equals(name))
					return n;
			}
		}
		return null;
	}

	private static void removeGroupNode(ItemGroupNode node) {
		groupNodes.remove(node);
	}

	private String name;
	private int limit = 0;

	ItemGroupNode(Node parent) {
		super(ElementName, parent);
		addGroupNode(this);
	}

	@Override
	public void init(Attributes atts) {
		name = atts.getValue("group");
		limit = getIntValue(atts, "limit", 0);
	}

	public int getLimit() { return limit; }
	void adjustLimit(int delta) {
		limit += delta;
		if (delta != 0)
			fireChangeEvent();
	}

	private List<ChangeListener> listeners;
	public void addChangeListener(ChangeListener l) {
		if (listeners == null)
			listeners = new LinkedList<>();
		listeners.add(l);
	}
	public void removeChangeListener(ChangeListener l) {
		listeners.remove(l);
	}
	private void fireChangeEvent() {
		if (listeners != null) {
			ChangeEvent e = new ChangeEvent(this);
			for (ChangeListener listener : listeners)
				listener.stateChanged(e);
		}
	}

	@Override
    public void dispose() {
		removeGroupNode(this);
	}

	@Override
	protected Element createElement() { return null; } // invisible

	@Override
	protected void saveProperties(Properties props) {
		super.saveProperties(props);
		props.setProperty("limit", Integer.toString(limit));
	}
	@Override
	protected void loadProperties(Attributes atts) {
		super.loadProperties(atts);
		limit = getIntValue(atts, "limit", limit);
	}
}