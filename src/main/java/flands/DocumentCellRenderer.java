package flands;


import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JList;
import javax.swing.JTextPane;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.text.StyledDocument;

/**
 * Custom renderer to handle the StyledDocument created by each Item.
 * Code shamelessly ripped off from {@link javax.swing.DefaultListCellRenderer}.
 */
public class DocumentCellRenderer extends JTextPane implements ListCellRenderer<StyledDocument> {
	private static Border noFocusBorder;

	DocumentCellRenderer() {
		super();
		if (noFocusBorder == null)
			noFocusBorder = new EmptyBorder(1, 1, 1, 1);
		setOpaque(true);
		setBorder(noFocusBorder);
		setFont(SectionDocument.getPreferredFont());
	}

	protected Color getBackground(JList list, int index, boolean selected) {
		return (selected ? list.getSelectionBackground() : list.getBackground());
	}

	@Override
	public Component getListCellRendererComponent(JList list, StyledDocument value, int index, boolean isSelected, boolean cellHasFocus) {
		setDocument(value);
		setBackground(getBackground(list, index, isSelected));
		if (isSelected) {
			setForeground(list.getSelectionForeground());
		}
		else {
			setForeground(list.getForeground());
		}
		setEnabled(list.isEnabled());

		Border border = null;
		if (cellHasFocus) {
			if (isSelected) {
				border = UIManager.getBorder("List.focusSelectedCellHighlightBorder");
			}
			if (border == null) {
				border = UIManager.getBorder("List.focusCellHighlightBorder");
			}
		}
		else {
			border = noFocusBorder;
		}
		setBorder(border);
		return this;
	}

	@Override
	public boolean isOpaque() {
		Color back = getBackground();
		Component p = getParent(); 
		if (p != null)
			p = p.getParent(); 

		// p should now be the JList. 
		boolean colorMatch = (back != null) && (p != null) && back.equals(p.getBackground()) && p.isOpaque();
		return !colorMatch && super.isOpaque(); 
	}

	@Override
	public void validate() {}
	@Override
	public void invalidate() {}
	@Override
	public void repaint() {}
	@Override
	public void revalidate() {}
	@Override
	public void repaint(long tm, int x, int y, int width, int height) {}
	@Override
	public void repaint(Rectangle r) {}
	@Override
	protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		if (propertyName.equals("document")) {
			super.firePropertyChange(propertyName, oldValue, newValue);
		}
	}
	@Override
	public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {}
	@Override
	public void firePropertyChange(String propertyName, char oldValue, char newValue) {}
	@Override
	public void firePropertyChange(String propertyName, short oldValue, short newValue) {}
	@Override
	public void firePropertyChange(String propertyName, int oldValue, int newValue) {}
	@Override
	public void firePropertyChange(String propertyName, long oldValue, long newValue) {}
	@Override
	public void firePropertyChange(String propertyName, float oldValue, float newValue) {}
	@Override
	public void firePropertyChange(String propertyName, double oldValue, double newValue) {}
	@Override
	public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}

	private static int SafeMinHeight = -1;
	@Override
	public Dimension getPreferredSize() {
		Dimension d = super.getPreferredSize();
		if (d.height > 5 && SafeMinHeight == -1)
			SafeMinHeight = d.height;
		if (d.height <= 5 && SafeMinHeight != -1)
			d.height = SafeMinHeight;
		// System.out.println("Cell preferred height: " + d.height);
		return d;
	}
}
