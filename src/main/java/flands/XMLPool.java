package flands;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;

import org.xml.sax.SAXException;

import javax.xml.parsers.SAXParser;

/**
 * Object that holds other objects that will be saved in XML format. Currently, these
 * objects are the ItemLists (possessions and caches), money caches and current curses.
 * The objects are loaded again via LoadableNode.
 * @author Jonathan Mann
 */
public class XMLPool implements Loadable, XMLOutput {
	private static XMLPool single = null;
	static XMLPool createPool(Adventurer adv) {
		single = new XMLPool(adv);
		return single;
	}
	public static XMLPool getPool() {
		return single;
	}

	private Adventurer adv;
	private XMLPool(Adventurer adv) {
		this.adv = adv;
	}

	public Adventurer getAdventurer() { return adv; }

	@Override
	public String getFilename() {
		return "saved.xml";
	}

	@Override
	public boolean loadFrom(InputStream in) throws IOException {
		try {
			SAXParser parser = FLApp.createSAXParser();
			ParserHandler handler = new ParserHandler();
			parser.parse(in, handler);

			return true;
		}
		catch (SAXException sax) {
			sax.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean saveTo(OutputStream out) throws IOException {
		PrintStream pout = new PrintStream(out);
		outputTo(pout, "", XMLOutput.OUTPUT_PROPS_DYNAMIC | XMLOutput.OUTPUT_PROPS_STATIC);
		return true;
	}

	@Override
	public String getXMLTag() {
		return "saved";
	}

	@Override
	public void storeAttributes(Properties atts, int flags) {}

	@Override
	public Iterator<XMLOutput> getOutputChildren() {
		LinkedList<XMLOutput> l = new LinkedList<>();
		l.add(getAdventurer().getCurses());
		l.add(getAdventurer().getItems());
		// do curses before items, because cursed items are included in the item list
		// (and we don't want their curses to get removed when curses are initialised).
		for (Iterator<ItemList> i = CacheNode.getItemCaches(); i.hasNext(); ) {
			ItemList il = i.next();
			if (il.getItemCount() > 0)
				l.add(il);
		}
		for (Iterator<Map.Entry<String,Integer>> i = CacheNode.getMoneyCaches(); i.hasNext(); )
			l.add(new MoneyCacheOutput(i.next()));
		return l.iterator();
	}

	@Override
	public void outputTo(PrintStream out, String indent, int flags) throws IOException {
		Node.output(this, out, indent, flags);
	}

	private static class MoneyCacheOutput implements XMLOutput {
		private Map.Entry<String,Integer> cache;
		private MoneyCacheOutput(Map.Entry<String,Integer> cache) {
			this.cache = cache;
		}
		@Override
		public String getXMLTag() {
			return "moneycache";
		}

		@Override
		public void storeAttributes(Properties atts, int flags) {
			atts.setProperty("name", cache.getKey());
			atts.setProperty("shards", cache.getValue().toString());
		}

		@Override
		public Iterator<XMLOutput> getOutputChildren() {
			return null;
		}

		@Override
		public void outputTo(PrintStream out, String indent, int flags) throws IOException {
			if (cache.getValue() > 0)
				Node.output(this, out, indent, flags);
		}
		
	}
	}
