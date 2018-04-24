package flands;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.util.Enumeration;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

public class SectionDocumentViewer extends JDialog implements GameListener, TreeSelectionListener {
	private JTree elementTree;
	private JTable attTable;

	public SectionDocumentViewer(Frame parent) {
		super(parent, "Document Browser");
		init(parent);
	}

	public SectionDocumentViewer(Dialog parent) {
		super(parent, "Document Browser");
		init(parent);
	}

	private void init(Window parent) {
		FLApp.getSingle().addGameListener(this);
		getContentPane().setLayout(new BorderLayout());
		elementTree = new JTree();
		elementTree.addTreeSelectionListener(this);
		attTable = new JTable();
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				new JScrollPane(elementTree),
				new JScrollPane(attTable));
		getContentPane().add(splitPane);
		eventOccurred(new GameEvent(GameEvent.NEW_SECTION)); // immediately show current section
		pack();
		setSize(400, 400);
		setLocationRelativeTo(parent);
		splitPane.setDividerLocation(0.7);
		setVisible(true);
	}

	@Override
	public void eventOccurred(GameEvent evt) {
		if (evt.getID() != GameEvent.NEW_SECTION) return;
		SectionDocument doc = FLApp.getSingle().getCurrentDocument();
		if (doc == null) return;
		setDocument(doc);
	}

	public void setDocument(Document doc) {
		Element[] rootElements = doc.getRootElements();
		if (rootElements.length > 1) {
			System.out.println("More than one root element in SectionDocument!");
			for (int e = 0; e < rootElements.length; e++)
				System.out.println("Root " + (e+1) + ": " + rootElements[e]);
		}
		if (rootElements.length > 0 && rootElements[0] instanceof TreeNode)
			elementTree.setModel(new DefaultTreeModel((TreeNode)rootElements[0]));
	}

	private TableModel makeTableModel(String content, AttributeSet atts) {
		DefaultTableModel model = new DefaultTableModel();
		model.addColumn("Attribute");
		model.addColumn("Value");
		if (content != null)
			model.addRow(new Object[] {"[Content]", convertString(content)});
		for (Enumeration<?> a = atts.getAttributeNames(); a.hasMoreElements(); ) {
			Object att = a.nextElement();
			Object val = atts.getAttribute(att);
			if (val instanceof String) val = convertString((String)val);
			model.addRow(new Object[] {att, val});
		}
		return model;
	}

	private static String convertString(String str) {
		StringBuilder sb = new StringBuilder();
		for (int c = 0; c < str.length(); c++) {
			switch (str.charAt(c)) {
			case '\t': sb.append("\\t"); break;
			case '\n': sb.append("\\n"); break;
			default: sb.append(str.charAt(c));
			}
		}
		return sb.toString();
	}

	@Override
	public void valueChanged(TreeSelectionEvent evt) {
		Object selectedNode = evt.getPath().getLastPathComponent();
		if (selectedNode instanceof Element) {
			Element node = (Element)selectedNode;
			AttributeSet atts = node.getAttributes();
			String textContent = null;
			try {
				textContent = node.getDocument().getText(node.getStartOffset(),
						node.getEndOffset() - node.getStartOffset());
			}
			catch (BadLocationException ble) {
				System.out.println("Bad document position: " + ble.offsetRequested());
			}
			attTable.setModel(makeTableModel(textContent, atts));
		}
	}
}
