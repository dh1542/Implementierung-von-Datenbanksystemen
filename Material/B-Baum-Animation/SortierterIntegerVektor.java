
import java.util.Vector;

public class SortierterIntegerVektor extends Vector {

    public synchronized int fügeEin(int i) {
        int j;
        for (j = 0; j < size() && i > elementAnPos(j); j++);
        insertElementAt(new Integer(i), j);
        return j;
    }

    public int elementAnPos(int i) {
        return ((Integer) elementAt(i)).intValue();
    }

    public synchronized void verschiebeElemente(Vector vector, int i) {
        for (int j = 0; j < i; j++) {
            vector.addElement(firstElement());
            removeElementAt(0);
        }

    }

}