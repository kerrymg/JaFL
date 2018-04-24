package flands;


import java.util.LinkedList;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.xml.sax.Attributes;

/**
 * Grouping node for a market - contains ItemNodes, TradeNodes and the (internal class)
 * HeaderNode, arranged as a table. It may also contain special TradeEventNodes that
 * are secretly activated when a matching item is bought or sold.
 * 
 * @author Jonathan Mann
 */
public class MarketNode extends TableNode implements Executable {
	public static final String ElementName = "market";
	private boolean buy = true, sell = true;
	private String currency;
	private LinkedList<TradeEventNode> tradeEvents;
	
	MarketNode(Node parent) {
		super(ElementName, parent);
		setEnabled(false);
	}

	@Override
	public void init(Attributes atts) {
		buy = getBooleanValue(atts, "buy", true);
		sell = getBooleanValue(atts, "sell", true);
		if (sell)
			getRoot().setMarketNode(this);
		currency = atts.getValue("currency");
		super.init(atts);
	}

	@Override
	public boolean handleEndTag() {
		findExecutableGrouper().addExecutable(this);
		return true;
	}
	
	boolean isBuyingMarket() { return buy; }
	boolean isSellingMarket() { return sell; }
	boolean isDefaultCurrency() { return currency == null; }
	String getCurrency() { return currency; }

	@Override
	protected Node createChild(String name) {
		Node n = null;
		switch (name) {
		case HeaderNode.ElementName:
			n = new HeaderNode(this, buy, sell);
			break;
		case TradeNode.ElementName:
			n = new TradeNode(this);
			break;
		case TradeEventNode.BoughtElementName:
		case TradeEventNode.SoldElementName:
			if (tradeEvents == null)
				tradeEvents = new LinkedList<>();
			tradeEvents.add(new TradeEventNode(name, this));
			n = tradeEvents.getLast();
			break;
		default:
			Item i = Item.createItem(name);
			if (i != null)
				n = new TradeNode(this, i);
			break;
		}

		if (n != null) {
			addChild(n);
			return n;
		}
		else
			return super.createChild(name);
	}

	@Override
	public boolean execute(ExecutableGrouper grouper) {
		setEnabled(true);
		return true;
	}

	@Override
	public void resetExecute() {}

	void itemTraded(boolean bought, Item traded) {
		if (tradeEvents != null)
			for (TradeEventNode tradeEvent : tradeEvents)
				tradeEvent.itemTraded(bought, traded);
	}

	@Override
	public void dispose() {
		if (isSellingMarket()) {
			// Remove all SellNodes from their Items
			String ourSection = getSectionName();
			for (int i = 0; i < getItems().getItemCount(); i++)
				getItems().getItem(i).removeSellNode(ourSection);
		}
	}

	public static class HeaderNode extends Node {
		public static final String ElementName = "header";
		public static int SHIP_TYPE = 0;
		public static int CARGO_TYPE = 1;
		public static int ARMOUR_TYPE = 2;
		public static int WEAPON_TYPE = 3;
		public static int MAGIC_TYPE = 4;
		public static int SHIP_SALE_TYPE = 5;
		public static int OTHER_TYPE = 6;
		private static final String[] TypeNames = {"ship", "cargo", "armour", "weapon", "magic", "shipsale", "other"};
		public static int getType(String name) {
			name = name.toLowerCase();
			int i = TypeNames.length-1;
			for (; i >= 0; i--)
				if (name.startsWith(TypeNames[i]))
					return i;
			return i;
		}
		private static final String[][] HeaderStrings = new String[][]
			{
				{"Ship type", "Cost", "Capacity"},
				{"Cargo", "To buy", "To sell"},
				{"Armour", "To buy", "To sell"},
				{"Weapons (sword, axe, etc)", "To buy", "To sell"},
				{"Magical equipment", "To buy", "To sell"},
				{"Type", "Sale price"},
				{"Other items", "To buy", "To sell"}
			};

		private int type = -1;
		private boolean buy, sell;

		HeaderNode(Node parent, boolean buy, boolean sell) {
			super(ElementName, parent);
			this.buy = buy;
			this.sell = sell;
		}

		@Override
		protected MutableAttributeSet getElementStyle() {
			SimpleAttributeSet italicAtts = new SimpleAttributeSet();
			StyleConstants.setItalic(italicAtts, true);
			StyleConstants.setSpaceAbove(italicAtts, 10.0f);
			return italicAtts;
		}

		@Override
		public void init(Attributes atts) {
			String val = atts.getValue("type");
			if (val != null)
				type = getType(val);
			super.init(atts);

			// Add the cells
			String[] headers;
			if (type >= 0)
				headers = HeaderStrings[type];
			else {
				headers = new String[5]; // unlikely to go past this
				for (int i = 0; i < headers.length; i++) {
					String attName = "header" + (i+1);
					headers[i] = atts.getValue(attName);
					if (headers[i] == null) {
						if (i < 2)
							System.out.println("Expected at least two headers here: " + atts);
						break;
					}
				}
			}

			for (int h = 0; h < headers.length; h++) {
				if (headers[h] == null)
					break;
				if (h == 1 && !buy)
					continue;
				else if (h == 2 && !sell)
					continue;
				// This assumes that the second column is 'buy', and the third is 'sell'
				ParagraphNode cell = new ParagraphNode(this, h == 0 ? StyleConstants.ALIGN_LEFT : StyleConstants.ALIGN_RIGHT);
				addChild(cell);
				cell.init(atts);
				cell.handleContent(headers[h]);
				cell.handleEndTag();
			}
		}

		@Override
		protected String getElementViewType() { return RowViewType; }
	}
}
