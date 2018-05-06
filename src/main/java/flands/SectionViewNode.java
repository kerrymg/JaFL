package flands;


import java.awt.event.ActionEvent;

import javax.swing.text.Element;

import org.xml.sax.Attributes;

/**
 * Action node, that when clicked will allow the player to browse through a number
 * of other sections. Currently only used in one place (5.114).
 * 
 * @see SectionBrowser
 * @author Jonathan Mann
 */
public class SectionViewNode extends ActionNode implements Executable {
	public static final String ElementName = "sectionview";

	private int random;
	private String title;

	SectionViewNode(Node parent) {
		super(ElementName, parent);
		setEnabled(false);
		addExecutableNode(this);
	}

	@Override
	public void init(Attributes atts) {
		title = atts.getValue("title");
		random = getIntValue(atts, "random", -1);
		// TODO: Read in other options (for any other version of this node)
		super.init(atts);
	}

	@Override
	protected Element createElement() { return null; }

	@Override
	public void handleContent(String text) {
		Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText(text, createStandardAttributes()));
		addEnableElements(leaves);
		addHighlightElements(leaves);
	}

	@Override
	public boolean execute(ExecutableGrouper caller) {
		setEnabled(true);
		return false;
	}

	@Override
	public void resetExecute() {
		setEnabled(false);
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		SectionBrowser browser = new SectionBrowser("5", false, random);
		browser.createDialog(FLApp.getSingle(), title).setVisible(true);
		setEnabled(false);
		continueNodeExecution(this, false);
	}

	@Override
	protected String getTipText() {
		if (random > 0)
			return "Randomly view other sections";
		else
			return "Browse through other sections";
	}
}
