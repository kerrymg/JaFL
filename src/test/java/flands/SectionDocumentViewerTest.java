package flands;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SectionDocumentViewerTest {
    private static final boolean method = true;
    public static void main(String args[]) {
        String htmlText = "<html><body><p>Para 1</p>" +
                "<p><a href=\"http://www.abc.net.au\">This is <b>a link</b></a>.</p>" +
                "<table><tr><th>Header 1</th><th>Header 2</th></tr><tr><td>Cell 1</td><td>Cell 2</td></tr></table></body></html>";
        JFrame frame = new JFrame("HTML Doc");
        final JEditorPane tp;
        if (method) {
            tp = new JEditorPane("text/html", htmlText);
        }
        else {
            tp = new JEditorPane();
            DefaultStyledDocument doc = new DefaultStyledDocument();
            try {
                doc.insertString(0, "This is the first paragraph.\nAnd this is the second.", null);
            }
            catch (BadLocationException ble) {}
            tp.setDocument(doc);
        }
        frame.getContentPane().add(new JScrollPane(tp));
        JButton refreshButton = new JButton("Refresh");
        JPanel buttonPane = new JPanel();
        buttonPane.add(refreshButton);
        frame.getContentPane().add(buttonPane, BorderLayout.SOUTH);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                System.exit(0);
            }
        });

        final SectionDocumentViewer viewer = new SectionDocumentViewer(frame);
        viewer.setDocument(tp.getDocument());

        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                viewer.setDocument(tp.getDocument());
            }
        });
    }
}
