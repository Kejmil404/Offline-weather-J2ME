package javax.microedition.sensor;

import javax.microedition.io.Connection;

/** MINIMALNY STUB do kompilacji - patrz komentarz w SensorInfo.java. */
public interface SensorConnection extends Connection {
    int STATE_CLOSED = 0;
    int STATE_OPENED = 1;
    int STATE_LISTENING = 2;

    ChannelInfo[] getChannelInfos();

    Data[] getData(int bufferSize);

    int getState();

    void setDataListener(DataListener listener, int bufferSize);

    void removeDataListener();
}
