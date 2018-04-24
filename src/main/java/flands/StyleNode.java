package flands;


import java.util.LinkedList;
import java.util.List;

import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * Common node to handle styled text - bold, italics, underlined, and mixed-size upper-case.
 * The currently active style is modified when one of these nodes is entered or exited; this
 * active style is applied to any added text.
 * 
 * @author Jonathan Mann
 */
public abstract class StyleNode extends Node {
	static final String BoldElementName = "b";
	static final String ItalicElementName = "i";
	static final String CapsElementName = "caps";
	static final String UnderlineElementName = "u";

	private static List<StyleNode> activeStyles;
	/** Add a style to the stack of active styles. */
	private static void addActiveStyle(StyleNode node) {
		if (activeStyles == null)
			activeStyles = new LinkedList<>();
				activeStyles.add(0, node);
	}
	/** Remove the style from the stack of active styles. */
	private static void removeActiveStyle(StyleNode node) {
		StyleNode removed = activeStyles.remove(0);
		if (!node.equals(removed))
			System.out.println("Removing style " + node + " that doesn't match " + removed);
	}
	/** Apply all active styles to an attribute set. */
	static void applyActiveStyles(MutableAttributeSet atts) {
		if (activeStyles != null)
			for (StyleNode activeStyle : activeStyles)
				activeStyle.setAttribute(atts);
	}
	static javax.swing.text.MutableAttributeSet createActiveAttributes() {
		if (activeStyles == null || activeStyles.size() == 0)
			return null;
		else {
			SimpleAttributeSet atts = new SimpleAttributeSet();
			applyActiveStyles(atts);
			return atts;
		}
	}

	/**
	 * Create a new StyleNode.
	 * Adds itself to the stack of active styles.
	 */
	public StyleNode(String name, Node parent) {
		super(name, parent);
		addActiveStyle(this);
	}

	@Override
	public boolean isStyleNode() { return true; }

	/**
	 * Child nodes should be created as direct children of the parent node.
	 */
	@Override
	protected Node createChild(String name) {
		return getParent().createChild(name);
	}

	/**
	 * Handle textual content by passing it to the parent node.
	 * If the parent has been keeping track of style nodes, as Node does,
	 * it can apply this style (and any others) to the content being handled.
	 */
	@Override
	public void handleContent(String content) {
		getParent().handleContent(content);
	}
	/** Handles the close tag by removing itself from the stack of active styles. */
	@Override
	public boolean handleEndTag() {
		removeActiveStyle(this);
		return true;
	}

	@Override
	protected Element createElement() { return null; }

	protected abstract void setAttribute(MutableAttributeSet attributes);

	public static class Bold extends StyleNode {
		public Bold(Node parent) { super(BoldElementName, parent); }
		@Override
		protected void setAttribute(MutableAttributeSet attributes) {
			StyleConstants.setBold(attributes, true);
		}
	}

	public static class Italic extends StyleNode {
		Italic(Node parent) { super(ItalicElementName, parent); }
		@Override
		protected void setAttribute(MutableAttributeSet attributes) {
			StyleConstants.setItalic(attributes, true);
		}
	}

	public static class Caps extends StyleNode {
		Caps(Node parent) { super(CapsElementName, parent); }
		@Override
		protected void setAttribute(MutableAttributeSet attributes) {
			attributes.addAttribute(CapsElementName, Boolean.TRUE);
		}
	}

	public static class Underline extends StyleNode {
		Underline(Node parent) { super(UnderlineElementName, parent); }
		@Override
		protected void setAttribute(MutableAttributeSet attributes) {
			StyleConstants.setUnderline(attributes, true);
		}
	}
}
