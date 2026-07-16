package javax.microedition.sensor;

/** MINIMALNY STUB do kompilacji - patrz komentarz w SensorInfo.java. */
public interface ChannelInfo {
    String getName();

    int getDataType();

    double getAccuracy();

    String getUnit();

    int getScale();

    MeasurementRange[] getMeasurementRanges();
}
