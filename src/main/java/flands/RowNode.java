package flands;


import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * Mimics the HTML table row tag (and table cell). Used in non-section XML ie. the rules.
 * 
 * @author Jonathan Mann
 */
public class RowNode extends Node {
	public static final String ElementName = "tr";

	RowNode(Node parent) {
		super(ElementName, parent);
	}

	@Override
	protected String getElementViewType() { return RowViewType; }

	public static class CellNode extends Node {
		public static final String ElementName = "td";
		CellNode(Node parent) {
			super(ElementName, parent);
		}

		@Override
		protected String getElementViewType() { return ParagraphViewType; }

		@Override
		public void handleContent(String text) {
			getDocument().addLeavesTo(getElement(), new StyledText(text, StyleNode.createActiveAttributes()));
		}

		@Override
		protected MutableAttributeSet getElementStyle() {
			SimpleAttributeSet atts = new SimpleAttributeSet();
			StyleConstants.setAlignment(atts, StyleConstants.ALIGN_LEFT);
			return atts;
		}
	}
}
