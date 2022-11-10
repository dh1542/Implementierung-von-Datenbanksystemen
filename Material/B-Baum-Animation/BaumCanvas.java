
import java.awt.Canvas;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.util.Enumeration;
import java.util.Vector;

public class BaumCanvas extends Canvas {

    public BaumCanvas(BBaum b_baum) {
        container = b_baum;
        setBackground(Color.white);
    }

    public void update(Graphics g) {
        paint(g);
    }

    public void paint(Graphics g) {
        offScreenImage = createImage(this.getSize().width, this.getSize().height);
        offScreenGraphics = offScreenImage.getGraphics();
        int i = 2;       
        
        fmetrics = getFontMetrics(getFont());        
        höhe = fmetrics.getHeight() + 2;
        breite = 2 * fmetrics.stringWidth("9") + 8;
        int j = this.getSize().width;
        int k = 2 * Knoten.k * breite;
        BBaum.wurzel.view.x = j / 2 - k / 2;
        BBaum.wurzel.view.y = i;
        Vector vector = new Vector();
        vector.addElement(BBaum.wurzel);
        Vector vector2;
        for (Vector vector1 = BBaum.wurzel.alleSöhne(); vector1.size() > 0; vector1 = vector2) {
            vector2 = new Vector();
            i += höhe + 20;
            boolean flag = vector1.size() * k < j - vector1.size();
            Knoten knoten = ((Knoten) vector1.firstElement()).vorgE4nger;
            int l = i;
            int i1 = i;
            for (int j1 = 0; j1 < vector1.size(); j1++) {
                Knoten knoten1 = (Knoten) vector1.elementAt(j1);
                if (flag) {
                    knoten1.view.x = (j / vector1.size() / 2 - k / 2) + j1 * (j / vector1.size());
                    knoten1.view.y = i;
                } else {
                    knoten1.view.x = j1 * ((j - k) / Math.max(vector1.size() - 1, 1));
                    if (knoten1.vorgE4nger == knoten) {
                        if (l > i1) i1 = l;
                        knoten1.view.y = l;
                    } else {
                        knoten1.view.y = l = i;
                        knoten = knoten1.vorgE4nger;
                    }
                    l += höhe + 10;
                }
                vector.addElement(knoten1);
                for (Enumeration enumeration1 = knoten1.alleSöhne().elements(); enumeration1.hasMoreElements(); vector2
                        .addElement(enumeration1.nextElement()));
            }

            i = i1;
        }

        offScreenGraphics.setColor(Color.black);
        for (Enumeration enumeration = vector.elements(); enumeration.hasMoreElements(); ((Knoten) enumeration
                .nextElement()).view.zeichne(this));
        zeichneOffScreenBuffer();
    }

    protected void zeichneOffScreenBuffer() {
        getGraphics().drawImage(offScreenImage, 0, 0, this);
    }

    private BBaum container;
    private Image offScreenImage;
    protected Graphics offScreenGraphics;
    protected FontMetrics fmetrics;
    protected int höhe;
    protected int breite;
    private static final int DISTANZ_Y = 20;
    private static final int ABSTAND_HORIZ = 8;
    private static final int ABSTAND_VERT = 2;
}