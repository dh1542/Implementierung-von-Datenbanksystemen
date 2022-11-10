
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Observable;
import java.util.Vector;

public class Knoten extends Observable {

    public Knoten(BBaum b_baum) {
        schl�ssel = new SortierterIntegerVektor();
        zeiger = new Vector();
        view = new KnotenView(this);
        baum = b_baum;
        addObserver(view);
    }

    public Knoten(Knoten knoten, int i, Knoten knoten1) {
        this(knoten1.baum);
        schl�ssel.f�geEin(i);
        zeiger.addElement(knoten);
        zeiger.addElement(knoten1);
    }

    public Vector alleS�hne() {
        return (Vector) zeiger.clone();
    }

    private boolean istBlatt() {
        return zeiger.isEmpty();
    }

    private boolean istWurzel() {
        return vorgE4nger == null;
    }

    private Knoten sucheSchl�ssel(int i) {
        if (schl�ssel.contains(new Integer(i))) return this;
        if (istBlatt()) return null;
        int j;
        for (j = 0; j < schl�ssel.size() && i > schl�ssel.elementAnPos(j); j++);
        return ((Knoten) zeiger.elementAt(j)).sucheSchl�ssel(i);
    }

    private Knoten sucheBlattF�r(int i) {
        if (istBlatt()) if (schl�ssel.contains(new Integer(i)))
            return null;
        else return this;
        int j;
        for (j = 0; j < schl�ssel.size() && i > schl�ssel.elementAnPos(j); j++);
        if (j < schl�ssel.size() && i == schl�ssel.elementAnPos(j))
            return null;
        else return ((Knoten) zeiger.elementAt(j)).sucheBlattF�r(i);
    }

    private Knoten blattMitKleinstemSchl�ssel() {
        if (istBlatt())
            return this;
        else return ((Knoten) zeiger.firstElement()).blattMitKleinstemSchl�ssel();
    }

    private void f�geSchl�sselEin(int i, Knoten knoten) {
        if (schl�ssel.size() == 2 * k) {
            setChanged();
            notifyObservers(baum.baumcanvas);
        }
        int j = schl�ssel.f�geEin(i);
        if (knoten != null) zeiger.insertElementAt(knoten, j);
        if (schl�ssel.size() > 2 * k) {
            Knoten knoten1 = new Knoten(baum);
            int l = schl�ssel.elementAnPos(k);
            schl�ssel.verschiebeElemente(knoten1.schl�ssel, k);
            schl�ssel.removeElementAt(0);
            if (!istBlatt()) {
                for (int i1 = 0; i1 < k + 1; i1++) {
                    Knoten knoten2 = (Knoten) zeiger.firstElement();
                    zeiger.removeElementAt(0);
                    knoten1.zeiger.addElement(knoten2);
                    knoten2.vorgE4nger = knoten1;
                }

            }
            if (vorgE4nger == null) {
                vorgE4nger = new Knoten(knoten1, l, this);
                knoten1.vorgE4nger = vorgE4nger;
                BBaum.wurzel = vorgE4nger;
                return;
            }
            knoten1.vorgE4nger = vorgE4nger;
            vorgE4nger.f�geSchl�sselEin(l, knoten1);
        }
    }

    private void f�geSchl�sselEin(int i) {
        f�geSchl�sselEin(i, null);
    }

    private void entferneSchl�ssel(int i) {
        int j = schl�ssel.indexOf(new Integer(i));
        if (j == -1) throw new NoSuchElementException();
        if (schl�ssel.size() == k && (!istWurzel() || !istBlatt())) {
            setChanged();
            notifyObservers(baum.baumcanvas);
        }
        if (istBlatt()) {
            schl�ssel.removeElementAt(j);
            unterlaufBehandlung();
            return;
        } else {
            Knoten knoten = ((Knoten) zeiger.elementAt(j + 1)).blattMitKleinstemSchl�ssel();
            int l = ((Integer) knoten.schl�ssel.firstElement()).intValue();
            schl�ssel.setElementAt(new Integer(l), j);
            knoten.entferneSchl�ssel(l);
            return;
        }
    }

    private void unterlaufBehandlung() {
        if (!istWurzel() && schl�ssel.size() < k) {
            Knoten knoten = bruderKnoten();
            int i = vorgE4nger.zeiger.indexOf(this);
            int j = vorgE4nger.zeiger.indexOf(knoten);
            int l = Math.min(i, j);
            if (schl�ssel.size() + knoten.schl�ssel.size() >= 2 * k) {
                int i1 = (knoten.schl�ssel.size() - schl�ssel.size() - 1) / 2;
                if (i < j) {
                    schl�ssel.addElement(vorgE4nger.schl�ssel.elementAt(l));
                    knoten.schl�ssel.verschiebeElemente(schl�ssel, i1);
                    if (!istBlatt()) {
                        for (int j1 = 0; j1 <= i1; j1++) {
                            Knoten knoten3 = (Knoten) knoten.zeiger.firstElement();
                            knoten.zeiger.removeElementAt(0);
                            zeiger.addElement(knoten3);
                            knoten3.vorgE4nger = this;
                        }

                    }
                    vorgE4nger.schl�ssel.setElementAt(knoten.schl�ssel.firstElement(), l);
                    knoten.schl�ssel.removeElementAt(0);
                    return;
                }
                schl�ssel.insertElementAt(vorgE4nger.schl�ssel.elementAt(l), 0);
                int k1 = 0;
                int l1 = knoten.schl�ssel.size() - 1;
                for (; k1 < i1; k1++) {
                    schl�ssel.insertElementAt(knoten.schl�ssel.lastElement(), 0);
                    knoten.schl�ssel.removeElementAt(l1 - k1);
                }

                if (!istBlatt()) {
                    int i2 = 0;
                    int j2 = knoten.zeiger.size() - 1;
                    for (; i2 <= i1; i2++) {
                        Knoten knoten4 = (Knoten) knoten.zeiger.lastElement();
                        knoten.zeiger.removeElementAt(j2 - i2);
                        zeiger.insertElementAt(knoten4, 0);
                        knoten4.vorgE4nger = this;
                    }

                }
                vorgE4nger.schl�ssel.setElementAt(knoten.schl�ssel.lastElement(), l);
                knoten.schl�ssel.removeElementAt(knoten.schl�ssel.size() - 1);
                return;
            }
            if (i < j) {
                schl�ssel.addElement(vorgE4nger.schl�ssel.elementAt(l));
                knoten.schl�ssel.verschiebeElemente(schl�ssel, knoten.schl�ssel.size());
                if (!istBlatt()) {
                    for (Enumeration enumeration = knoten.zeiger.elements(); enumeration.hasMoreElements();) {
                        Knoten knoten1 = (Knoten) enumeration.nextElement();
                        zeiger.addElement(knoten1);
                        knoten1.vorgE4nger = this;
                    }

                }
                if (vorgE4nger.schl�ssel.size() == k) {
                    vorgE4nger.setChanged();
                    vorgE4nger.notifyObservers(baum.baumcanvas);
                }
                vorgE4nger.zeiger.removeElementAt(j);
                vorgE4nger.schl�ssel.removeElementAt(l);
                vorgE4nger.unterlaufBehandlung();
                if (vorgE4nger.istWurzel() && vorgE4nger.schl�ssel.isEmpty()) {
                    BBaum.wurzel = this;
                    vorgE4nger = null;
                    return;
                }
            } else {
                knoten.schl�ssel.addElement(vorgE4nger.schl�ssel.elementAt(l));
                schl�ssel.verschiebeElemente(knoten.schl�ssel, schl�ssel.size());
                if (!istBlatt()) {
                    for (Enumeration enumeration1 = zeiger.elements(); enumeration1.hasMoreElements();) {
                        Knoten knoten2 = (Knoten) enumeration1.nextElement();
                        knoten.zeiger.addElement(knoten2);
                        knoten2.vorgE4nger = knoten;
                    }

                }
                if (vorgE4nger.schl�ssel.size() == k) {
                    vorgE4nger.setChanged();
                    vorgE4nger.notifyObservers(baum.baumcanvas);
                }
                vorgE4nger.zeiger.removeElementAt(i);
                vorgE4nger.schl�ssel.removeElementAt(l);
                vorgE4nger.unterlaufBehandlung();
                if (vorgE4nger.istWurzel() && vorgE4nger.schl�ssel.isEmpty()) {
                    BBaum.wurzel = knoten;
                    knoten.vorgE4nger = null;
                }
            }
        }
    }

    private Knoten bruderKnoten() {
        int i = vorgE4nger.zeiger.indexOf(this);
        if (i + 1 == vorgE4nger.zeiger.size())
            i--;
        else i++;
        return (Knoten) vorgE4nger.zeiger.elementAt(i);
    }

    public synchronized boolean insert(int i) {
        Knoten knoten = sucheBlattF�r(i);
        if (knoten == null) {
            return false;
        } else {
            knoten.f�geSchl�sselEin(i);
            return true;
        }
    }

    public synchronized boolean delete(int i) {
        Knoten knoten = sucheSchl�ssel(i);
        if (knoten == null) {
            return false;
        } else {
            knoten.entferneSchl�ssel(i);
            return true;
        }
    }

    public void dereferenziere() {
        view.dereferenziere();
        view = null;
        deleteObservers();
        vorgE4nger = null;
        if (!istBlatt()) {
            Enumeration enumeration = zeiger.elements();
            zeiger = null;
            for (; enumeration.hasMoreElements(); ((Knoten) enumeration.nextElement()).dereferenziere());
        }
    }

    public static int k = 1;
    protected Knoten vorgE4nger;
    protected SortierterIntegerVektor schl�ssel;
    private Vector zeiger;
    public KnotenView view;
    private BBaum baum;

}