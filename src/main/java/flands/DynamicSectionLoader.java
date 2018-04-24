package flands;


import java.util.LinkedList;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

/**
 * Handles loading a saved section (and its working state) from XML.
 * @author Jonathan Mann
 */
public class DynamicSectionLoader implements ContentHandler {
	private static class StackEntry {
		private Node n;
		private int childIndex;
		private StackEntry(Node n) {
			this.n = n;
			childIndex = 0;
		}
	}

	private LinkedList<StackEntry> stack = new LinkedList<>();
	private SectionNode rootNode;
	DynamicSectionLoader(SectionNode node) {
		rootNode = node;
	}

	/* *******************************
	 * Relevant ContentHandler methods
	 ******************************* */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) {
		Node childNode;
		if (stack.isEmpty()) {
			childNode = rootNode;
		}
		else {
			StackEntry entry = stack.getLast();
			childNode = entry.n.getChild(entry.childIndex++);
		}
		childNode.loadProperties(atts);
		stack.addLast(new StackEntry(childNode));
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		stack.removeLast();
	}

	/* *********************************
	 * Irrelevant ContentHandler methods
	 ********************************* */
	@Override
	public void setDocumentLocator(org.xml.sax.Locator l) {
		//System.out.println("setDocumentLocator(" + l + ")");
	}
	@Override
	public void startDocument() {
		//System.out.println("startDocument()");
	}
	@Override
	public void endDocument() {
		//System.out.println("endDocument()");
	}
	@Override
	public void startPrefixMapping(String prefix, String uri) {
		//System.out.println("startPrefixMapping(" + prefix + "," + uri + ")");
	}
	@Override
	public void endPrefixMapping(String prefix) {
		//System.out.println("endPrefixMapping(" + prefix + ")");
	}
	@Override
	public void characters(char[] ch, int start, int length) {
		//System.out.println("characters(" + length + ")");
	}
	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) {
		//System.out.println("ignorableWhitespace('" + new String(ch) + "'," + start + "," + length + ")");
	}
	@Override
	public void processingInstruction(String target, String data) {
		//System.out.println("processingInstruction(" + target + "," + data + ")");
	}
	@Override
	public void skippedEntity(String name) {
		//System.out.println("skippedEntity(" + name + ")");
	}
}
