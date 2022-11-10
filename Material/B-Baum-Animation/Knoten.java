
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Observable;
import java.util.Vector;

public class Knoten extends Observable {

    public Knoten(BBaum b_baum) {
        schlüssel = new SortierterIntegerVektor();
        zeiger = new Vector();
        view = new KnotenView(this);
        baum = b_baum;
        addObserver(view);
    }

    public Knoten(Knoten knoten, int i, Knoten knoten1) {
        this(knoten1.baum);
        schlüssel.fügeEin(i);
        zeiger.addElement(knoten);
        zeiger.addElement(knoten1);
    }

    public Vector alleSöhne() {
        return (Vector) zeiger.clone();
    }

    private boolean istBlatt() {
        return zeiger.isEmpty();
    }

    private boolean istWurzel() {
        return vorgE4nger == null;
    }

    private Knoten sucheSchlüssel(int i) {
        if (schlüssel.contains(new Integer(i))) return this;
        if (istBlatt()) return null;
        int j;
        for (j = 0; j < schlüssel.size() && i > schlüssel.elementAnPos(j); j++);
        return ((Knoten) zeiger.elementAt(j)).sucheSchlüssel(i);
    }

    private Knoten sucheBlattFür(int i) {
        if (istBlatt()) if (schlüssel.contains(new Integer(i)))
            return null;
        else return this;
        int j;
        for (j = 0; j < schlüssel.size() && i > schlüssel.elementAnPos(j); j++);
        if (j < schlüssel.size() && i == schlüssel.elementAnPos(j))
            return null;
        else return ((Knoten) zeiger.elementAt(j)).sucheBlattFür(i);
    }

    private Knoten blattMitKleinstemSchlüssel() {
        if (istBlatt())
            return this;
        else return ((Knoten) zeiger.firstElement()).blattMitKleinstemSchlüssel();
    }

    private void fügeSchlüsselEin(int i, Knoten knoten) {
        if (schlüssel.size() == 2 * k) {
            setChanged();
            notifyObservers(baum.baumcanvas);
        }
        int j = schlüssel.fügeEin(i);
        if (knoten != null) zeiger.insertElementAt(knoten, j);
        if (schlüssel.size() > 2 * k) {
            Knoten knoten1 = new Knoten(baum);
            int l = schlüssel.elementAnPos(k);
            schlüssel.verschiebeElemente(knoten1.schlüssel, k);
            schlüssel.removeElementAt(0);
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
            vorgE4nger.fügeSchlüsselEin(l, knoten1);
        }
    }

    private void fügeSchlüsselEin(int i) {
        fügeSchlüsselEin(i, null);
    }

    private void entferneSchlüssel(int i) {
        int j = schlüssel.indexOf(new Integer(i));
        if (j == -1) throw new NoSuchElementException();
        if (schlüssel.size() == k && (!istWurzel() || !istBlatt())) {
            setChanged();
            notifyObservers(baum.baumcanvas);
        }
        if (istBlatt()) {
            schlüssel.removeElementAt(j);
            unterlaufBehandlung();
            return;
        } else {
            Knoten knoten = ((Knoten) zeiger.elementAt(j + 1)).blattMitKleinstemSchlüssel();
            int l = ((Integer) knoten.schlüssel.firstElement()).intValue();
            schlüssel.setElementAt(new Integer(l), j);
            knoten.entferneSchlüssel(l);
            return;
        }
    }

    private void unterlaufBehandlung() {
        if (!istWurzel() && schlüssel.size() < k) {
            Knoten knoten = bruderKnoten();
            int i = vorgE4nger.zeiger.indexOf(this);
            int j = vorgE4nger.zeiger.indexOf(knoten);
            int l = Math.min(i, j);
            if (schlüssel.size() + knoten.schlüssel.size() >= 2 * k) {
                int i1 = (knoten.schlüssel.size() - schlüssel.size() - 1) / 2;
                if (i < j) {
                    schlüssel.addElement(vorgE4nger.schlüssel.elementAt(l));
                    knoten.schlüssel.verschiebeElemente(schlüssel, i1);
                    if (!istBlatt()) {
                        for (int j1 = 0; j1 <= i1; j1++) {
                            Knoten knoten3 = (Knoten) knoten.zeiger.firstElement();
                            knoten.zeiger.removeElementAt(0);
                            zeiger.addElement(knoten3);
                            knoten3.vorgE4nger = this;
                        }

                    }
                    vorgE4nger.schlüssel.setElementAt(knoten.schlüssel.firstElement(), l);
                    knoten.schlüssel.removeElementAt(0);
                    return;
                }
                schlüssel.insertElementAt(vorgE4nger.schlüssel.elementAt(l), 0);
                int k1 = 0;
                int l1 = knoten.schlüssel.size() - 1;
                for (; k1 < i1; k1++) {
                    schlüssel.insertElementAt(knoten.schlüssel.lastElement(), 0);
                    knoten.schlüssel.removeElementAt(l1 - k1);
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
                vorgE4nger.schlüssel.setElementAt(knoten.schlüssel.lastElement(), l);
                knoten.schlüssel.removeElementAt(knoten.schlüssel.size() - 1);
                return;
            }
            if (i < j) {
                schlüssel.addElement(vorgE4nger.schlüssel.elementAt(l));
                knoten.schlüssel.verschiebeElemente(schlüssel, knoten.schlüssel.size());
                if (!istBlatt()) {
                    for (Enumeration enumeration = knoten.zeiger.elements(); enumeration.hasMoreElements();) {
                        Knoten knoten1 = (Knoten) enumeration.nextElement();
                        zeiger.addElement(knoten1);
                        knoten1.vorgE4nger = this;
                    }

                }
                if (vorgE4nger.schlüssel.size() == k) {
                    vorgE4nger.setChanged();
                    vorgE4nger.notifyObservers(baum.baumcanvas);
                }
                vorgE4nger.zeiger.removeElementAt(j);
                vorgE4nger.schlüssel.removeElementAt(l);
                vorgE4nger.unterlaufBehandlung();
                if (vorgE4nger.istWurzel() && vorgE4nger.schlüssel.isEmpty()) {
                    BBaum.wurzel = this;
                    vorgE4nger = null;
                    return;
                }
            } else {
                knoten.schlüssel.addElement(vorgE4nger.schlüssel.elementAt(l));
                schlüssel.verschiebeElemente(knoten.schlüssel, schlüssel.size());
                if (!istBlatt()) {
                    for (Enumeration enumeration1 = zeiger.elements(); enumeration1.hasMoreElements();) {
                        Knoten knoten2 = (Knoten) enumeration1.nextElement();
                        knoten.zeiger.addElement(knoten2);
                        knoten2.vorgE4nger = knoten;
                    }

                }
                if (vorgE4nger.schlüssel.size() == k) {
                    vorgE4nger.setChanged();
                    vorgE4nger.notifyObservers(baum.baumcanvas);
                }
                vorgE4nger.zeiger.removeElementAt(i);
                vorgE4nger.schlüssel.removeElementAt(l);
                vorgE4nger.unterlaufBehandlung();
                if (vorgE4nger.istWurzel() && vorgE4nger.schlüssel.isEmpty()) {
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
        Knoten knoten = sucheBlattFür(i);
        if (knoten == null) {
            return false;
        } else {
            knoten.fügeSchlüsselEin(i);
            return true;
        }
    }

    public synchronized boolean delete(int i) {
        Knoten knoten = sucheSchlüssel(i);
        if (knoten == null) {
            return false;
        } else {
            knoten.entferneSchlüssel(i);
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
    protected SortierterIntegerVektor schlüssel;
    private Vector zeiger;
    public KnotenView view;
    private BBaum baum;

}