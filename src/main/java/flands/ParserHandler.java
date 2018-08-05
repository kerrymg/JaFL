package flands;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.LinkedList;

/**
 * The standard SAX handler when parsing section XML files.
 * @author Jonathan Mann
 */
public class ParserHandler extends DefaultHandler {
	private Node rootNode;
	private final boolean startExecution;
	private String book;

	ParserHandler() {
		this(false);
	}

	ParserHandler(boolean startExecution) {
		this.startExecution = startExecution;
	}

	public SectionDocument getDocument() { return rootNode.getDocument(); }
	Node getRootNode() { return rootNode; }
	/**
	 * Set the book of the section being loaded. This will be passed to the
	 * SectionNode at creation.
	 */
	public void setBook(String book) { this.book = book; }

	/* **************
	 * ContentHandler
	 ************** */
	@Override
	public void startDocument() {
		System.out.println("startDocument()");

		// Reset any variables - if there was an error in the last parse,
		// these may not be in the right state.
		rootNode = null;
		nodeStack.clear();
	}

	// Tracks whether the current Node contains any textual content - if not, it will have
	// handleContent() called with an empty string before handleEndTag(). This way each
	// Node is guaranteed to get a call to handleContent() (where it can create default content).
	private boolean emptyTag;
	private boolean trimContentStart = true;
	private LinkedList<Node> nodeStack = new LinkedList<>();
	private StringBuffer accumulatedContent = new StringBuffer();

	private Node getCurrentNode() { return (nodeStack.isEmpty()) ? null : nodeStack.getFirst(); }
	private void pushNode(Node node) { nodeStack.addFirst(node); }
	private Node popNode() { return (nodeStack.isEmpty()) ? null : nodeStack.removeFirst(); }

	private static final char OpenQuote = '\u2018';
	private static final char CloseQuote = '\u2019';
	/**
	 * Convert any open and close quotes in a string into the standard apostrophe.
	 * This makes item matching a lot easier.
	 */
	public static String simplifyQuotes(String text) {
		if (text.indexOf(OpenQuote) < 0 && text.indexOf(CloseQuote) < 0)
			return text;

		StringBuilder sb = new StringBuilder(text);
		for (int i = 0; i < sb.length(); i++) {
			char ch = sb.charAt(i);
			if (ch == OpenQuote || ch == CloseQuote)
				sb.setCharAt(i, '\'');
		}
		return sb.toString();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) {
		condenseContent(accumulatedContent);
		String contentStr = accumulatedContent.toString();
		if (contentStr.length() > 0) {
			// Pass it in now
			Node currentNode = getCurrentNode();
			if (currentNode != null)
				currentNode.handleContent(contentStr);
			accumulatedContent.setLength(0);
		}

		emptyTag = true;
		trimContentStart = true;
		localName = localName.toLowerCase();

		Node newNode = Node.createNode(localName, getCurrentNode());
		if (newNode != null) {
			if (nodeStack.size() == 0 && book != null && newNode instanceof SectionNode) {
				// Set the book immediately
				SectionNode root = (SectionNode)newNode;
				root.setBook(book);
			}

			pushNode(newNode);
			newNode.init(atts);
		}

		StringBuilder sb = new StringBuilder("startElement(");
		sb.append('"').append(uri).append('"');
		sb.append(',');
		sb.append('"').append(localName).append('"');
		sb.append(',');
		sb.append('"').append(qName).append('"');
		sb.append(',');
		sb.append("attributes[");
		for (int i = 0; i < atts.getLength(); i++) {
			if (i > 0)
				sb.append(',');
			sb.append(atts.getLocalName(i));
			sb.append('=');
			sb.append(atts.getValue(i));
		}
		sb.append("])");
		System.out.println(sb.toString());
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		condenseContent(accumulatedContent);
		String contentStr = Node.trimEnd(accumulatedContent.toString());
		if (contentStr.length() > 0 || emptyTag)
			getCurrentNode().handleContent(contentStr);
		accumulatedContent.setLength(0);
		emptyTag = false;
		trimContentStart = false;

		Node n = popNode();
		boolean contentAdded = n.handleEndTag();
		if (!contentAdded) trimContentStart = true;
		if (nodeStack.size() == 0) {
			rootNode = n;
			if (startExecution && rootNode instanceof SectionNode) {
				((SectionNode)rootNode).startExecution();
			}
		}
		System.out.println("endElement(" + uri + "," + localName + "," + qName + ")");
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		if (length > 0) {
			// Not sure why this would get called otherwise, but just in case
			if (trimContentStart)
				accumulatedContent.append(Node.trimStart(new String(ch, start, length)));
			else
				accumulatedContent.append(ch, start, length);
			trimContentStart = false;
			emptyTag = false;
		}

		//System.out.println("characters(\"" + convertString(new String(ch, start, length)) + "\", range[" + start + "-" + (start+length-1) + "] of " + ch.length + ")");
	}
	/*
	private static String convertString(String str) {
		int index;
		while ((index = str.indexOf('\n')) >= 0)
			str = str.substring(0, index) + "\\n" + str.substring(index + 1);
		while ((index = str.indexOf('\t')) >= 0)
			str = str.substring(0, index) + "\\t" + str.substring(index + 1);
		while ((index = str.indexOf('\r')) >= 0)
			str = str.substring(0, index) + str.substring(index+1);
		return str;
	}
	*/
	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) {
		System.out.println("ignorableWhitespace('" + new String(ch) + "'," + start + "," + length + ")");
	}
	@Override
	public void processingInstruction(String target, String data) {
		System.out.println("processingInstruction(" + target + "," + data + ")");
	}
	@Override
	public void skippedEntity(String name) {
		System.out.println("skippedEntity(" + name + ")");
	}

	/**
	 * Removes excess whitespace, converts multiple dashes into a single mdash,
	 * and replaces triple periods with an ellipsis.
	 */
	private static void condenseContent(StringBuffer text) {
		for (int i = 0; i < text.length(); i++) {
			if (Character.isWhitespace(text.charAt(i))) {
				while (i+1 < text.length() && Character.isWhitespace(text.charAt(i+1)))
					text.deleteCharAt(i+1);
				text.setCharAt(i, ' '); // rather than other whitespace characters
			}
		}

		int index = text.indexOf("-");
		while (index >= 0) {
			if (index > 0 && text.charAt(index-1) == ' ' &&
				index < text.length() - 1 && text.charAt(index+1) == ' ')
				text.setCharAt(index, '\u2013');
			index = text.indexOf("-", index+1);
		}
		while (true) {
			index = text.indexOf("...");
			if (index < 0)
				break;
			text.replace(index, index + 3, "\u2026");
		}
	}
}
