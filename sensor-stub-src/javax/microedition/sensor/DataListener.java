package javax.microedition.sensor;

/** MINIMALNY STUB do kompilacji - patrz komentarz w SensorInfo.java. */
public interface DataListener {
    void dataReceived(SensorConnection sensor, Data[] data, boolean isDataLost);
}
