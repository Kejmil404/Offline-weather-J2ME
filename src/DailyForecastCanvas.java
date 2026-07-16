import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/**
 * Lista dni z zakresem temperatur na wspolnej skali (jak "Prognoza
 * dzienna" w apce, ktora user pokazal jako inspiracje) - kazdy wiersz:
 * dzien, procent szans opadow, pasek min-maks na tle wspolnego zakresu
 * wszystkich dni, wartosci brzegowe.
 *
 * Przewijanie: gora/dol (dziala tez przy przytrzymaniu - patrz
 * RotatableCanvas.keyRepeated). OK/FIRE na wybranym dniu wraca do
 * ekranu glownego i przeskakuje tam do pierwszego wpisu godzinowego
 * tego dnia.
 */
public class DailyForecastCanvas extends RotatableCanvas implements CommandListener {

    private final Display display;
    private final Displayable previous;
    private final DaySelectedListener listener;

    private Vector days = new Vector(); // DaySummary
    private int selected = 0;
    private int topRow = 0;

    private static final int ROW_H = 38;
    private static final int LIST_TOP = 30;

    private final Command cmdBack = new Command("Wstecz", Command.BACK, 1);
    private final Command cmdSelect = new Command("Godzinowo tego dnia", Command.OK, 1);
    private final Command cmdTheme = new Command("Zmien motyw", Command.SCREEN, 5);

    public DailyForecastCanvas(Display display, Displayable previous, DaySelectedListener listener) {
        this.display = display;
        this.previous = previous;
        this.listener = listener;
        addCommand(cmdBack);
        addCommand(cmdSelect);
        addCommand(cmdTheme);
        addCommand(cmdRotate);
        setCommandListener(this);
    }

    public void setDays(Vector days) {
        this.days = days;
        this.selected = 0;
        this.topRow = 0;
        repaint();
    }

    private void selectCurrentDay() {
        if (days.isEmpty()) {
            return;
        }
        DaySummary ds = (DaySummary) days.elementAt(selected);
        if (listener != null) {
            listener.onDaySelected(ds.date);
        }
    }

    protected void keyPressed(int keyCode) {
        if (days.isEmpty()) {
            return;
        }
        int action = getGameAction(keyCode);
        if (action == Canvas.UP) {
            selected--;
            if (selected < 0) {
                selected = days.size() - 1;
            }
            ensureVisible();
            repaint();
        } else if (action == Canvas.DOWN) {
            selected++;
            if (selected >= days.size()) {
                selected = 0;
            }
            ensureVisible();
            repaint();
        } else if (action == Canvas.FIRE) {
            // dublujemy z cmdSelect ponizej - na niektorych telefonach (gdy
            // Canvas ma tylko jedna "zwykla" komende obok Wstecz) srodkowy
            // klawisz bywa podpinany bezposrednio pod ta komende zamiast
            // dostarczac zdarzenie FIRE, wiec cmdSelect (typu Command.OK)
            // jest tu glownym, pewnym mechanizmem, a to tylko zapasowy
            selectCurrentDay();
        }
    }

    private int visibleRows() {
        int h = getHeight();
        int n = (h - LIST_TOP - 20) / ROW_H;
        return n < 1 ? 1 : n;
    }

    private void ensureVisible() {
        int rows = visibleRows();
        if (selected < topRow) {
            topRow = selected;
        } else if (selected >= topRow + rows) {
            topRow = selected - rows + 1;
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == cmdBack) {
            display.setCurrent(previous);
        } else if (c == cmdSelect) {
            selectCurrentDay();
        } else if (c == cmdTheme) {
            AppTheme.toggle();
            repaint();
        } else if (c == cmdRotate) {
            toggleLandscape();
        }
    }

    protected void paintContent(Graphics g, int w, int h) {
        g.setColor(AppTheme.bg());
        g.fillRect(0, 0, w, h);

        Font fBold = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        Font fTiny = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);

        g.setColor(AppTheme.textPrimary());
        g.setFont(fBold);
        g.drawString("Prognoza dzienna", 8, 6, Graphics.TOP | Graphics.LEFT);

        if (days.isEmpty()) {
            g.setColor(AppTheme.textSecondary());
            g.setFont(fTiny);
            g.drawString("Brak danych.", 8, LIST_TOP, Graphics.TOP | Graphics.LEFT);
            return;
        }

        int globalMin = Integer.MAX_VALUE;
        int globalMax = Integer.MIN_VALUE;
        for (int i = 0; i < days.size(); i++) {
            DaySummary ds = (DaySummary) days.elementAt(i);
            if (ds.minTemp != Integer.MAX_VALUE && ds.minTemp < globalMin) {
                globalMin = ds.minTemp;
            }
            if (ds.maxTemp != Integer.MIN_VALUE && ds.maxTemp > globalMax) {
                globalMax = ds.maxTemp;
            }
        }
        if (globalMin == Integer.MAX_VALUE) {
            globalMin = 0;
            globalMax = 0;
        }
        if (globalMin == globalMax) {
            globalMin -= 1;
            globalMax += 1;
        }

        int rows = visibleRows();
        int barX0 = 116;
        int barX1 = w - 44;

        for (int i = 0; i < rows && (topRow + i) < days.size(); i++) {
            int idx = topRow + i;
            DaySummary ds = (DaySummary) days.elementAt(idx);
            int y = LIST_TOP + i * ROW_H;
            boolean sel = idx == selected;

            if (sel) {
                g.setColor(AppTheme.dark ? 0x151515 : 0xF0F0F0);
                g.fillRect(0, y - 2, w, ROW_H - 4);
            }

            g.setColor(AppTheme.textPrimary());
            g.setFont(fBold);
            g.drawString(DateUtil.dayLabel(ds.date), 8, y + 4, Graphics.TOP | Graphics.LEFT);

            g.setFont(fTiny);
            if (ds.rainProc >= 0) {
                g.setColor(AppTheme.accentBlue());
                g.drawString(ds.rainProc + "%", 8, y + 20, Graphics.TOP | Graphics.LEFT);
            }

            boolean hasRange = ds.minTemp != Integer.MAX_VALUE;
            String minStr = hasRange ? ds.minTemp + "\u00b0" : "--";
            String maxStr = hasRange ? ds.maxTemp + "\u00b0" : "--";

            g.setColor(AppTheme.textSecondary());
            g.drawString(minStr, barX0 - 6, y + 10, Graphics.TOP | Graphics.RIGHT);

            g.setColor(AppTheme.dotInactive());
            int trackY = y + 14;
            g.fillRect(barX0, trackY, barX1 - barX0, 3);

            if (hasRange) {
                int segX0 = barX0 + ((ds.minTemp - globalMin) * (barX1 - barX0)) / (globalMax - globalMin);
                int segX1 = barX0 + ((ds.maxTemp - globalMin) * (barX1 - barX0)) / (globalMax - globalMin);
                int segW = segX1 - segX0;
                if (segW < 2) {
                    segW = 2;
                }
                g.setColor(AppTheme.textPrimary());
                g.fillRect(segX0, trackY, segW, 3);
            }

            g.setColor(AppTheme.textPrimary());
            g.drawString(maxStr, barX1 + 6, y + 10, Graphics.TOP | Graphics.LEFT);
        }

        g.setColor(AppTheme.textMuted());
        g.setFont(fTiny);
        g.drawString("OK: godzinowo tego dnia", 8, h - 16, Graphics.TOP | Graphics.LEFT);
    }
}
