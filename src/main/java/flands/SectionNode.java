package flands;

import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * The root node, having the section name and parenting all other nodes.
 * It also creates and keeps the only pointer to the document.
 * 
 * @author Jonathan Mann
 */
public class SectionNode extends Node implements ItemListener, Loadable {
	public static final String ElementName = "section";

	private String book, section;
	private String name;
	private int boxes;
	private String dock, todock;
	private int profession = -1;
	private String tag;
	private String imageFilename;
	private boolean isStart;
	private MarketNode market = null;
	private ChoiceNode fleeChoice = null;
	private GotoNode fleeGoto = null;
	private boolean itemCache = false;
	private JCheckBox[] tickBoxes = null;
	private SectionDocument doc;
	private ExecutableRunner runner;
	private ParagraphNode currentChild = null;

	public SectionNode() { this(null); }
	public SectionNode(Node parent) {
		super(ElementName, null);
		if (parent != null)
			System.out.println("Section node should be root, and shouldn't have a parent: " + parent);

		doc = new SectionDocument();
		doc.grabWriteLock();

		runner = new ExecutableRunner("root", null);
	}

	@Override
	public SectionDocument getDocument() { return doc; }

	private ParagraphNode getParagraphChild() {
		if (currentChild == null) {
			currentChild = new ParagraphNode(this);
			addChild(currentChild);
		}
		return currentChild;
	}
	private void closeParagraphChild() {
		if (currentChild != null) {
			currentChild.handleEndTag();
			currentChild = null;
		}
	}

	static boolean isTopLevelNode(String name) {
		return (name.equals(ParagraphNode.ElementName) ||
				name.equals("choices") || name.equals("table") ||
				name.equals(OutcomesTableNode.ElementName) ||
				name.equals(FieldNode.ElementName) ||
				name.equals(MarketNode.ElementName) ||
				name.equals(FightNode.ElementName) ||
				name.equals(WhileNode.ElementName) ||
				name.equals(CacheNode.ItemElementName) ||
				name.equals(CacheNode.MoneyElementName) ||
				HeadingNode.isElementName(name));
	}

	@Override
	protected Node createChild(String name) {
		if (isTopLevelNode(name)) {
			// ie. a valid top-level element (paragraph or table)
			// or node without elements (WhileNode)
			closeParagraphChild();
			return super.createChild(name);
		}
		else {
			// Let a paragraph node deal with these tags
			return getParagraphChild().createChild(name);
		}
	}

	@Override
	public ExecutableGrouper getExecutableGrouper() { return runner; }
	void startExecution() {
		// Moved a whole bunch of these out of init(), where  - they shouldn't be
		// executed when the section is loaded from a saved game
		if (dock != null)
			getShips().setAtDock(dock);
		if (profession >= 0) {
			FLApp.getSingle().showProfession(profession);
			FLApp.getSingle().setNameEditable(true, true);
		}
		if (isStart) {
			FLApp.getSingle().setNameEditable(false, true);
			FLApp.getSingle().showMapWindow();
			//getAdventurer().getCodewords().clear();
		}
		runner.startExecution(false); // on the same thread
	}

	@Override
	public void init(Attributes atts) {
		name = atts.getValue("name"); // we need the section name before we can create the element
		boxes = getIntValue(atts, "boxes", 0);
		dock = atts.getValue("dock");
		todock = atts.getValue("todock");
		String val = atts.getValue("profession");
		if (val != null)
			profession = Adventurer.getProfessionType(val);
		isStart = getBooleanValue(atts, "start", false);
		tag = atts.getValue("tag");
		if (tag != null)
			tag = tag.toLowerCase();
		imageFilename = atts.getValue("image");
		super.init(atts);
	}

	@Override
	public void handleContent(String text) {
		if (text.trim().length() == 0) return;
		getParagraphChild().handleContent(text);
	}

	@Override
	public boolean handleEndTag() {
		closeParagraphChild();

		// Remove the first-line indent on the first paragraph child (after the section name)
		boolean removeIndent = true;
		for (int e = 1; e < getElement().getElementCount(); e++) {
			Element childElement = getElement().getElement(e);
			String viewType = getViewType(childElement);
			if (removeIndent) {
				if (viewType != null && viewType.equals(ParagraphViewType) && (childElement instanceof AbstractDocument.AbstractElement)) {
					((AbstractDocument.AbstractElement)childElement).removeAttribute(StyleConstants.FirstLineIndent);
					removeIndent = false;
				}
			}
			else if (viewType != null && viewType.equals(TableViewType))
				removeIndent = true;
		}

		if (imageFilename != null) {
			// Add an element displaying the image
			Books.BookDetails bookInfo = Books.getCanon().getBook(book);
			InputStream imageStream = bookInfo.getInputStream(imageFilename);
			System.out.println("Got input stream for illustration: " + imageStream);
			if (imageStream != null) {
				Image image = FLApp.getSingle().loadImage(imageStream);
				System.out.println("Got illustration: " + image);
				if (image != null) {
					SectionDocument.RootElement root = (SectionDocument.RootElement)getElement();
					SimpleAttributeSet imageAtts = new SimpleAttributeSet();
					StyleConstants.setIcon(imageAtts, new ImageIcon(image));
					StyleConstants.setAlignment(imageAtts, StyleConstants.ALIGN_CENTER);
					setViewType(imageAtts, ImageViewType);
					setImage(imageAtts, image);
					SectionDocument.Branch branch = doc.createBranchElement(root, imageAtts);
					doc.addLeavesTo(branch, new StyledText("\n", null));
					root.addChild(branch);
				}
			}
		}
		
		doc.releaseWriteLock();
		//if (FLApp.debugging)
		//	doc.dump(System.out);
		//doc.printTimingInfo();

		// Start stepping through the Executables
		// This is now called by ParserHandler
		//startExecution();

		return true;
	}

	@Override
	public String getSectionName() { return name; }
	public String getBook() { return book; }
	public void setBook(String book) { this.book = book; }
	public String getSection() { return section; }
	public void setSection(String section) { this.section = section; }

	@Override
	protected String getElementViewType() { return BoxYViewType; }

	@Override
	protected Element createElement() {
		SectionDocument.RootElement root = doc.createRootElement();

		if (name != null && name.length() > 0) {
			// Add section name as initial paragraph element
			SimpleAttributeSet nameAtts = new SimpleAttributeSet();
			StyleConstants.setBold(nameAtts, true);
			StyleConstants.setAlignment(nameAtts, StyleConstants.ALIGN_CENTER);
			setViewType(nameAtts, ParagraphViewType);
			SectionDocument.Branch branch = doc.createBranchElement(root, nameAtts);
			doc.addLeavesTo(branch, new StyledText(name + (boxes >= 1 ? "" : "\n"), null));

			if (boxes >= 1) {
				// Display some checkboxes for the number of ticks associated with this section
				tickBoxes = new JCheckBox[boxes];
				SimpleAttributeSet tickAtts = new SimpleAttributeSet();
				setViewType(tickAtts, ComponentViewType);
				//StyleConstants.setSpaceBelow(tickAtts, 6);
				int selectedTicks = getCodewords().getTickCount(book, name);
				for (int t = 0; t < boxes; t++) {
					JCheckBox tickBox = new JCheckBox();
					if (t < selectedTicks)
						tickBox.setSelected(true);
					tickBox.setEnabled(false);
					tickBox.setMargin(new Insets(0, 0, 0, 0));
					tickBox.setAlignmentY(SectionDocument.getFontVerticalAlignment());
					SimpleAttributeSet thisTickAtts = new SimpleAttributeSet(tickAtts);
					StyleConstants.setComponent(thisTickAtts, tickBox);
					doc.addLeavesTo(branch, new StyledText("." /*+ (t == boxes - 1 ? "\n" : "")*/, thisTickAtts));
					tickBoxes[t] = tickBox;
				}
				/*doc.addLeavesTo(branch, new String[] { "\n" }, null);*/
			}
			root.addChild(branch);
		}

		return root;
	}

	public void addTick() { addTicks(1); }
	void addTicks(int ticks) {
		if (tickBoxes != null) {
			// Tick the first <ticks> number of unselected boxes we come across
			int ticked = 0;
			for (JCheckBox tickBox : tickBoxes) {
				if (!tickBox.isSelected()) {
					tickBox.removeItemListener(this); // so we won't respond to the following event
					tickBox.setEnabled(false);
					tickBox.setSelected(true);
					if (++ticked == ticks)
						break;
				}
			}
			getCodewords().addTicks(name, ticks);
		}
	}
	void enableTicks(int ticks, ItemListener l) {
		if (tickBoxes != null) {
			// Enable the first <ticks> number of unselected boxes we come across,
			// and add an item listener to them
			for (JCheckBox tickBox : tickBoxes) {
				if (!tickBox.isSelected()) {
					tickBox.setEnabled(true);
					tickBox.addItemListener(l);
					tickBox.addItemListener(this);
					if (--ticks == 0)
						break;
				}
			}
		}
	}
	@Override
	public void itemStateChanged(ItemEvent evt) {
		for (JCheckBox tickBox : tickBoxes) {
			if (tickBox == evt.getSource()) {
				tickBox.setEnabled(false);
				getCodewords().addTicks(name, 1);
				break;
			}
		}
	}

	boolean hasTag(String t) {
		if (tag != null) {
			if (!tag.startsWith(","))
				tag = "," + tag;
			if (!tag.endsWith(","))
				tag += ",";
			return tag.contains("," + t.toLowerCase() + ",");
		}
		return false;
	}

	boolean isSellingMarket() {
		return (market != null && market.isSellingMarket());
	}
	void setMarketNode(MarketNode market) {
		this.market = market;
	}
	String getMarketCurrency() {
		return (market == null || market.isDefaultCurrency() ? "Shard" : market.getCurrency());
	}

	public boolean isItemCache() { return itemCache; }
	public void setItemCache(boolean b) {
		itemCache = b;
	}

	ChoiceNode getFleeChoice() { return fleeChoice; }
	void setFleeChoice(ChoiceNode node) {
		fleeChoice = node;
	}
	GotoNode getFleeGoto() { return fleeGoto; }
	void setFleeGoto(GotoNode node) {
		fleeGoto = node;
	}

	/* ************************
	 * Variable-keeping methods
	 ************************ */
	/**
	 * The name used for the 'anonymous' variable (mostly only one variable is necessary).
	 */
	private static final String AnonymousVariableName = "'anon'";
	private Map<String,Integer> variables = new java.util.HashMap<>();

	/** Returns the variable name, converting nulls to an actual (anonymous) name. */
	private String checkVariableName(String name) { return (name == null ? AnonymousVariableName : name); }

	/**
	 * Check whether this variable is defined.
	 * @param name the name of the variable; may be <code>null</code>.
	 */
	@Override
	public boolean isVariableDefined(String name) { return variables.get(checkVariableName(name)) != null; }

	/**
	 * Get the current value of a variable.
	 * @param name the name of the variable; may be <code>null</code>.
	 */
	@Override
	public int getVariableValue(String name) {
		Integer val = variables.get(checkVariableName(name));
		if (val == null) {
			System.out.println("Variable '" + checkVariableName(name) + "' is not defined");
			return Integer.MIN_VALUE;
		}
		else
			return val;
	}

	/**
	 * Set the value of a variable.
	 * @param name the name of the variable; may be <code>null</code>.
	 */
	@Override
	public void setVariableValue(String name, int value) {
		variables.put(checkVariableName(name), value);
	}

	/**
	 * Adjust the value of a variable.
	 * @param name the name of the variable; may be <code>null</code>.
	 */
	@Override
	public void adjustVariableValue(String name, int delta) {
		name = checkVariableName(name);
		Integer val = variables.get(name);
		if (val == null)
			System.err.println("Variable '" + name + "' is not defined; can't adjust");
		else
			variables.put(name, val + delta);
	}

	/**
	 * Remove a variable and its value.
	 */
	@Override
	public void removeVariable(String name) {
		variables.remove(name);
	}

	@Override
	public String getDockLocation() { return dock; }
	String getToDockLocation() { return todock; }

	private int ifElseCounter = 0;
	String getIfElseVarName(boolean newVar) {
		if (newVar)
			// An <if> node - give it a new, unique variable name
			return "*if" + ifElseCounter++;
		else
			// An <elseif> or <else> node - give it the variable name used by the most
			// recently closed <if> node
			return elseVarName;
	}
	private String elseVarName = null;
	void setElseVarName(String varName) {
		elseVarName = varName;
	}

	/* *****************
	 * Load/Save methods
	 ***************** */
	public void saveToFile(String filepath) throws IOException {
		try {
			FileOutputStream out = new FileOutputStream(filepath);
			saveTo(out);
			out.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			throw ioe;
		}
	}

	@Override
	protected void saveProperties(Properties props) {
		super.saveProperties(props);

		// Save the current variable values
		props.setProperty("variableCount", Integer.toString(variables.size()));
		int j = 0;
		for (Iterator<Map.Entry<String,Integer>> i = variables.entrySet().iterator(); i.hasNext(); j++) {
			Map.Entry<String,Integer> e = i.next();
			if (!e.getKey().equals(AnonymousVariableName))
				props.setProperty("variableKey" + j, e.getKey());
			props.setProperty("variableValue" + j, e.getValue().toString());
		}
	}

	@Override
	protected void loadProperties(Attributes props) {
		super.loadProperties(props);

		// Get the variable values
		variables.clear();
		int variableCount = getIntValue(props, "variableCount", 0);
		for (int i = 0; i < variableCount; i++) {
			String variableKey = props.getValue("variableKey" + i);
			int variableValue = getIntValue(props, "variableValue" + i, Integer.MIN_VALUE);
			setVariableValue(variableKey, variableValue);
			System.out.println("Variable " + variableKey + "=" + variableValue);
		}
	}

	@Override
	public String getFilename() {
		return "sectiondump.xml";
	}
	public boolean loadFrom(InputStream in) {
		try {
			SAXParser parser = FLApp.createSAXParser();
			parser.parse(in, new DynamicSectionLoader(this));
			return true;
		}
		catch (SAXException e) {
			System.err.println("Error in setting up XMLReader");
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public boolean saveTo(OutputStream out) throws IOException {
		PrintStream pout = new PrintStream(out);
		outputTo(pout, "", XMLOutput.OUTPUT_PROPS_DYNAMIC);
		return true;
	}
}