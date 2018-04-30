package flands;


import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.xml.sax.Attributes;

/**
 * An action that will simulate a dice roll, storing the result in a section variable.
 * The roll can be adjusted by one or more AdjustNodes.
 * @author Jonathan Mann
 */
public class RandomNode extends ActionNode implements Executable, Roller.Listener, UndoManager.Creator, Flag.Listener {
	public static Random randomGen = new Random();
	public static final String ElementName = "random";
	private int dice;
	private String var;
	private String flag;
	protected int result = -1;
	private boolean forced;
	private String type;
	private List<AdjustNode> adjustments = null;

	RandomNode(Node parent) {
		this(ElementName, parent);
	}

	private RandomNode(String name, Node parent) {
		super(name, parent);
		setEnabled(false);
	}

	@Override
	public void init(Attributes xmlAtts) {
		dice = getIntValue(xmlAtts, "dice", 2);
		var = xmlAtts.getValue("var");
		flag = xmlAtts.getValue("flag");
		if (flag != null) {
			getFlags().addListener(flag, this);
		}
		type = xmlAtts.getValue("type");
		forced = getBooleanValue(xmlAtts, "force", true);

		// Create the table element
		super.init(xmlAtts);
	}

	@Override
	protected void outit(Properties props) {
		super.outit(props);
		if (dice != 2) saveProperty(props, "dice", dice);
		if (var != null) props.setProperty("var", var);
		if (flag != null) props.setProperty("flag", flag);
		if (type != null) props.setProperty("type", type);
		if (!forced) saveProperty(props, "force", false);
	}
	
	public boolean isTravel() { return (type != null && type.equalsIgnoreCase("travel")); }

	private boolean addedContent = false;
	@Override
	public void handleContent(String content) {
		if (content.trim().length() == 0)
			return;
		addedContent = true;
		System.out.println("Adding RandomNode content: " + content);
		Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText(content, createStandardAttributes()));
		addEnableElements(leaves);
		addHighlightElements(leaves);
	}

	@Override
	public boolean handleEndTag() {
		if (!addedContent && !hidden && !getParent().hideChildContent()) {
			String content;
			if (getDocument().isNewSentence(getDocument().getLength()))
				content = "Roll ";
			else
				content = "roll ";
			if (dice == 1)
				content += "one die";
			else if (dice == 2)
				content += "two dice";
			else
				content += dice + " dice";
			handleContent(content);
		}
		System.out.println("Adding RandomNode(" + dice + "D) as Executable child");
		findExecutableGrouper().addExecutable(this);
		
		return super.handleEndTag();
	}

	@Override
	protected Node createChild(String name) {
		Node n = null;
		if (name.equals(AdjustNode.ElementName)) {
			AdjustNode an = new AdjustNode(this);
			if (adjustments == null)
				adjustments = new LinkedList<>();
			adjustments.add(an);
			n = an;
		}

		if (n == null)
			return super.createChild(name);
		else {
			addChild(n);
			return n;
		}
	}

	@Override
	public boolean execute(ExecutableGrouper grouper) {
		if (flag != null) {
			if (!getFlags().getState(flag))
				return true;
		}

		if (result < 0) {
			// Set up for user to roll
			System.out.println("RandomNode: ready to roll!");
			setEnabled(true);
			return !forced;
		}
		else {
			// Already rolled
			System.out.println("RandomNode.execute() called - we already have a result!?");
			return true;
		}
	}

	@Override
	public void resetExecute() {
		removeVariable(var);
		result = -1;
		setEnabled(false);
	}

	private Roller roller = null;
	@Override
	public void actionPerformed(ActionEvent evt) {
		if (roller != null) return;

		int delta = getAdjustment();
		setEnabled(false);
		//if (flag != null)
		//	PriceNode.getFlag(flag).setState(false);

		roller = new Roller(dice, delta);
		roller.addListener(this);
		roller.startRolling();
	}

	private int getAdjustment() {
		int delta = 0;
		if (adjustments != null) {
			for (AdjustNode adjustment : adjustments)
				delta += adjustment.getAdjustment();
			System.out.println("Adjustment for random=" + delta);
		}
		return delta;
	}

	@Override
	public void rollerFinished(Roller r) {
		if (roller == r) {
			setVariableValue(var, r.getResult());
			System.out.println("RandomNode: result is " + r.getResult());
			roller = null;
			UndoManager.createNew(this).add(this);
			
			// Keep a pointer to it - we'll need it for rerolls
			System.out.println("RandomNode: calling parent to continue execution");
			findExecutableGrouper().continueExecution(this, true);
		}
	}

	@Override
	public void undoOccurred(UndoManager undo) {
		// Re-enable, ready to roll again
		removeVariable(var);
		result = -1;
		setEnabled(true);
	}

	@Override
	public void flagChanged(String name, boolean state) {
		if (flag.equals(name)) {
			if (state) {
				result = -1;
				removeVariable(var);
			}
			setEnabled(state);
		}
	}

	// TODO: Why the hell is RandomNode returning paragraph-style attributes?
	@Override
	protected MutableAttributeSet getElementStyle() {
		SimpleAttributeSet atts = new SimpleAttributeSet();
		StyleConstants.setAlignment(atts, StyleConstants.ALIGN_JUSTIFIED);
		StyleConstants.setFirstLineIndent(atts, 25.0f);
		return atts;
	}

	@Override
	public void dispose() {
		if (flag != null)
			getFlags().removeListener(flag, this);
	}

	protected String getTipText() {
		String text = "Roll " + getDiceText(dice);
		int delta = getAdjustment();
		if (delta > 0)
			text += " and add " + delta;
		else if (delta < 0)
			text += " and subtract " + (-delta);
		return text;
	}
}
