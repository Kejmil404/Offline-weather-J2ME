package javax.microedition.sensor;

/** MINIMALNY STUB do kompilacji - patrz komentarz w SensorInfo.java. */
public interface Data {
    ChannelInfo getChannelInfo();

    int[] getIntValues();

    double[] getDoubleValues();

    Object[] getObjectValues();

    long[] getTimestamps();

    int[] getUncertaintyValues();

    int[] getValidities();
}
