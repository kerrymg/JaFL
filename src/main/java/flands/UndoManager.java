package flands;


import java.util.LinkedList;

/**
 * Central point for collecting undoable events.
 * @author Jonathan Mann
 */
public class UndoManager {
	public interface Creator {
		void undoOccurred(UndoManager undo);
	}

	public static class NullCreator implements Creator {
		@Override
		public void undoOccurred(UndoManager undo) {}
	}

	private static UndoManager single = null;
	static UndoManager createNew(Creator creator) {
		single = new UndoManager(creator);
		return single;
	}
	static UndoManager createNull() {
		return createNew(new NullCreator());
	}
	public static UndoManager getCurrent() {
		if (single == null)
			return createNew(undo -> {});
		else
			return single;
	}

	private final Creator creator;
	private LinkedList<Executable> executables = new LinkedList<>();
	private int ignoreCalls = 0;

	private UndoManager(Creator creator) {
		this.creator = creator;
	}

	public Creator getCreator() { return creator; }

	void ignoreCalls(boolean b) {
		System.out.println("UndoManager.ignoreCalls(" + b + ")");
		if (b)
			ignoreCalls++;
		else {
			ignoreCalls--;
			if (ignoreCalls < 0) {
				// Not an error
				System.out.println("UndoManager.setIgnoreCalls(false) called more than (true)");
				ignoreCalls = 0;
			}
		}
	}

	public void add(Executable e) {
		if (ignoreCalls == 0)
			executables.addFirst(e);
	}

	public void undo() {
		for (Executable executable : executables)
			executable.resetExecute();
		executables.clear();
		creator.undoOccurred(this);
	}
}
