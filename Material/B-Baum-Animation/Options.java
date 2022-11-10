
import java.awt.Event;
import java.awt.Panel;

public class Options extends Panel {

    public Options(BBaum b_baum) {
        parent = b_baum;
    }

    public boolean action(Event event, Object obj) {
        return parent.action(event, obj);
    }

    private BBaum parent;
}