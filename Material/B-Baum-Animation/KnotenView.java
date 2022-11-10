// Decompiled by Jad v1.5.7f. Copyright 2000 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   KnotenView.java

import java.awt.Color;
import java.awt.Graphics;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

public class KnotenView implements Observer {
    
    //Das Blinken eines Knotens dauert zweimal blinkTime in msec
    private int blinkTime = 500;
    //So oft blinkt ein Knoten
    private int blinkNo = 3;

    public KnotenView(Knoten knoten) {
        model = knoten;
    }

    public void update(Observable observable, Object obj) {
        if (obj instanceof BaumCanvas) animation((BaumCanvas) obj);
    }

    private synchronized void animation(BaumCanvas baumcanvas) {
        Graphics g = baumcanvas.offScreenGraphics;
        for (int i = 0; i < blinkNo; i++) {
            g.setColor(Color.black);
            g.fillRect(x + 2, y + 2, 2 * Knoten.k * baumcanvas.breite - 2, baumcanvas.höhe - 2);
            g.setColor(Color.white);
            zeichneTrennstriche(baumcanvas);
            zeichneSchlüssel(baumcanvas);
            baumcanvas.zeichneOffScreenBuffer();
            baumcanvas.repaint(x, y, 2 * Knoten.k * baumcanvas.breite, baumcanvas.höhe);
            try {
                wait(blinkTime);
            } catch (InterruptedException _ex) {}
            g.setColor(Color.white);
            g.fillRect(x + 2, y + 2, 2 * Knoten.k * baumcanvas.breite - 2, baumcanvas.höhe - 2);
            g.setColor(Color.black);
            zeichne(baumcanvas);
            baumcanvas.zeichneOffScreenBuffer();
            baumcanvas.repaint(x, y, 2 * Knoten.k * baumcanvas.breite, baumcanvas.höhe);
            try {
                wait(blinkTime);
            } catch (InterruptedException _ex) {}
        }

    }

    public void zeichne(BaumCanvas baumcanvas) {
        Graphics g = baumcanvas.offScreenGraphics;
        g.drawRect(x, y, 2 * Knoten.k * baumcanvas.breite, baumcanvas.höhe);
        g.drawRect(x + 1, y + 1, 2 * Knoten.k * baumcanvas.breite - 2, baumcanvas.höhe - 2);
        zeichneTrennstriche(baumcanvas);
        zeichneSchlüssel(baumcanvas);
        Vector vector = model.alleSöhne();
        Enumeration enumeration = vector.elements();
        for (int i = 0; enumeration.hasMoreElements(); i++) {
            Knoten knoten = (Knoten) enumeration.nextElement();
            g.drawLine(x + i * baumcanvas.breite, y + baumcanvas.höhe + 1,
                    knoten.view.x + Knoten.k * baumcanvas.breite, knoten.view.y - 1);
        }

    }

    private void zeichneSchlüssel(BaumCanvas baumcanvas) {
        Graphics g = baumcanvas.offScreenGraphics;
        for (int i = 0; i < model.schlüssel.size(); i++) {
            String s = Integer.toString(model.schlüssel.elementAnPos(i));
            int j = baumcanvas.fmetrics.stringWidth(s);
            g.drawString(s, x + i * baumcanvas.breite + (baumcanvas.breite - j) / 2, (y + baumcanvas.höhe) - 3);
        }

    }

    private void zeichneTrennstriche(BaumCanvas baumcanvas) {
        Graphics g = baumcanvas.offScreenGraphics;
        for (int i = 1; i < 2 * Knoten.k; i++)
            g.drawLine(x + i * baumcanvas.breite, y + 2, x + i * baumcanvas.breite, (y + baumcanvas.höhe) - 2);

    }

    public void dereferenziere() {
        model = null;
    }

    public int x;
    public int y;
    private Knoten model;
    private static final int ABSTAND = 3;
    private static final int BLINK_TAKT = 80;
    private static final int BLINK_ANZAHL = 2;
}