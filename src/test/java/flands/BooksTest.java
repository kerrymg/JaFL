package flands;

public class BooksTest {
	public static void main(String args[]) {
		Books books = Books.getCanon();
		for (String arg : args) {
			Books.BookDetails book = books.getBook(arg);
			if (book.hasBook()) {
				String[] codewords = book.getOfficialCodewords();
				System.out.println("Official codewords for book " + arg + ": " + codewords.length);
				for (int c = 0; c < codewords.length; c++)
					System.out.println((c + 1) + ": " + codewords[c]);
			}
		}
	}
}
