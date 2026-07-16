package javax.microedition.sensor;

/**
 * MINIMALNY STUB do kompilacji - patrz komentarz w SensorInfo.java.
 * Cialo metod nie ma znaczenia (zwraca null) - w runtime na prawdziwym
 * telefonie i tak wykonuje sie kod z ROM-u urzadzenia, nie ten plik.
 */
public final class SensorManager {

    private SensorManager() {
    }

    public static SensorInfo[] findSensors(String quantity, String contextType) {
        return null;
    }

    public static SensorInfo[] findSensors(String url) {
        return null;
    }
}
