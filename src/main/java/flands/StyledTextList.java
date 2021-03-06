package flands;


import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * A utility class that should have been built far earlier in the project than it was.
 * Oh well, you grab ideas when they come to you.
 * @author Jonathan Mann
 */
public class StyledTextList implements XMLOutput {
	private LinkedList<StyledText> list = new LinkedList<>();
	private boolean abilityChecked = true;

	public int getSize() { return list.size(); }
	private StyledText getStyledText(int index) { return list.get(index); }
	public Iterator<StyledText> iterator() { return list.iterator(); }

	public void add(String text, AttributeSet atts) {
		add(new StyledText(text, atts));
	}

	void addAbilityName(String name, AttributeSet atts) {
		add(new StyledText(name.toUpperCase(), atts)); // and let's see if it works
		/*
		boolean temp = abilityChecked;
		name = name.toUpperCase();
		SimpleAttributeSet smallerAtts = SectionDocument.getSmallerAtts(atts);
		add(name.substring(0, 1), atts);
		add(name.substring(1), smallerAtts);
		abilityChecked = temp;
		*/
	}

	public void add(StyledText st) {
		abilityChecked = false;
		if (list.size() > 0) {
			StyledText lastSt = list.getLast();
			if (st.atts == lastSt.atts ||
				(st.atts != null && lastSt.atts != null && st.atts.equals(lastSt.atts))) {
				lastSt.text = lastSt.text + st.text;
				return;
			}
		}
		list.addLast(st);
	}

	public void add(StyledTextList list2) {
		if (list2.getSize() > 0) {
			add(list2.getStyledText(0));
			Iterator<StyledText> i = list2.iterator();
			i.next();
			while (i.hasNext())
				list.addLast(i.next());
		}
	}

	static AttributeSet combine(AttributeSet atts1, AttributeSet atts2) {
		if (atts2 == null)
			return atts1;
		else if (atts1 == null)
			return atts2;
		
		SimpleAttributeSet atts = new SimpleAttributeSet(atts1);
		atts.addAttributes(atts2);
		return atts;
	}

	public void add(StyledTextList list2, AttributeSet atts) {
		if (list2.getSize() > 0) {
			add(list2.getStyledText(0).text, combine(list2.getStyledText(0).atts, atts));
			Iterator<StyledText> i = list2.iterator();
			i.next();
			while (i.hasNext()) {
				StyledText st = i.next();
				list.addLast(new StyledText(st.text, combine(st.atts, atts)));
			}
		}
	}
	
	public StyledText getLast() { return list.getLast(); }

	boolean addEffects(Effect e, AttributeSet atts, boolean startWithComma) {
		boolean addedAnything = false;
		Effect currentEffect = e;
		while (currentEffect != null) {
			if (startWithComma)
				add(", ", atts);
			if (currentEffect.addTo(this, atts)) {
				addedAnything = true;
				startWithComma = true;
			}
			else
				startWithComma = false;
			currentEffect = currentEffect.nextEffect();
		}

		if (getSize() > 0) {
			StyledText st = getLast();
			if (st.text.endsWith(", "))
				st.text = st.text.substring(0, st.text.length() - 2);
		}

		return addedAnything;
	}

	private void formatAbilityNames() {
		if (!abilityChecked) {
			String[] abilityNames = SectionDocument.getCapsWords();
			for (int i = 0; i < list.size(); i++) {
				StyledText st = list.get(i);
				for (String abilityName : abilityNames) {
					int index = st.text.indexOf(abilityName);
					if (index >= 0) {
						AttributeSet atts = st.atts;
						SimpleAttributeSet smallerAtts = SectionDocument.getSmallerAtts(atts);
						String remainder = st.text.substring(index + abilityName.length());
						st.text = st.text.substring(0, index + 1);
						list.add(i + 1, new StyledText(abilityName.substring(1), smallerAtts));
						list.add(i + 2, new StyledText(remainder, atts));
					}
				}
			}
			abilityChecked = true;
		}
	}

	public Element[] addTo(SectionDocument doc, Element parent) {
		if (list.size() > 0) {
			//formatAbilityNames(); // SectionDocument already does this!
			StyledText[] stArr = list.toArray(new StyledText[0]);
			return doc.addLeavesTo(parent, stArr);
		}
		else
			return new Element[0];
	}

	public void addTo(DefaultStyledDocument doc, boolean replace) {
		formatAbilityNames();
		try {
			Iterator<StyledText> i = list.iterator();
			StyledText st;
			if (i.hasNext()) {
				if (replace) {
					st = i.next();
					doc.replace(0, doc.getLength(), st.text, st.atts);
				}
				while (i.hasNext()) {
					st = i.next();
					doc.insertString(doc.getLength(), st.text, st.atts);
				}
			}
			else if (replace)
				doc.remove(0, doc.getLength());
		}
		catch (BadLocationException ble) {
			System.err.println("StyledTextList.addTo(DSD): insertString error: " + ble);
		}
	}

	String toXML() {
		StringBuilder sb = new StringBuilder();

		boolean bold = false, italic = false, underline = false;
		for (Iterator<StyledText> it = iterator(); it.hasNext(); ) {
			StyledText st = it.next();
			AttributeSet atts = st.atts;

			boolean b, i, u;
			if (atts == null)
				b = i = u = false;
			else {
				b = StyleConstants.isBold(atts);
				i = StyleConstants.isItalic(atts);
				u = StyleConstants.isUnderline(atts);
			}
			if (u && !underline)
				sb.append("<u>");
			if (i && !italic)
				sb.append("<i>");
			if (b && !bold)
				sb.append("<b>");
			if (!b && bold)
				sb.append("</b>");
			if (!i && italic)
				sb.append("</i>");
			if (!u && underline)
				sb.append("</u>");
			bold = b; italic = i; underline = u;

			sb.append(st.text);
		}

		if (bold)
			sb.append("</b>");
		if (italic)
			sb.append("</i>");
		if (underline)
			sb.append("</u>");

		return sb.toString();
	}

	@Override
	public String getXMLTag() { return "desc"; } // not that it matters
	@Override
	public void storeAttributes(Properties atts, int flags) {}
	@Override
	public Iterator<XMLOutput> getOutputChildren() { return null; }
	@Override
	public void outputTo(PrintStream out, String indent, int flags) {
		out.print(indent);
		out.print("<desc>");
		out.print(toXML());
		out.println("</desc>");
	}
}
