package flands;

public class ExpressionTest {
	public static void main(String args[]) {
		for (int a = 0; a < args.length; a++) {
			String arg = args[a];
			Expression exp = new Expression(arg);
			System.out.println("Expression " + a + "=" + exp.getRoot());
		}
	}
}
