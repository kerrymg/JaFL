package flands;


import java.awt.event.ActionEvent;
import java.util.Properties;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Element;

import org.xml.sax.Attributes;

/**
 * Node to either gain a resurrection deal, or activate an existing one.
 * 
 * @author Jonathan Mann
 */
public class ResurrectionNode extends ActionNode implements Executable, ChangeListener, Flag.Listener {
	public static final String ElementName = "resurrection";

	private Resurrection resurrection;
	private String shards = null;
	private String flag = null;

	ResurrectionNode(Node parent) {
		super(ElementName, parent);
		setEnabled(false);
		findExecutableGrouper().addExecutable(this);
	}

	@Override
	public void init(Attributes atts) {
		String text = atts.getValue("text");
		String book = atts.getValue("book");
		String section = atts.getValue("section");
		if (book != null && section != null) {
			resurrection = new Resurrection(text, book, section);
			String god = atts.getValue("god");
			if (god != null)
				resurrection.setGod(god);
			resurrection.setSupplemental(getBooleanValue(atts, "supplemental", false));
		}
		// resurrection == null signals that we want to 'use' the players resurrection

		shards = atts.getValue("shards");
		if (shards != null)
			getAdventurer().addMoneyListener(this);

		flag = atts.getValue("flag");
		if (flag != null)
			getFlags().addListener(flag, this);
		
		super.init(atts);
	}

	@Override
	protected void outit(Properties props) {
		super.outit(props);
		if (resurrection != null) resurrection.saveTo(props);
		if (shards != null) saveVarProperty(props, "shards", shards);
		if (flag != null) props.setProperty("flag", flag);
	}

	private boolean hadContent = false;
	@Override
	public void handleContent(String text) {
		if (!hadContent && text.trim().length() == 0) return;
		hadContent = true;
		Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText(text, createStandardAttributes()));
		addEnableElements(leaves);
		addHighlightElements(leaves);
	}

	@Override
	public boolean handleEndTag() {
		if (!hadContent && !hidden) {
			if (resurrection != null) {
				StyledTextList content = resurrection.getContent(createStandardAttributes());
				Element[] leaves = content.addTo(getDocument(), getElement());
				addEnableElements(leaves);
				addHighlightElements(leaves);
			}
		}
		return super.handleEndTag();
	}

	private boolean canDoAction() {
		return (resurrection == null ?
				getAdventurer().hasResurrection() :
				(shards == null || getAttributeValue(shards) <= getAdventurer().getMoney())
				);
	}

	@Override
	public boolean execute(ExecutableGrouper grouper) {
		if (flag != null && !getFlags().getState(flag))
			setEnabled(false);
		else {
			boolean canDo = canDoAction();
			setEnabled(canDo);
			if (hidden && canDo)
				actionPerformed(null);
		}
		return true;
	}

	@Override
	public void stateChanged(ChangeEvent evt) {
		setEnabled(getAdventurer().getMoney() >= getAttributeValue(shards));
	}


	@Override
	public void flagChanged(String name, boolean state) {
		if (flag != null && flag.equals(name)) {
			if (state && canDoAction())
				setEnabled(true);
			else
				setEnabled(false);
		}
	}

	@Override
	public void resetExecute() { setEnabled(false); }

	@Override
	public void actionPerformed(ActionEvent evt) {
		if (shards != null) {
			System.out.println("Resurrection costs " + getAttributeValue(shards));
			getAdventurer().adjustMoney(-getAttributeValue(shards));
		}
		if (resurrection != null)
			getAdventurer().addResurrection(resurrection);
		else {
			Resurrection r = getAdventurer().chooseResurrection("Choose Resurrection");
			if (r != null)
				r.activate();
		}
		setEnabled(false);
		callsContinue = false;

		if (flag != null)
			getFlags().setState(flag, false);
	}

	@Override
	protected Element createElement() { return null; }

	@Override
	public void dispose() {
		if (shards == null)
			getAdventurer().removeMoneyListener(this);
		if (flag != null)
			getFlags().removeListener(flag, this);
	}

	@Override
	protected String getTipText() {
		String text;
		if (resurrection == null)
			text = "Resurrect your character";
		else if (shards != null)
			text = "Purchase resurrection arrangements for " + getAttributeValue(shards) + " Shards";
		else if (!getAdventurer().hasResurrection() || resurrection.isSupplemental())
			text = "Add resurrection arrangements";
		else
			text = "Replace resurrection arrangements";
		return text;
	}
}
