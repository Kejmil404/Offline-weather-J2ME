import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/**
 * Ekran glowny z danymi pogodowymi, rysowany recznie zeby dobrze
 * wygladal na ekranie 240x320 (W595). Rysowanie zawsze uzywa wymiarow
 * przekazanych do paintContent(w,h) - dzieki temu dziala tez w trybie
 * "obroconym" z RotatableCanvas.
 *
 * Sterowanie: gora/dol/lewo/prawo (klawisze 2/4/6/8 lub joystick) -
 * przelacza miedzy wpisami czasowymi (kolejne dni/godziny) dla
 * aktualnie wybranego miasta - dziala tez przy przytrzymaniu klawisza
 * (patrz RotatableCanvas.keyRepeated). FIRE (OK/joystick) - otwiera
 * prognoze dzienna. Polecenia w menu telefonu: wybor innego pliku,
 * odswiezenie, "Miasta" (tylko gdy plik zawiera wiecej niz jedno
 * miasto), "Prognoza dzienna", zmiana motywu, obrot ekranu, wyjscie.
 */
public class WeatherCanvas extends RotatableCanvas {

    private final Display display;
    private Vector records = new Vector();
    private String cityName = "";
    private String syncTime = "";
    private int index = 0;
    private String errorMsg = null;
    private boolean citiesEnabled = false;

    public interface Actions {
        void onPickAnotherFile();
        void onRefresh();
        void onExit();
        void onOpenCities();
        void onOpenDaily();
        void onToggleTheme();
    }

    private final Actions actions;

    private final Command cmdPick = new Command("Inny plik", Command.SCREEN, 1);
    private final Command cmdDaily = new Command("Prognoza dzienna", Command.SCREEN, 2);
    private final Command cmdRefresh = new Command("Odswiez", Command.SCREEN, 3);
    private final Command cmdCities = new Command("Miasta", Command.SCREEN, 1);
    private final Command cmdTheme = new Command("Zmien motyw", Command.SCREEN, 5);
    private final Command cmdExit = new Command("Wyjscie", Command.EXIT, 6);

    public WeatherCanvas(Display display, final Actions actions) {
        this.display = display;
        this.actions = actions;
        addCommand(cmdPick);
        addCommand(cmdDaily);
        addCommand(cmdRefresh);
        addCommand(cmdTheme);
        addCommand(cmdRotate);
        addCommand(cmdExit);
        setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                if (c == cmdPick) {
                    actions.onPickAnotherFile();
                } else if (c == cmdRefresh) {
                    actions.onRefresh();
                } else if (c == cmdDaily) {
                    actions.onOpenDaily();
                } else if (c == cmdCities) {
                    actions.onOpenCities();
                } else if (c == cmdTheme) {
                    actions.onToggleTheme();
                } else if (c == cmdRotate) {
                    toggleLandscape();
                } else if (c == cmdExit) {
                    actions.onExit();
                }
            }
        });
    }

    /** Wlacza/wylacza pozycje "Miasta" w menu - tylko gdy w pliku wykryto wiecej niz jedno miasto. */
    public void setCitySwitchingEnabled(boolean enabled) {
        if (enabled == citiesEnabled) {
            return;
        }
        citiesEnabled = enabled;
        if (enabled) {
            addCommand(cmdCities);
        } else {
            removeCommand(cmdCities);
        }
    }

    public void setSyncTime(String syncTime) {
        this.syncTime = syncTime == null ? "" : syncTime;
        repaint();
    }

    public Vector getRecords() {
        return records;
    }

    /**
     * Ustawia liste wpisow dla wybranego miasta. Zamiast zawsze zaczynac
     * od pierwszego wpisu, appka probuje znalezc ten najblizszy "teraz"
     * wg zegara telefonu - tak jak prawdziwa apka pogodowa pokazujaca
     * od razu aktualna/najblizsza prognoze zamiast poczatku pliku.
     */
    public void setRecords(Vector records, String cityName) {
        this.records = records;
        this.cityName = cityName;
        this.index = findNearestToNowIndex(records);
        this.errorMsg = null;
        repaint();
    }

    /**
     * Przeskakuje do wpisu z podana data (np. po wyborze dnia w prognozie
     * dziennej). Jesli to dzisiejsza data, celuje w najblizszy do "teraz"
     * wpis tego dnia (a nie zawsze w pierwszy/00:00) - tak jak przy
     * pierwszym wczytaniu pliku.
     */
    public void jumpToDate(String date) {
        if (date == null) {
            return;
        }
        Integer found = indexForDate(date, true);
        if (found != null) {
            index = found.intValue();
            repaint();
        }
    }

    /**
     * Szuka wpisu dla podanej daty. Jesli preferNearNow=true i to dzisiejsza
     * data, zwraca najblizszy do "teraz" wpis tego dnia (albo ostatni, jesli
     * wszystkie juz minely); w przeciwnym razie zwraca pierwszy wpis danego
     * dnia. Zwraca null, jesli w ogole nie ma wpisow z ta data.
     */
    private Integer indexForDate(String date, boolean preferNearNow) {
        boolean isToday = false;
        try {
            isToday = preferNearNow && date.equals(DateUtil.nowIsoDate());
        } catch (Exception e) {
            isToday = false;
        }

        int firstOfDay = -1;
        int lastOfDay = -1;
        if (isToday) {
            try {
                String nowKey = DateUtil.nowIsoDateTime();
                for (int i = 0; i < records.size(); i++) {
                    WeatherRecord r = (WeatherRecord) records.elementAt(i);
                    if (!date.equals(r.data)) {
                        continue;
                    }
                    if (firstOfDay < 0) {
                        firstOfDay = i;
                    }
                    lastOfDay = i;
                    String key = r.data + " " + (r.godzina == null ? "" : r.godzina);
                    if (key.compareTo(nowKey) >= 0) {
                        return new Integer(i);
                    }
                }
                // wszystkie wpisy dzisiejszego dnia sa juz w przeszlosci -
                // pokazujemy ostatni (najbardziej aktualny) zamiast pierwszego
                if (lastOfDay >= 0) {
                    return new Integer(lastOfDay);
                }
                return null;
            } catch (Exception e) {
                // spadamy do zwyklego wyszukiwania ponizej
            }
        }

        for (int i = 0; i < records.size(); i++) {
            WeatherRecord r = (WeatherRecord) records.elementAt(i);
            if (date.equals(r.data)) {
                return new Integer(i);
            }
        }
        return null;
    }

    /**
     * Szuka pierwszego wpisu, ktorego Data+Godzina >= punktowi odniesienia,
     * i ustawia go jako aktualny (porownanie leksykograficzne dziala
     * poprawnie dla formatu "YYYY-MM-DD"/"HH:MM"). Jesli wszystkie wpisy sa
     * juz przed tym punktem, bierze ostatni; jesli zaden nie ma poprawnej
     * daty/godziny, zaczyna od 0 jak dawniej.
     *
     * Punkt odniesienia to dzisiejsza data (wg zegara telefonu) + godzina
     * ze znacznika synchronizacji, jesli da sie ja odczytac - to lepiej
     * oddaje "jak najświeższe dane, jakie mamy" niz surowa godzina z
     * zegara telefonu, ktora moze byc znacznie pozniejsza niz moment
     * faktycznej synchronizacji pliku. Jesli znacznika brak/nie da sie
     * odczytac, spada do zwyklej godziny z zegara telefonu.
     */
    private int findNearestToNowIndex(Vector recs) {
        if (recs == null || recs.isEmpty()) {
            return 0;
        }
        try {
            String syncHHMM = DateUtil.extractHHMM(syncTime);
            String nowKey = (syncHHMM != null)
                    ? (DateUtil.nowIsoDate() + " " + syncHHMM)
                    : DateUtil.nowIsoDateTime();
            for (int i = 0; i < recs.size(); i++) {
                WeatherRecord r = (WeatherRecord) recs.elementAt(i);
                if (r.data == null || r.data.length() == 0) {
                    continue;
                }
                String key = r.data + " " + (r.godzina == null ? "" : r.godzina);
                if (key.compareTo(nowKey) >= 0) {
                    return i;
                }
            }
            return recs.size() - 1;
        } catch (Exception e) {
            return 0;
        }
    }

    public void setError(String msg) {
        this.errorMsg = msg;
        repaint();
    }

    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        if (action == Canvas.FIRE) {
            if (!records.isEmpty()) {
                actions.onOpenDaily();
            }
            return;
        }
        if (records.isEmpty()) {
            return;
        }
        if (action == Canvas.UP || action == Canvas.LEFT) {
            index--;
            if (index < 0) {
                index = records.size() - 1;
            }
            repaint();
        } else if (action == Canvas.DOWN || action == Canvas.RIGHT) {
            index++;
            if (index >= records.size()) {
                index = 0;
            }
            repaint();
        } else if (keyCode == Canvas.KEY_POUND) {
            // "#" - skok do nastepnego dnia (przydatne przy danych co godzine)
            jumpDay(1);
        } else if (keyCode == Canvas.KEY_STAR) {
            // "*" - skok do poprzedniego dnia
            jumpDay(-1);
        }
    }

    /**
     * Przeskakuje do najblizszego wpisu z innym polem "Data" niz aktualny,
     * w podanym kierunku. Nie zaklada stalej liczby wpisow na dzien (np.
     * co godzine vs co 3 godziny), wiec dziala z dowolna gestoscia danych.
     * Jesli docelowy dzien to dzisiaj, doprecyzowuje pozycje do najblizszej
     * godziny "teraz" tego dnia zamiast zostawiac na pierwszym trafionym
     * wpisie (ktory zazwyczaj bylby o 00:00).
     */
    private void jumpDay(int dir) {
        if (records.size() < 2) {
            return;
        }
        String startData = ((WeatherRecord) records.elementAt(index)).data;
        int i = index;
        for (int step = 0; step < records.size(); step++) {
            i += dir;
            if (i < 0) {
                i = records.size() - 1;
            } else if (i >= records.size()) {
                i = 0;
            }
            WeatherRecord r = (WeatherRecord) records.elementAt(i);
            String d = r.data;
            if (d == null || !d.equals(startData)) {
                Integer refined = indexForDate(d, true);
                index = refined != null ? refined.intValue() : i;
                repaint();
                return;
            }
        }
    }

    protected void paintContent(Graphics g, int w, int h) {
        g.setColor(AppTheme.bg());
        g.fillRect(0, 0, w, h);

        Font fTiny = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        Font fSmallBold = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);

        // -- wiersz 1: nazwa miasta / hint do menu miast --
        g.setColor(AppTheme.textPrimary());
        g.setFont(fSmallBold);
        String header = cityName.length() > 0 ? cityName : "Pogoda offline";
        g.drawString(header, 8, 4, Graphics.TOP | Graphics.LEFT);
        if (citiesEnabled) {
            g.setColor(AppTheme.textMuted());
            g.setFont(fTiny);
            g.drawString("Miasta >", w - 8, 4, Graphics.TOP | Graphics.RIGHT);
        }

        int y = 20;
        if (syncTime.length() > 0) {
            g.setColor(AppTheme.textMuted());
            g.setFont(fTiny);
            // lewo + przycinanie zamiast wyśrodkowania - dlugi tekst
            // wysrodkowany potrafil wystawac poza lewa krawedz ekranu
            // i zostac obciety (np. "Z" z "Zsynchronizowano")
            String syncLine = fitText(fTiny, "Zsynchronizowano: " + syncTime, w - 16);
            g.drawString(syncLine, 8, y, Graphics.TOP | Graphics.LEFT);
            y += 14;
        }

        if (errorMsg != null) {
            g.setColor(AppTheme.errorColor());
            g.setFont(fTiny);
            drawWrapped(g, errorMsg, 8, y + 8, w - 16);
            return;
        }

        if (records.isEmpty()) {
            g.setColor(AppTheme.textSecondary());
            g.setFont(fTiny);
            g.drawString("Brak danych.", 8, y + 8, Graphics.TOP | Graphics.LEFT);
            g.drawString("Wybierz plik pogody.", 8, y + 28, Graphics.TOP | Graphics.LEFT);
            return;
        }

        WeatherRecord rec = (WeatherRecord) records.elementAt(index);

        // -- duza temperatura (lewo) + ikonka obok + opis i min/maks dnia (prawo) --
        int tempRowY = y + 4;
        Font fLarge = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
        g.setColor(tempColor(rec.tempC));
        g.setFont(fLarge);
        String tempStr = rec.tempC.length() > 0 ? rec.tempC + "\u00b0" : "--";
        g.drawString(tempStr, 8, tempRowY, Graphics.TOP | Graphics.LEFT);

        int tempW = fLarge.stringWidth(tempStr);
        drawIcon(g, rec.opis, 8 + tempW + 6, tempRowY + 3, 22);

        int rightColX = w - 8;
        g.setColor(AppTheme.textSecondary());
        g.setFont(fTiny);
        String opisFit = fitText(fTiny, rec.opis, w / 2 - 4);
        g.drawString(opisFit, rightColX, tempRowY, Graphics.TOP | Graphics.RIGHT);

        int[] minMax = computeDayMinMax(rec.data);
        int arrowRowY = tempRowY + 16;
        if (minMax != null) {
            g.setFont(fTiny);
            String maxStr = minMax[1] + "\u00b0";
            String minStr = minMax[0] + "\u00b0";
            int maxW = fTiny.stringWidth(maxStr);
            int gapW = 10;
            int minW = fTiny.stringWidth(minStr);

            int maxX = rightColX - maxW;
            drawArrow(g, maxX - 10, arrowRowY + 2, true, AppTheme.accentRed());
            g.setColor(AppTheme.textPrimary());
            g.drawString(maxStr, maxX, arrowRowY, Graphics.TOP | Graphics.LEFT);

            int minX = maxX - gapW - minW - 12;
            drawArrow(g, minX - 10, arrowRowY + 2, false, AppTheme.accentBlue());
            g.setColor(AppTheme.textSecondary());
            g.drawString(minStr, minX, arrowRowY, Graphics.TOP | Graphics.LEFT);
        }

        if (rec.tempOdczuwalnaC.length() > 0) {
            g.setColor(AppTheme.textMuted());
            g.setFont(fTiny);
            g.drawString("odczuwalna " + rec.tempOdczuwalnaC + "\u00b0", 8, tempRowY + 26, Graphics.TOP | Graphics.LEFT);
        }

        y = tempRowY + 46;

        g.setColor(AppTheme.divider());
        g.drawLine(0, y, w, y);
        y += 4;

        // ktorego dnia dotyczy aktualnie pokazywany wpis - "Dzisiaj" jesli
        // to dzisiejsza data wg zegara telefonu, w przeciwnym razie skrot
        // dnia tygodnia/"Jutro"
        g.setColor(AppTheme.textMuted());
        g.setFont(fTiny);
        g.drawString(DateUtil.dayLabel(rec.data), 8, y, Graphics.TOP | Graphics.LEFT);
        y += 14;

        // -- karta "Prognoza godzinowa": krzywa temperatury + % opadow --
        int graphCardBottom = drawHourlyGraph(g, 8, y, w - 16);
        y = graphCardBottom + 6;

        g.setColor(AppTheme.divider());
        g.drawLine(0, y, w, y);
        y += 8;

        // -- siatka dodatkowych danych, 2 kolumny - tylko pola faktycznie
        // obecne w pliku, wiec dziala tez ze starszym/prostszym formatem --
        Vector labels = new Vector();
        Vector values = new Vector();
        addStat(labels, values, "Wiatr", rec.wiatrKmh, " km/h");
        addStat(labels, values, "Wilg.", rec.wilgotnosc, " %");
        addStat(labels, values, "Cisn.", rec.cisnienieHpa, " hPa");
        addStat(labels, values, "Zachm.", rec.zachmurzenie, " %");
        addStat(labels, values, "Opad", rec.opadMm, " mm");
        addStat(labels, values, "Deszcz", rec.deszczMm, " mm");
        addStat(labels, values, "Snieg", rec.sniegCm, " cm");

        g.setFont(fTiny);
        int statsBottom = h - 44;
        int colGap = 8;
        int colW = (w - 24 - colGap) / 2;
        int rowH = 16;
        int maxRows = (statsBottom - y) / rowH;
        int count = labels.size();
        for (int i = 0; i < count && (i / 2) < maxRows; i++) {
            int col = i % 2;
            int row = i / 2;
            int x = 12 + col * (colW + colGap);
            int yy = y + row * rowH;
            g.setColor(AppTheme.textSecondary());
            String line = (String) labels.elementAt(i) + ": " + (String) values.elementAt(i);
            // przytnij, zeby nigdy nie wjechac na sasiednia kolumne (np. gdy
            // wartosc z pliku bedzie dluzsza niz sie spodziewamy)
            line = fitText(fTiny, line, colW - 2);
            g.drawString(line, x, yy, Graphics.TOP | Graphics.LEFT);
        }

        // wskaznik pozycji na dole ekranu - kropki dla malej liczby wpisow
        // (np. kilka dni), pasek postepu dla duzej (np. tydzien co godzine -
        // 168 wpisow by nie zmiescilo sie jako osobne kropki)
        int dotsY = h - 30;
        int total = records.size();
        int maxWidth = w - 40;

        if (total > 1 && total * 10 <= maxWidth) {
            int dotW = 10;
            int startX = w / 2 - (total * dotW) / 2;
            for (int i = 0; i < total; i++) {
                if (i == index) {
                    g.setColor(AppTheme.accentBlue());
                } else {
                    g.setColor(AppTheme.dotInactive());
                }
                g.fillArc(startX + i * dotW, dotsY, 6, 6, 0, 360);
            }
        } else if (total > 1) {
            int trackX = 20;
            int trackW = w - 40;
            int trackH = 4;
            int trackY = dotsY + 1;
            g.setColor(AppTheme.divider());
            g.fillRect(trackX, trackY, trackW, trackH);
            g.setColor(AppTheme.accentBlue());
            int markerX = trackX + (index * trackW) / (total - 1);
            g.fillRect(markerX - 2, trackY - 3, 4, trackH + 6);
        }

        g.setColor(AppTheme.textMuted());
        g.setFont(fTiny);
        g.drawString((index + 1) + "/" + total, w - 8, h - 20, Graphics.TOP | Graphics.RIGHT);
        g.drawString(total > 20 ? "OK:dzienna */#:dzien" : "OK: prognoza dzienna", 8, h - 20, Graphics.TOP | Graphics.LEFT);
    }

    /**
     * Rysuje kompaktowa karte "Prognoza godzinowa": temperatury, krzywa,
     * podswietlony procent szansy opadow przy szczycie i etykiety godzin -
     * inspirowane wykresem z apki pogodowej, ktora user pokazal jako wzor.
     * Zwraca wspolrzedna Y konca karty.
     */
    private int drawHourlyGraph(Graphics g, int x, int y, int w) {
        Font fTiny = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        Font fTinyBold = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);

        g.setColor(AppTheme.textPrimary());
        g.setFont(fTinyBold);
        g.drawString("Prognoza godzinowa", x, y, Graphics.TOP | Graphics.LEFT);
        y += 16;

        int windowCount = 6;
        if (windowCount > records.size()) {
            windowCount = records.size();
        }
        int start = index;
        if (records.size() - start < windowCount) {
            start = records.size() - windowCount;
            if (start < 0) {
                start = 0;
            }
        }

        int[] temps = new int[windowCount];
        int[] rainProc = new int[windowCount];
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int peakRainIdx = -1;
        int peakRainVal = 0;
        for (int i = 0; i < windowCount; i++) {
            WeatherRecord r = (WeatherRecord) records.elementAt(start + i);
            temps[i] = NumUtil.parseIntOrMin(r.tempC);
            if (temps[i] != Integer.MIN_VALUE) {
                if (temps[i] < min) {
                    min = temps[i];
                }
                if (temps[i] > max) {
                    max = temps[i];
                }
            }
            rainProc[i] = NumUtil.parseIntOrMin(r.szansaOpaduProc);
            if (rainProc[i] != Integer.MIN_VALUE && rainProc[i] > peakRainVal) {
                peakRainVal = rainProc[i];
                peakRainIdx = i;
            }
        }
        if (min == Integer.MAX_VALUE) {
            min = 0;
            max = 0;
        }
        if (min == max) {
            min -= 1;
            max += 1;
        }

        int labelsY = y;
        int curveTop = y + 16;
        int curveBottom = curveTop + 34;
        int hourLabelsY = curveBottom + 6;

        int[] xs = new int[windowCount];
        int[] ys = new int[windowCount];
        for (int i = 0; i < windowCount; i++) {
            xs[i] = (windowCount == 1) ? (x + w / 2) : (x + (i * w) / (windowCount - 1));
            if (temps[i] == Integer.MIN_VALUE) {
                ys[i] = curveBottom;
            } else {
                ys[i] = curveBottom - ((temps[i] - min) * (curveBottom - curveTop)) / (max - min);
            }
        }

        // krzywa
        g.setColor(AppTheme.accentBlue());
        for (int i = 1; i < windowCount; i++) {
            g.drawLine(xs[i - 1], ys[i - 1], xs[i], ys[i]);
        }

        // etykiety temperatur nad kazdym punktem + kropki na krzywej +
        // pionowe kreski/godziny pod krzywa
        for (int i = 0; i < windowCount; i++) {
            boolean selected = (start + i) == index;

            g.setColor(selected ? AppTheme.textPrimary() : AppTheme.textSecondary());
            g.setFont(selected ? fTinyBold : fTiny);
            String t = temps[i] == Integer.MIN_VALUE ? "--" : temps[i] + "\u00b0";
            g.drawString(t, xs[i], labelsY, Graphics.TOP | Graphics.HCENTER);

            g.setColor(selected ? AppTheme.textPrimary() : AppTheme.accentBlue());
            g.fillArc(xs[i] - 3, ys[i] - 3, 6, 6, 0, 360);

            g.setColor(AppTheme.divider());
            g.drawLine(xs[i], curveBottom + 2, xs[i], hourLabelsY - 2);

            g.setColor(AppTheme.textMuted());
            g.setFont(fTiny);
            WeatherRecord r = (WeatherRecord) records.elementAt(start + i);
            String hourLbl = (r.godzina != null && r.godzina.length() > 0)
                    ? (r.godzina.length() > 5 ? r.godzina.substring(0, 5) : r.godzina)
                    : "?";
            g.drawString(hourLbl, xs[i], hourLabelsY, Graphics.TOP | Graphics.HCENTER);
        }

        // procent szansy opadow - jedna etykieta przy szczycie, tak jak w
        // apce ktora user pokazal jako inspiracje
        if (peakRainIdx >= 0) {
            g.setColor(0x4D8FE0);
            g.setFont(fTiny);
            g.drawString(peakRainVal + "%", xs[peakRainIdx], ys[peakRainIdx] + 6, Graphics.TOP | Graphics.HCENTER);
        }

        return hourLabelsY + fTiny.getHeight();
    }

    /** Rysuje mala trojkatna strzalke (gora/dol) - do wskaznika min/maks dnia. */
    private void drawArrow(Graphics g, int x, int y, boolean up, int color) {
        g.setColor(color);
        if (up) {
            g.fillTriangle(x, y + 5, x + 8, y + 5, x + 4, y - 2);
        } else {
            g.fillTriangle(x, y - 2, x + 8, y - 2, x + 4, y + 5);
        }
    }

    /**
     * Liczy min/maks temperature (z pola TempC) dla wszystkich wpisow
     * biezacego miasta majacych to samo pole Data co podane. Zwraca null,
     * jesli brak jakichkolwiek poprawnych wartosci.
     */
    private int[] computeDayMinMax(String date) {
        if (date == null) {
            return null;
        }
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < records.size(); i++) {
            WeatherRecord r = (WeatherRecord) records.elementAt(i);
            if (r.data == null || !r.data.equals(date)) {
                continue;
            }
            int t = NumUtil.parseIntOrMin(r.tempC);
            if (t == Integer.MIN_VALUE) {
                continue;
            }
            if (t < min) {
                min = t;
            }
            if (t > max) {
                max = t;
            }
        }
        if (min == Integer.MAX_VALUE) {
            return null;
        }
        return new int[]{min, max};
    }

    private void addStat(Vector labels, Vector values, String label, String raw, String unit) {
        if (raw != null && raw.length() > 0) {
            labels.addElement(label);
            values.addElement(raw + unit);
        }
    }

    /** Przycina tekst tak, zeby zmiescil sie w maxW pikseli (z "..", jesli obciety). */
    private String fitText(Font f, String text, int maxW) {
        if (f.stringWidth(text) <= maxW) {
            return text;
        }
        String cut = text;
        while (cut.length() > 1 && f.stringWidth(cut + "..") > maxW) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "..";
    }

    /** Rysuje prosta ikonke pogody na podstawie slow kluczowych w opisie - obok temperatury. */
    private void drawIcon(Graphics g, String opis, int x, int y, int size) {
        String o = opis == null ? "" : opis.toLowerCase();
        boolean rain = o.indexOf("deszcz") >= 0 || o.indexOf("opad") >= 0;
        boolean cloud = o.indexOf("chmur") >= 0 || o.indexOf("pochmur") >= 0 || rain;
        boolean sun = o.indexOf("slon") >= 0 || o.indexOf("słon") >= 0 || !cloud;
        boolean snow = o.indexOf("snieg") >= 0 || o.indexOf("śnieg") >= 0;

        int cx = x + size / 2;
        int cy = y + size / 2;

        if (sun && !cloud) {
            g.setColor(0xF5A623);
            g.fillArc(x + size / 4, y + size / 4, size / 2, size / 2, 0, 360);
            // CLDC nie ma Math.sin/cos/toRadians, wiec 8 kierunkow (co 45
            // stopni) mamy zaszyte na sztywno jako wspolczynniki jednostkowe
            double[] dirX = {1.0, 0.7071, 0.0, -0.7071, -1.0, -0.7071, 0.0, 0.7071};
            double[] dirY = {0.0, 0.7071, 1.0, 0.7071, 0.0, -0.7071, -1.0, -0.7071};
            for (int i = 0; i < 8; i++) {
                int x1 = cx + (int) (dirX[i] * (size / 2));
                int y1 = cy + (int) (dirY[i] * (size / 2));
                int x2 = cx + (int) (dirX[i] * (size / 2 + 4));
                int y2 = cy + (int) (dirY[i] * (size / 2 + 4));
                g.drawLine(x1, y1, x2, y2);
            }
        }

        if (cloud) {
            g.setColor(AppTheme.dark ? 0xAAAAAA : 0x999999);
            g.fillArc(x, y + size / 3, size / 2, size / 2, 0, 360);
            g.fillArc(x + size / 3, y + size / 4, size / 2, size / 2, 0, 360);
            g.fillRect(x + size / 6, y + size / 2, (int) (size * 0.7), size / 3);
        }

        if (rain) {
            g.setColor(0x3399FF);
            for (int i = 0; i < 3; i++) {
                int rx = x + 4 + i * (size / 4);
                g.drawLine(rx, y + size, rx - 2, y + size + 6);
            }
        }

        if (snow) {
            g.setColor(0x99CCFF);
            for (int i = 0; i < 3; i++) {
                int sx = x + 4 + i * (size / 4);
                g.drawString("*", sx, y + size, Graphics.TOP | Graphics.LEFT);
            }
        }
    }

    /** Kolor temperatury: niebieski na zimno, czerwony na goraco, kolor tekstu motywu neutralnie (zeby nie zniknal na tle). */
    private int tempColor(String tempC) {
        try {
            int dot = tempC.indexOf('.');
            String intPart = dot >= 0 ? tempC.substring(0, dot) : tempC;
            int t = Integer.parseInt(intPart.trim());
            if (t <= 0) {
                return AppTheme.accentBlue();
            } else if (t >= 25) {
                return AppTheme.accentRed();
            } else {
                return AppTheme.textPrimary();
            }
        } catch (Exception e) {
            return AppTheme.textPrimary();
        }
    }

    private void drawWrapped(Graphics g, String text, int x, int y, int maxWidth) {
        Font f = g.getFont();
        int lineHeight = f.getHeight() + 2;
        StringBuffer line = new StringBuffer();
        int i = 0;
        while (i < text.length()) {
            char ch = text.charAt(i);
            line.append(ch);
            if (f.stringWidth(line.toString()) > maxWidth) {
                line.deleteCharAt(line.length() - 1);
                g.drawString(line.toString(), x, y, Graphics.TOP | Graphics.LEFT);
                y += lineHeight;
                line = new StringBuffer();
            } else {
                i++;
            }
        }
        if (line.length() > 0) {
            g.drawString(line.toString(), x, y, Graphics.TOP | Graphics.LEFT);
        }
    }
}
