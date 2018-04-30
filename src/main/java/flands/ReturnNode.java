package flands;


import java.awt.event.ActionEvent;
import java.util.Properties;

import javax.swing.text.Element;

import org.xml.sax.Attributes;

/**
 * Return to the previous section (generally after a Goto, often after activating
 * an item with a UseEffect). The previous section will be as you left it, which
 * occasionally results in some buggy behaviour.
 * 
 * @author Jonathan Mann
 */
public class ReturnNode extends ActionNode implements Executable {
	public static final String ElementName = "return";
	private boolean forced;
	ReturnNode(Node parent) {
		super(ElementName, parent);
		findExecutableGrouper().addExecutable(this);
		setEnabled(false);
	}

	@Override
	public void init(Attributes atts) {
		forced = getBooleanValue(atts, "force", true);
		super.init(atts);
	}
	@Override
	protected void outit(Properties props) {
		super.outit(props);
		if (!forced) saveProperty(props, "force", false);
	}

	@Override
	public void handleContent(String text) {
		if (text.trim().length() == 0) return;
		Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText(text, createStandardAttributes()));
		addEnableElements(leaves);
		addHighlightElements(leaves);
	}

	@Override
	public boolean execute(ExecutableGrouper grouper) {
		setEnabled(true);
		return !forced;
	}

	@Override
	public void resetExecute() {
		setEnabled(false);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		setEnabled(false);
		FLApp.getSingle().returnFromSection();
	}

	@Override
	protected Element createElement() { return null; }

	@Override
	protected String getTipText() { return "Return to the previous section"; }
}
