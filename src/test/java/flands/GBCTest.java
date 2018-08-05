package flands;

public class GBCTest {
	public static void main(String args[]) {
		for (int a = 0; a < args.length; a++) {
			if (a > 0)
				System.out.println();
			System.out.println("Params=" + args[a]);
			GBC gbc = new GBC();
			gbc.setValues(args[a]);
			System.out.println("GBC=" + gbc);

			if (args.length == 1) {
				long startms = System.currentTimeMillis();
				for (int i = 0; i < 100; i++)
					gbc.setValues(args[0]);
				long endms = System.currentTimeMillis();
				System.out.println("Parse time=" + (endms-startms/(double)100));
			}
		}
	}
}
