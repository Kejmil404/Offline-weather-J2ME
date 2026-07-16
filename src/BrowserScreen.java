import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

/**
 * Bardzo prosta przegladarka systemu plikow telefonu (wymaga JSR-75,
 * ktore Sony Ericsson W595 obsluguje). Pozwala dojsc do pliku .txt
 * przeslanego przez Bluetooth (zwykle telefon zapisuje odebrane pliki
 * w katalogu typu "other/" na karcie pamieci lub w pamieci telefonu)
 * i go wybrac.
 */
public class BrowserScreen implements CommandListener {

    private final Display display;
    private final Displayable previous;
    private final FileSelectedListener listener;

    private final Command cmdSelect = new Command("Wybierz", Command.OK, 1);
    private final Command cmdUp = new Command("Wyzej", Command.SCREEN, 2);
    private final Command cmdBack = new Command("Wroc", Command.BACK, 3);

    // sciezka biezacego katalogu, np. "file:///E:/other/" ; null = lista dyskow
    private String currentPath = null;
    private List list;

    public BrowserScreen(Display display, Displayable previous, FileSelectedListener listener) {
        this.display = display;
        this.previous = previous;
        this.listener = listener;
    }

    public void show() {
        showRoots();
    }

    private void showRoots() {
        currentPath = null;
        list = new List("Wybierz dysk", List.IMPLICIT);
        Enumeration roots = FileSystemRegistry.listRoots();
        while (roots.hasMoreElements()) {
            String root = (String) roots.nextElement();
            list.append(root, null);
        }
        list.addCommand(cmdBack);
        list.setCommandListener(this);
        display.setCurrent(list);
    }

    private void showDirectory(String path) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(path, Connector.READ);
            if (!fc.exists() || !fc.isDirectory()) {
                showError("To nie jest katalog");
                return;
            }
            Vector dirs = new Vector();
            Vector files = new Vector();
            Enumeration entries = fc.list();
            while (entries.hasMoreElements()) {
                String name = (String) entries.nextElement();
                if (name.endsWith("/")) {
                    dirs.addElement(name);
                } else {
                    files.addElement(name);
                }
            }
            currentPath = path;
            list = new List(shortLabel(path), List.IMPLICIT);
            for (int i = 0; i < dirs.size(); i++) {
                list.append("[" + dirs.elementAt(i) + "]", null);
            }
            for (int i = 0; i < files.size(); i++) {
                list.append((String) files.elementAt(i), null);
            }
            list.addCommand(cmdSelect);
            list.addCommand(cmdUp);
            list.addCommand(cmdBack);
            list.setCommandListener(this);
            display.setCurrent(list);
        } catch (IOException e) {
            showError("Blad odczytu: " + e.getMessage());
        } finally {
            if (fc != null) {
                try {
                    fc.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private String shortLabel(String path) {
        // skraca "file:///E:/other/" do "E:/other/" dla czytelnosci na malym ekranie
        if (path.startsWith("file://")) {
            return path.substring(7);
        }
        return path;
    }

    private void goUp() {
        if (currentPath == null) {
            return;
        }
        String p = currentPath;
        if (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        int idx = p.lastIndexOf('/');
        if (idx < 0) {
            showRoots();
            return;
        }
        String parent = p.substring(0, idx + 1);
        if (parent.equals("file://") || parent.length() <= 8) {
            showRoots();
        } else {
            showDirectory(parent);
        }
    }

    private void showError(String msg) {
        Alert a = new Alert("Blad", msg, null, AlertType.ERROR);
        a.setTimeout(2500);
        display.setCurrent(a, list != null ? list : previous);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == cmdBack) {
            if (currentPath == null) {
                display.setCurrent(previous);
            } else {
                goUp();
            }
            return;
        }
        if (c == cmdUp) {
            goUp();
            return;
        }

        int sel = list.getSelectedIndex();
        if (sel < 0) {
            return;
        }
        String label = list.getString(sel);

        if (currentPath == null) {
            // wybrano dysk z listy root
            String root = label;
            showDirectory("file:///" + root);
            return;
        }

        if (label.startsWith("[") && label.endsWith("]")) {
            String dirName = label.substring(1, label.length() - 1);
            showDirectory(currentPath + dirName);
            return;
        }

        // wybrano plik
        if (c == cmdSelect || c == List.SELECT_COMMAND) {
            String fileUrl = currentPath + label;
            listener.onFileSelected(fileUrl);
        }
    }
}
