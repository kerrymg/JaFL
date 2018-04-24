package flands;

import java.util.LinkedList;
import java.util.List;

import javax.swing.text.Element;

import org.xml.sax.Attributes;

/**
 * Special hidden node attached to a market - if it matches an item being bought or sold,
 * the child action nodes will be activated.
 * 
 * @author Jonathan Mann
 */
public class TradeEventNode extends Node {
	static final String BoughtElementName = "bought";
	static final String SoldElementName = "sold";
	private final boolean bought;
	private final List<ActionNode> events;
	private Item item = null;

	TradeEventNode(String name, Node parent) {
		super(name, parent);
		if (name.equalsIgnoreCase(BoughtElementName))
			bought = true;
		else if (name.equalsIgnoreCase(SoldElementName))
			bought = false;
		else {
			System.err.println("TradeEventNode: unrecognised element name: " + name);
			bought = false;
		}
		events = new LinkedList<>();
	}

	@Override
	protected Node createChild(String name) {
		Node n = super.createChild(name);
		if (n instanceof ActionNode) {
			System.out.println("TradeNode: adding action as child: " + n);
			events.add((ActionNode)n);
		}
		return n;
	}

	private ExecutableRunner runner = null;
	@Override
	public ExecutableGrouper getExecutableGrouper() {
		if (runner == null)
			runner = new ExecutableRunner();
		return runner;
	}

	@Override
	protected Element createElement() { return null; }

	public void setItem(Item i) {
		this.item = i;
	}

	@Override
	public void init(Attributes atts) {
		if (item == null)
			item = Item.createItem(atts);
		
		super.init(atts);
	}
	@Override
	public boolean hideChildContent() { return true; }

	@Override
	public boolean handleEndTag() {
		runner = null;
		return false;
	}

	void itemTraded(boolean bought, Item trade) {
		if (this.bought == bought &&
			(item == null || item.matches(trade))) {
			System.out.println("TradeEventNode triggered");
			for (ActionNode event : events)
				event.actionPerformed(null);
		}
	}
}
