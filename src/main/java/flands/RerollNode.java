package flands;


import java.awt.event.ActionEvent;

import javax.swing.text.Element;

/**
 * Action node that, when clicked, will undo the last dice roll made.
 * Works as if a Luck blessing had just been activated. Possibly flakey.
 * 
 * @author Jonathan Mann
 */
public class RerollNode extends ActionNode implements Executable {
	public static final String ElementName = "reroll";

	RerollNode(Node parent) {
		super(ElementName, parent);
		setEnabled(false);
		addExecutableNode(this);
	}

	private boolean hadContent = false;
	@Override
	public void handleContent(String text) {
		if (text.trim().length() == 0) return;
		hadContent = true;
		Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText(text, createStandardAttributes()));
		addEnableElements(leaves);
		addHighlightElements(leaves);
	}

	@Override
	public boolean handleEndTag() {
		if (!hadContent && !getParent().hideChildContent()) {
			String text = (getDocument().isNewSentence() ? "Roll again" : "roll again");
			handleContent(text);
		}
		return super.handleEndTag();
	}

	@Override
	public boolean execute(ExecutableGrouper grouper) {
		setEnabled(true);

		// Shouldn't need to cache the calling grouper, because we'll be backtracking
		return false;
	}

	@Override
	public void resetExecute() {
		setEnabled(false);
	}

	@Override
	public void fireActionEvent(Element e) {
		// Overridden so we can comment out the call below:
		//UndoManager.createNull();
		actionPerformed(new ActionEvent(e, ActionEvent.ACTION_PERFORMED, "command?"));
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		setEnabled(false);
		System.out.println("RerollNode: calling UndoManager.undo");
		UndoManager.getCurrent().undo();
	}

	@Override
	protected Element createElement() { return null; }

	@Override
	protected String getTipText() { return "Redo the last roll"; }
}
