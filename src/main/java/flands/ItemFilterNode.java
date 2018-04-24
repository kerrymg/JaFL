package flands;

import javax.swing.text.Element;

import org.xml.sax.Attributes;

/**
 * Modify which Items can be matched by a parent Node.
 * Item matching can be modified by a series of child ItemFilterNodes, which may include
 * or exclude items.
 * 
 * @author Jonathan Mann
 */
public class ItemFilterNode extends Node {
	static final String IncludeName = "include";
	static final String ExcludeName = "exclude";
	private final boolean include;
	private Item item;
	//private ItemList cache;
	private String reason;

	ItemFilterNode(boolean include, Node parent) {
		super(include ? IncludeName : ExcludeName, parent);
		this.include = include;
	}

	public boolean isInclude() { return include; }
	private int[] getMatchedItems() {
		return getItems().findMatches(item);
	}
	void filterItems(IndexSet set) {
		int[] matches = getMatchedItems();
		if (include)
			set.add(matches);
		else
			set.remove(matches, reason);
	}

	@Override
	public void init(Attributes atts) {
		item = Item.createItem(atts);
		if (item == null) {
			System.err.println("Error: FilterNode didn't include item attributes!");
			item = new Item("&%"); // match nothing
		}
		reason = atts.getValue("reason");

		super.init(atts);
	}

	@Override
	protected Element createElement() { return null; }
}
