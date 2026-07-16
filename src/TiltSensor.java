import javax.microedition.io.Connector;
import javax.microedition.sensor.Data;
import javax.microedition.sensor.DataListener;
import javax.microedition.sensor.SensorConnection;
import javax.microedition.sensor.SensorInfo;
import javax.microedition.sensor.SensorManager;

/**
 * Odczyt akcelerometru przez JSR-256 (Mobile Sensor API), zeby wykrywac
 * przekrecenie telefonu i automatycznie przelaczac RotatableCanvas w
 * tryb "obrocony".
 *
 * Wzorzec API (SensorManager.findSensors, SensorConnection,
 * DataListener, Data.getIntValues(), kolejnosc kanalow X=0/Y=1) wzorowany
 * na kodzie z bloga profesjonalnego dewelopera J2ME, ktory uzyl
 * dokladnie tego samego mechanizmu w prawdziwej, wydanej grze na
 * Sony Ericsson W910i (ta sama rodzina platformy Sony Ericsson co
 * W595 - obie wspieraja JSR-256 od JP-8 wzwyz):
 * https://gamesdev.wordpress.com/2008/11/03/circuit-simple-j2me-game/
 *
 * Progi/histereza (TILT_ON/TILT_OFF) sa moim dopisaniem "na oko" - nie
 * mialem jak ich dostroic na prawdziwym telefonie. Jesli auto-obrot
 * bedzie zbyt czuly (miga) albo za mao czuly (nie reaguje), te dwie
 * stale sa pierwszym miejscem do poprawki.
 *
 * Kazdy blad (brak JSR-256 na telefonie, brak zgody, klasa w ogole
 * niedostepna w tym buildzie) jest lapany i traktowany jako "czujnik
 * niedostepny" - appka wtedy po prostu wraca do recznego przelacznika
 * "Obroc ekran", zamiast sie wywalic.
 */
public class TiltSensor implements DataListener {

    public interface Listener {
        void onOrientationChanged(boolean landscape);
    }

    private static final String QUANTITY = "acceleration";

    // progi w jednostkach jakie zwraca sensor (typowo mili-g, ale rozne
    // telefony/build'y moga sie roznic - stad histereza jest szeroka)
    private static final int TILT_ON = 500;
    private static final int TILT_OFF = 300;

    private SensorInfo sensorInfo;
    private SensorConnection conn;
    private final Listener listener;
    private boolean available;
    private boolean currentLandscape = false;

    public TiltSensor(Listener listener) {
        this.listener = listener;
        try {
            SensorInfo[] infos = SensorManager.findSensors(QUANTITY, SensorInfo.CONTEXT_TYPE_USER);
            if (infos != null && infos.length > 0) {
                sensorInfo = infos[0];
                available = true;
            } else {
                available = false;
            }
        } catch (Throwable t) {
            // JSR-256 niedostepne w tym buildzie/telefonie
            available = false;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public void start() {
        if (!available || conn != null) {
            return;
        }
        try {
            conn = (SensorConnection) Connector.open(sensorInfo.getUrl());
            conn.setDataListener(this, 1);
        } catch (Throwable t) {
            available = false;
            conn = null;
        }
    }

    public void stop() {
        if (conn == null) {
            return;
        }
        try {
            conn.removeDataListener();
            conn.close();
        } catch (Throwable t) {
            // ignorujemy - i tak zwalniamy referencje ponizej
        }
        conn = null;
    }

    public void dataReceived(SensorConnection connection, Data[] data, boolean isDataLost) {
        if (data == null || data.length < 2) {
            return;
        }
        try {
            int x = data[0].getIntValues()[0];
            int y = data[1].getIntValues()[0];
            int absX = Math.abs(x);
            int absY = Math.abs(y);

            boolean newLandscape = currentLandscape;
            if (!currentLandscape && absX > TILT_ON && absX > absY) {
                newLandscape = true;
            } else if (currentLandscape && (absX < TILT_OFF || absY > absX)) {
                newLandscape = false;
            }

            if (newLandscape != currentLandscape) {
                currentLandscape = newLandscape;
                if (listener != null) {
                    listener.onOrientationChanged(currentLandscape);
                }
            }
        } catch (Throwable t) {
            // dane w nieoczekiwanym formacie - ignorujemy pojedyncza probke,
            // nie wywalamy calej appki
        }
    }
}
