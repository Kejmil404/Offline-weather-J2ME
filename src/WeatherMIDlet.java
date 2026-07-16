import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStore;

/**
 * Offline czytnik pogody dla Sony Ericsson W595 (240x320, MIDP 2.0,
 * CLDC 1.1, JSR-75 FileConnection). Dane pogodowe wczytywane sa z pliku
 * tekstowego, ktory user przesyla na telefon przez Bluetooth (OBEX -
 * telefon sam zapisuje odebrany plik, zwykle do "Inne/Other" na karcie
 * pamieci albo w pamieci telefonu).
 *
 * Plik moze zawierac dane dla wielu miast (grupowanie po polu Miasto) -
 * przelaczanie miedzy nimi jest w menu "Miasta" na glownym ekranie.
 *
 * Sciezka do ostatnio wybranego pliku i wybor motywu (jasny/ciemny) sa
 * zapamietywane w RecordStore, wiec przy kolejnym uruchomieniu appka od
 * razu proboje wczytac plik i pamieta ostatni wybrany motyw.
 */
public class WeatherMIDlet extends MIDlet implements FileSelectedListener, CitySelectedListener, DaySelectedListener, WeatherCanvas.Actions {

    private static final String RS_FILE = "weathercfg";
    private static final String RS_THEME = "weathertheme";

    private Display display;
    private WeatherCanvas canvas;
    private DailyForecastCanvas dailyCanvas;
    private String currentFileUrl = null;

    private Vector cityGroups = new Vector(); // CityGroup
    private int currentCityIndex = 0;

    protected void startApp() {
        if (display == null) {
            display = Display.getDisplay(this);
            loadThemePref();
            canvas = new WeatherCanvas(display, this);
            display.setCurrent(canvas);

            String saved = loadSavedPath();
            if (saved != null) {
                currentFileUrl = saved;
                loadFile(saved);
            } else {
                onPickAnotherFile();
            }
        }
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) {
    }

    // ---- WeatherCanvas.Actions ----

    public void onPickAnotherFile() {
        BrowserScreen browser = new BrowserScreen(display, canvas, this);
        browser.show();
    }

    public void onRefresh() {
        if (currentFileUrl != null) {
            loadFile(currentFileUrl);
        } else {
            onPickAnotherFile();
        }
    }

    public void onOpenCities() {
        if (cityGroups.size() <= 1) {
            return;
        }
        Vector names = new Vector();
        for (int i = 0; i < cityGroups.size(); i++) {
            CityGroup cg = (CityGroup) cityGroups.elementAt(i);
            names.addElement(cg.miasto);
        }
        CityListScreen screen = new CityListScreen(display, canvas, names, this);
        screen.show();
    }

    public void onOpenDaily() {
        if (cityGroups.isEmpty()) {
            return;
        }
        CityGroup cg = (CityGroup) cityGroups.elementAt(currentCityIndex);
        Vector days = WeatherParser.groupByDay(cg.records);
        if (dailyCanvas == null) {
            dailyCanvas = new DailyForecastCanvas(display, canvas, this);
        }
        dailyCanvas.setDays(days);
        display.setCurrent(dailyCanvas);
    }

    public void onToggleTheme() {
        AppTheme.toggle();
        saveThemePref();
        Displayable current = display.getCurrent();
        if (current instanceof Canvas) {
            ((Canvas) current).repaint();
        }
    }

    public void onExit() {
        destroyApp(true);
        notifyDestroyed();
    }

    // ---- CitySelectedListener ----

    public void onCitySelected(int cityIndex) {
        if (cityIndex < 0 || cityIndex >= cityGroups.size()) {
            return;
        }
        currentCityIndex = cityIndex;
        CityGroup cg = (CityGroup) cityGroups.elementAt(cityIndex);
        canvas.setRecords(cg.records, cg.miasto.length() > 0 ? cg.miasto : "(bez nazwy)");
        display.setCurrent(canvas);
    }

    // ---- DaySelectedListener ----

    public void onDaySelected(String isoDate) {
        canvas.jumpToDate(isoDate);
        display.setCurrent(canvas);
    }

    // ---- FileSelectedListener ----

    public void onFileSelected(String fileUrl) {
        currentFileUrl = fileUrl;
        savePath(fileUrl);
        display.setCurrent(canvas);
        loadFile(fileUrl);
    }

    // ---- logika wczytywania ----

    private void loadFile(String fileUrl) {
        FileConnection fc = null;
        InputStream is = null;
        try {
            fc = (FileConnection) Connector.open(fileUrl, Connector.READ);
            if (!fc.exists()) {
                canvas.setError("Plik nie istnieje:\n" + fileUrl);
                return;
            }
            is = fc.openInputStream();
            ParsedFile parsed = WeatherParser.parse(is);
            if (parsed.records.isEmpty()) {
                canvas.setError("Plik nie zawiera poprawnych\ndanych pogodowych.");
                cityGroups = new Vector();
                canvas.setCitySwitchingEnabled(false);
                return;
            }
            cityGroups = WeatherParser.groupByCity(parsed.records);
            currentCityIndex = 0;
            canvas.setCitySwitchingEnabled(cityGroups.size() > 1);
            canvas.setSyncTime(parsed.syncTime);

            CityGroup first = (CityGroup) cityGroups.elementAt(0);
            canvas.setRecords(first.records, first.miasto.length() > 0 ? first.miasto : "(bez nazwy)");
        } catch (SecurityException se) {
            canvas.setError("Brak uprawnien do odczytu pliku.\nSprawdz zgody aplikacji w\nustawieniach telefonu.");
        } catch (IOException e) {
            canvas.setError("Nie mozna wczytac pliku:\n" + e.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
            if (fc != null) {
                try {
                    fc.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    // ---- zapamietywanie ostatniej sciezki i motywu w RecordStore ----

    private void savePath(String path) {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(RS_FILE, true);
            byte[] data = path.getBytes("UTF-8");
            if (rs.getNumRecords() == 0) {
                rs.addRecord(data, 0, data.length);
            } else {
                rs.setRecord(1, data, 0, data.length);
            }
        } catch (Exception ignored) {
            // brak mozliwosci zapisu nie jest krytyczny - po prostu za kazdym
            // razem trzeba bedzie wybrac plik recznie
        } finally {
            closeQuietly(rs);
        }
    }

    private String loadSavedPath() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(RS_FILE, false);
            if (rs.getNumRecords() == 0) {
                return null;
            }
            byte[] data = rs.getRecord(1);
            return new String(data, "UTF-8");
        } catch (Exception e) {
            return null;
        } finally {
            closeQuietly(rs);
        }
    }

    private void saveThemePref() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(RS_THEME, true);
            byte[] data = {(byte) (AppTheme.dark ? 1 : 0)};
            if (rs.getNumRecords() == 0) {
                rs.addRecord(data, 0, data.length);
            } else {
                rs.setRecord(1, data, 0, data.length);
            }
        } catch (Exception ignored) {
            // niekrytyczne - po prostu przy kolejnym starcie wroci domyslny motyw
        } finally {
            closeQuietly(rs);
        }
    }

    private void loadThemePref() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(RS_THEME, false);
            if (rs.getNumRecords() == 0) {
                return;
            }
            byte[] data = rs.getRecord(1);
            if (data != null && data.length > 0) {
                AppTheme.dark = data[0] != 0;
            }
        } catch (Exception e) {
            // brak zapisanej preferencji - zostaje domyslny (ciemny) motyw
        } finally {
            closeQuietly(rs);
        }
    }

    private void closeQuietly(RecordStore rs) {
        if (rs != null) {
            try {
                rs.closeRecordStore();
            } catch (Exception ignored) {
            }
        }
    }
}

