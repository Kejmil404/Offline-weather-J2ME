package javax.microedition.sensor;

/**
 * MINIMALNY STUB do kompilacji (JSR-256 nie jest w zwyklym Sun WTK).
 * Sygnatury zgodne ze specyfikacja JSR-256. W runtime na prawdziwym
 * telefonie uzywana jest PRAWDZIWA implementacja z ROM-u urzadzenia -
 * ten plik NIGDY nie trafia do finalnego weather.jar (build.sh uzywa
 * go tylko jako bootclasspath przy kompilacji, tak samo jak
 * cldcapi11.jar/midpapi20.jar).
 */
public interface SensorInfo {
    String CONTEXT_TYPE_AMBIENT = "ambient";
    String CONTEXT_TYPE_USER = "user";
    String CONTEXT_TYPE_VEHICLE = "vehicle";
    String CONTEXT_TYPE_UNKNOWN = "unknown";

    String getUrl();

    String getQuantity();

    String getContextType();

    String getConnectionType();

    String getModel();

    int getMaxRate();

    boolean isAvailable();

    ChannelInfo[] getChannelInfos();

    String getDescription();

    Object getProperty(String name);

    String[] getPropertyNames();
}
