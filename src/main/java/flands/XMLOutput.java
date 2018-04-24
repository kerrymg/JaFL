package flands;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Properties;

/**
 * Interface for objects wanting to store themselves in an XML format.
 * Used primarily to store Effects (as part of curses or items).
 * 
 * @author Jonathan Mann
 */
public interface XMLOutput {
	int OUTPUT_PROPS_STATIC = 1 << 0;
	int OUTPUT_PROPS_DYNAMIC = 1 << 1;
	String getXMLTag();
	void storeAttributes(Properties atts, int flags);
	Iterator<XMLOutput> getOutputChildren();
	void outputTo(PrintStream out, String indent, int flags) throws IOException;
}
