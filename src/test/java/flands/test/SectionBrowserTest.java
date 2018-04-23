package flands.test;

import flands.Adventurer;
import flands.FLApp;
import flands.SectionBrowser;

import javax.swing.JFrame;

public class SectionBrowserTest {
    public static void main(String args[]) {
        FLApp.getSingle().init(null);
        FLApp.getSingle().showProfession((int)(Math.random() * Adventurer.PROF_COUNT));
        FLApp.getSingle().setVisible(false);

        SectionBrowser sb = null;
        if (args.length > 0)
            try {
                sb = new SectionBrowser(null, false, Integer.parseInt(args[0]));
            }
            catch (NumberFormatException ignored) {}
        if (sb == null)
            sb = new SectionBrowser(null, true, 100);
        JFrame jf = sb.createFrame("Section Browser");
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.setVisible(true);
    }
}
