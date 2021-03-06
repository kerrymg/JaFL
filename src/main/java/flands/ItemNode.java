package flands;


import java.awt.event.ActionEvent;
import java.util.Properties;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Element;

import org.xml.sax.Attributes;

/**
 * Action node that contains an item to be taken.
 * @author Jonathan Mann
 */
public class ItemNode extends ActionNode implements Executable, ChangeListener, Flag.Listener {
	static ItemNode createItemNode(String name, Node parent) {
		Item item = Item.createItem(name);
		return (item == null ? null : new ItemNode(item, parent));
	}

	private Item item;
	private String replace = null;
	private String flag = null;
	private ItemGroupNode itemGroup = null;
	private String quantityStr;
	private int quantity;
	private boolean forced;

	private ItemNode(Item item, Node parent) {
		super(item.getTypeName(), parent);
		this.item = item;
		setEnabled(false);
		addExecutableNode(this);
	}

	@Override
	public void init(Attributes xmlAtts) {
		item.init(xmlAtts);
		if (item.getGroup() != null) {
			itemGroup = ItemGroupNode.getGroupNode(item.getGroup());
			if (itemGroup != null)
				itemGroup.addChangeListener(this);
		}
		replace = xmlAtts.getValue("replace");
		if (replace != null && replace.length() == 0)
			replace = item.getName();
		flag = xmlAtts.getValue("flag");
		if (flag != null) {
			getFlags().addListener(flag, this);
		}
		quantityStr = xmlAtts.getValue("quantity");
		forced = getBooleanValue(xmlAtts, "force", false);
		
		super.init(xmlAtts);
	}

	public Item getItem() { return item; }

	@Override
	protected Node createChild(String name) {
		Node n = null;
		if (name.equals(EffectNode.ElementName))
			n = new EffectNode(this, item); 
		else {
			CurseNode curse = CurseNode.createCurseNode(name, this);
			if (curse != null) {
				n = curse;
				item.setCurse(curse.getCurse());
				curse.getCurse().setItem(item);
			}
		}

		if (n == null)
			n = super.createChild(name);
		else
			addChild(n);

		return n;
	}

	private boolean hadContent = false;
	@Override
	public void handleContent(String text) {
		if (text.trim().length() == 0) return;

		hadContent = true;
		Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText(text, createStandardAttributes()));
		addHighlightElements(leaves);
		addEnableElements(leaves);
	}

	@Override
	public boolean handleEndTag() {
		if (!hadContent && !hidden && !getParent().hideChildContent()) {
			Element[] leaves = item.addTo(getDocument(), getElement(), createStandardAttributes(), getDocument().isNewSentence());
			setHighlightElements(leaves);
			addEnableElements(leaves);
		}
		return super.handleEndTag();
	}

	private boolean callContinue = false;
	@Override
	public boolean execute(ExecutableGrouper grouper) {
		if (quantityStr != null) {
			quantity = getAttributeValue(quantityStr);
			//quantityStr = null;
			/*
			 * Don't do this - if the quantity is generated randomly
			 * (see 1.561), the player may use a luck blessing and get
			 * a different quantity. Clicking the action will break the undo chain
			 * that allows the luck blessing to be used anyway.
			 */
		}
		else if (flag == null)
			quantity = 1;
		else
			quantity = -1;
		
		if (flag != null) {
			flagChanged(flag, getFlags().getState(flag));
			return true; // don't block
		}
		
		if (replace != null) {
			// Check whether we have the item to be replaced
			Item match = new Item(replace);
			int[] indices = getItems().findMatches(match);
			if (indices.length == 0)
				return true;
		}

		if (quantity != 0)
			setEnabled(true);

		if (replace != null || forced) {
			callContinue = true;
			return false;
		}

		return true;
	}

	@Override
	public void resetExecute() {
		setEnabled(false);
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		boolean taken = false;
		if (replace != null) {
			Item match = new Item(replace);
			int[] indices = getItems().findMatches(match);
			if (indices.length > 0) {
				getItems().removeItem(indices[0]);
				getItems().insertItem(item, indices[0]);
				taken = true;
			}
			if (indices.length > 1)
				System.out.println(indices.length + " possible items to replace: I picked the first one!");
		}
		else if (getItems().addItem(item))
			taken = true;

		if (taken) {
			if (quantity-- > 0)
				// Copy the item so we don't add the same object multiple times
				// (otherwise wielding/wearing get screwed up, for one)
				item = item.copy();
			
			if (flag != null || quantity == 0)
				setEnabled(false);
			if (flag != null)
				getFlags().setState(flag, false);
		}

		if (itemGroup != null && !isEnabled()) {
			itemGroup.adjustLimit(-1);
		}

		if (callContinue && (!forced || taken)) {
			continueNodeExecution(this, false);
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == itemGroup) {
			if (itemGroup.getLimit() == 0)
				setEnabled(false); // one-way at this stage
		}
	}

	@Override
	public void flagChanged(String name, boolean state) {
		if (flag != null && flag.equals(name)) {
			System.out.println("ItemNode: flag '" + name + "' = " + state);
			if (state && quantity != 0)
				setEnabled(true);
			else
				setEnabled(false);
		}
	}

	@Override
	public void loadProperties(Attributes atts) {
		super.loadProperties(atts);
		quantity = getIntValue(atts, "quantity", -1);
		if (quantity == 0)
			setEnabled(false);
		callContinue = getBooleanValue(atts, "continue", false);
	}
	@Override
	public void saveProperties(Properties props) {
		super.saveProperties(props);
		saveProperty(props, "quantity", quantity);
		saveProperty(props, "continue", callContinue);
	}
	
	@Override
    public void dispose() {
		if (flag != null)
			getFlags().removeListener(flag, this);
	}
	
	protected String getTipText() {
		StyledTextList itemText = item.createItemText(null, false);
		StringBuilder sb = new StringBuilder();
		if (replace != null) {
			Item match = new Item(replace);
			StyledTextList replacesText = match.createItemText(null, false);
			sb.append("Replace ").append(replacesText.toXML()).append(" with ");
		}
		else {
			sb.append("Take ");
			if (quantity > 1)
				sb.append("1 of ").append(quantity).append(" ");
			else
				sb.append("a ");
			sb.append(itemText.toXML());
		}
		return sb.toString();
	}
}
