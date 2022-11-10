
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Scrollbar;
import java.awt.TextArea;
import java.awt.TextField;

import javax.swing.JFrame;

public class BBaum extends Applet {

    public BBaum() {}

    public void init() {
        label = new Label("k = 1", 2);
        Options options = new Options(this);
        options.setLayout(new FlowLayout(1, 10, 10));
        setBackground(Color.lightGray);
        newTree = new Button("New");
        insert = new Button("Insert");
        delete = new Button("Delete");
        eingabe = new TextField("1", 2);
        ausgabe = new TextArea(4, 45);
        ausgabe.setEditable(false);
        exit = new Button("EXIT");
        options.add(ausgabe);
        options.add(label);
        options.add(new Scrollbar(0, 1, 1, 1, 4));
        options.add(newTree);
        options.add(delete);
        options.add(insert);
        options.add(eingabe);
        options.add(exit);
        setLayout(new BorderLayout(10, 10));
        add("North", options);
        baumcanvas = new BaumCanvas(this);
        add("Center", baumcanvas);
        wurzel = new Knoten(this);
    }

    public Insets insets() {
        return new Insets(10, 10, 10, 10);
    }

    public boolean handleEvent(Event event) {
        if (event.target instanceof Scrollbar) {
            int i = ((Scrollbar) event.target).getValue();
            label.setText("k = " + String.valueOf(i));
            k = i;
            return true;
        } else {
            return false;
        }
    }

    public boolean action(Event event, Object obj) {
        if (event.target == newTree) {
            wurzel.dereferenziere();
            wurzel = new Knoten(this);
            Knoten.k = k;
            baumcanvas.repaint();
            ausgabe.insert("Baum geloescht!\n", 0);
        } else if (event.target == exit) {
            System.exit(0);
        } else if (event.target == insert)
            try {
                int i = Integer.parseInt(eingabe.getText());
                Integer newVal = new Integer(i + 1);
                eingabe.setText(newVal.toString());
                if (i < EINGABE_MIN || i > EINGABE_MAX) {
                    ausgabe.insert(EINGABE_FEHLER, 0);
                } else {
                    boolean flag = wurzel.insert(i);
                    if (flag) {
                        baumcanvas.repaint();
                        ausgabe.insert("insert(" + i + ")\n", 0);
                    } else {
                        ausgabe.insert(i + " ist schon im Baum enthalten!\n", 0);
                    }
                }
            } catch (NumberFormatException _ex) {
                ausgabe.insert(EINGABE_FEHLER, 0);
            }
        else if (event.target == delete)
            try {
                int j = Integer.parseInt(eingabe.getText());
                if (j < EINGABE_MIN || j > EINGABE_MAX) {
                    ausgabe.insert(EINGABE_FEHLER, 0);
                } else {
                    boolean flag1 = wurzel.delete(j);
                    if (flag1) {
                        baumcanvas.repaint();
                        ausgabe.insert("delete(" + j + ")\n", 0);
                    } else {
                        ausgabe.insert(j + " ist nicht im Baum enthalten!\n", 0);
                    }
                }
            } catch (NumberFormatException _ex) {
                ausgabe.insert(EINGABE_FEHLER, 0);
            }
        else return false;
        return true;
    }

    public static void main(String args[]) {
        BBaum b_baum = new BBaum();
        JFrame mainFrame = new JFrame("B-Baum");
        b_baum.init();
        mainFrame.add("Center", b_baum);
        mainFrame.setSize(800, 600);
        mainFrame.setVisible(true);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private static Button newTree;
    private static Button insert;
    private static Button delete;
    private static Button exit;
    private static final int ABSTAND = 10;
    private Label label;
    private TextField eingabe;
    private TextArea ausgabe;
    private static final int ZEILEN = 4;
    private static final int SPALTEN = 45;
    protected BaumCanvas baumcanvas;
    protected static final int STELLIGKEIT = 2;
    private static final int EINGABE_MAX;
    private static final int EINGABE_MIN;
    private static final int K_MIN = 1;
    private static final int K_MAX = 4;
    protected static final int K_INITIAL = 1;
    private static final String EINGABE_FEHLER;
    private static final String SCHON_ENTHALTEN = " ist schon im Baum enthalten!\n";
    private static final String NICHT_ENTHALTEN = " ist nicht im Baum enthalten!\n";
    private static final String NEW_TEXT = "Baum geloescht!\n";
    private static int k = 1;
    public static Knoten wurzel;

    static {
        EINGABE_MAX = (int) Math.pow(10D, 2D) - 1;
        EINGABE_MIN = -((int) Math.pow(10D, 1.0D) - 1);
        EINGABE_FEHLER = "Bitte nur Integerwerte zwischen " + EINGABE_MIN + " und " + EINGABE_MAX + " eingeben!\n";
    }
}