package flands;

import java.awt.Image;

public class ImageWindowTest {
    public static void main(String args[]) {
        Image i;
        if (args.length == 0) {
            System.out.println("Usage: ImageWindow <image-filename>");
            return;
        }

        javax.swing.JFrame jf = new javax.swing.JFrame("Test");
        jf.setSize(100, 100);
        jf.setVisible(true);
        jf.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

        //i = jf.getToolkit().createImage(args[0]);
        i = jf.getToolkit().createImage(args[0]);
        ImageWindow window = new ImageWindow(jf, i, args[0]);
        window.setVisible(true);
    }
}
