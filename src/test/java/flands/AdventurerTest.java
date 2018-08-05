package flands;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;

import java.awt.BorderLayout;
import java.util.Random;

public class AdventurerTest {
	public static void main(String args[]) {
		Adventurer[] advs = Adventurer.loadStarting(Books.getCanon().getBook(args.length > 0 ? args[0] : "5"));
		for (int a = 0; a < advs.length; a++) {
			System.out.println("Adventurer " + (a+1) + ":");
			System.out.println(advs[a].toDebugString());
			Document history = advs[a].getHistoryDocument();
			if (history != null) {
				System.out.print("History:");
				if (history instanceof AbstractDocument)
					((AbstractDocument)history).dump(System.out);
				else
					System.out.println(history.toString());
			}
			advs[a].getCodewords().addCodeword("Codeword" + a);
		}

		JFrame jf = new JFrame("Starting Adventurers");
		final JTextPane textPane = new JTextPane();
		textPane.setDocument(advs[0].getHistoryDocument());
		final JComboBox<Adventurer> jcb = new JComboBox<>();
		for (Adventurer adv : advs) {
			jcb.addItem(adv);
		}
		jcb.setSelectedItem(advs[0]);
		jcb.addActionListener(e -> {
			Adventurer a = jcb.getItemAt(jcb.getSelectedIndex());
			if (a.getHistoryDocument() != null)
				textPane.setDocument(a.getHistoryDocument());
		});

		jf.getContentPane().add(new JScrollPane(textPane));
		jf.getContentPane().add(jcb, BorderLayout.SOUTH);
		jf.setSize(200, 200);
		jf.setVisible(true);
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		int randomAdv = new Random().nextInt(advs.length);
		AdventurerFrame af = new AdventurerFrame();
		advs[randomAdv].addTitle("Masked Lady");
		af.init(advs[randomAdv]);
		af.pack();
		af.setVisible(true);
	}
}
