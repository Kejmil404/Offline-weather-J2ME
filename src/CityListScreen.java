import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

/**
 * Prosta lista miast wykrytych w aktualnie wczytanym pliku. Otwierana
 * z menu kontekstowego (Opcje -> Miasta) na glownym ekranie pogody.
 */
public class CityListScreen implements CommandListener {

    private final Display display;
    private final Displayable previous;
    private final CitySelectedListener listener;
    private final List list;

    private final Command cmdBack = new Command("Wstecz", Command.BACK, 1);

    public CityListScreen(Display display, Displayable previous, Vector cityNames, CitySelectedListener listener) {
        this.display = display;
        this.previous = previous;
        this.listener = listener;

        list = new List("Wybierz miasto", List.IMPLICIT);
        for (int i = 0; i < cityNames.size(); i++) {
            String name = (String) cityNames.elementAt(i);
            list.append(name.length() > 0 ? name : "(bez nazwy)", null);
        }
        list.addCommand(cmdBack);
        list.setCommandListener(this);
    }

    public void show() {
        display.setCurrent(list);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == cmdBack) {
            display.setCurrent(previous);
            return;
        }
        int idx = list.getSelectedIndex();
        if (idx >= 0) {
            listener.onCitySelected(idx);
        }
    }
}
