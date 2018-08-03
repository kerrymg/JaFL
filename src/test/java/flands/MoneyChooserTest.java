package flands;

import java.awt.Frame;

public class MoneyChooserTest {
    public static void main(String args[]) {
        Frame f = new Frame("Test");
        f.setBounds(100, 100, 50, 50);
        f.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.exit(0);
            }
        });
        f.setVisible(true);

        MoneyChooser chooser = new MoneyChooser(f, "Withdraw Money", "Choose the amount to withdraw:", 0, 10000, 0.1f, 100);
        chooser.setVisible(true);
        System.out.println("Amount chosen=" + chooser.getResult());
        System.out.println("Amount minus charge=" + chooser.getResultLessCharge());
    }
}
