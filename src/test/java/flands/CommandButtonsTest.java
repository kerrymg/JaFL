package flands;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static flands.CommandButtons.APPLY_RESET_CLOSE_HELP;

public class CommandButtonsTest {
    /* ***********
     * Test method
     *********** */
    public static void main(String args[]) {
        CommandButtons row =
                CommandButtons.createRow(APPLY_RESET_CLOSE_HELP,
                        new ActionListener() {
                            public void actionPerformed(ActionEvent evt) {
                                System.out.println("Action command=" + evt.getActionCommand());
                            }
                        });
        row.addButton(new javax.swing.JButton("Bogus"));

        javax.swing.JFrame f = new javax.swing.JFrame("Test");
        f.getContentPane().add(row);
        f.pack();
        f.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }
}
