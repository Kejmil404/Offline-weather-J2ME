import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Canvas z "obrotem" ekranu o 90 stopni - automatycznym przez akcelerometr
 * (JSR-256, patrz TiltSensor.java) jesli telefon go udostepnia, a jesli
 * nie - recznym przelacznikiem w menu ("Obroc ekran").
 *
 * W595 FAKTYCZNIE ma akcelerometr (uzywany np. do auto-obrotu w Media
 * Center i Shake Control w Walkmanie) i jest na platformie Sony Ericsson
 * JP-8.3, ktora wg dokumentacji SE wspiera JSR-256 (Mobile Sensor API)
 * dla aplikacji trzecich - wiec teoretycznie prawdziwy auto-obrot
 * powinien dzialac. Mechanizm odczytu czujnika (TiltSensor) jest oparty
 * na kodzie, ktory faktycznie dzialal na prawdziwym Sony Ericssonie
 * (patrz komentarz w TiltSensor.java) - ale progi czulosci sa moim
 * zgadywaniem i mogly wymagac dostrojenia po przetestowaniu.
 *
 * Jesli TiltSensor zglosi, ze czujnik jest niedostepny (np. akurat ten
 * build firmware go nie udostepnia MIDletom, mimo ze platforma
 * teoretycznie powinna), appka automatycznie chowa sie za reczny
 * przelacznik "Obroc ekran" w menu - wiec w najgorszym razie wracamy
 * do rozwiazania, ktore juz wiemy ze dziala mechanicznie (sam obrot
 * pikseli), tylko bez automatyki.
 *
 * Sam mechanizm obrotu obrazu: rysujemy caly ekran do bufora w
 * "logicznej" orientacji (zamienionych wymiarach), potem recznie
 * obracamy piksele o 90 stopni i wklejamy na prawdziwy ekran przez
 * Graphics.drawRGB. To standardowe metody MIDP 2.0 (Image.createImage,
 * Image.getRGB, Graphics.drawRGB) - jesli obrazek wyjdzie w zla strone,
 * to kwestia zamiany dwoch linijek zaznaczonych nizej.
 *
 * Znana wada: klawisze (gora/dol/lewo/prawo) NIE sa przemapowane w
 * trybie obroconym - dzialaja tak jak w trybie portretowym.
 */
public abstract class RotatableCanvas extends Canvas implements TiltSensor.Listener {

    protected boolean landscape = false;

    protected final Command cmdRotate = new Command("Obroc ekran", Command.SCREEN, 8);

    private TiltSensor tiltSensor;
    private boolean autoRotateHandledCommand = false;

    /** Podklasy rysuja tutaj, zawsze zakladajac wymiary w i h przekazane jako argumenty (NIE getWidth()/getHeight()). */
    protected abstract void paintContent(Graphics g, int w, int h);

    public void toggleLandscape() {
        landscape = !landscape;
        repaint();
    }

    /** Wywolywane automatycznie przez platforme, gdy ten ekran staje sie widoczny. */
    protected void showNotify() {
        if (tiltSensor == null) {
            tiltSensor = new TiltSensor(this);
            if (tiltSensor.isAvailable() && !autoRotateHandledCommand) {
                // czujnik dziala - reczny przelacznik jest zbedny, chowamy go
                // (tak jak w prawdziwych telefonach z auto-obrotem nie ma
                // recznej opcji "obroc" w menu)
                removeCommand(cmdRotate);
                autoRotateHandledCommand = true;
            }
        }
        tiltSensor.start();
    }

    /** Wywolywane automatycznie, gdy ten ekran przestaje byc widoczny (np. przejscie na inny ekran). */
    protected void hideNotify() {
        if (tiltSensor != null) {
            tiltSensor.stop();
        }
    }

    /** TiltSensor.Listener - wywolywane z watku czujnika, gdy wykryty jest obrot. */
    public void onOrientationChanged(boolean newLandscape) {
        landscape = newLandscape;
        repaint();
    }

    /**
     * Domyslnie przytrzymanie klawisza dziala tak samo jak wielokrotne
     * jego wcisniecie - dziala automatycznie w kazdym ekranie dziedziczacym
     * po tej klasie (WeatherCanvas, DailyForecastCanvas), bo obie maja
     * juz sensownie zaimplementowane keyPressed(). Platforma sama decyduje
     * czy wysyla te zdarzenia (hasRepeatEvents()) - jesli nie, nic sie nie
     * dzieje, zwykle wcisniecia dalej dzialaja normalnie.
     */
    protected void keyRepeated(int keyCode) {
        keyPressed(keyCode);
    }

    protected void paint(Graphics g) {
        int physW = getWidth();
        int physH = getHeight();

        if (!landscape) {
            paintContent(g, physW, physH);
            return;
        }

        try {
            int logW = physH;
            int logH = physW;

            Image buffer = Image.createImage(logW, logH);
            Graphics bg = buffer.getGraphics();
            paintContent(bg, logW, logH);

            int[] src = new int[logW * logH];
            buffer.getRGB(src, 0, logW, 0, 0, logW, logH);

            int[] dst = new int[physW * physH];
            for (int y = 0; y < logH; y++) {
                for (int x = 0; x < logW; x++) {
                    // obrot o 90 st. w prawo (clockwise). Jesli po tescie na
                    // telefonie okaze sie, ze obraca w zla strone, zamien
                    // ponizsze dwie linijki miejscami (albo napisz do mnie).
                    int nx = logH - 1 - y;
                    int ny = x;
                    dst[ny * physW + nx] = src[y * logW + x];
                }
            }
            g.drawRGB(dst, 0, physW, 0, 0, physW, physH, false);
        } catch (Throwable t) {
            // Na starym/slabym telefonie moze zabraknac pamieci na bufor -
            // wtedy po prostu wracamy do trybu portretowego zamiast wywalic
            // aplikacje.
            landscape = false;
            paintContent(g, physW, physH);
        }
    }
}
