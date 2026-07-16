/** Wspolny pomocnik parsowania - uzywany w kilku miejscach, zeby nie duplikowac logiki. */
public final class NumUtil {

    private NumUtil() {
    }

    /** Parsuje liczbe calkowita z ewentualna czescia dziesietna (np. "3.2" -> 3). Zwraca Integer.MIN_VALUE, jesli sie nie da. */
    public static int parseIntOrMin(String s) {
        if (s == null || s.length() == 0) {
            return Integer.MIN_VALUE;
        }
        try {
            int dot = s.indexOf('.');
            String intPart = dot >= 0 ? s.substring(0, dot) : s;
            return Integer.parseInt(intPart.trim());
        } catch (Exception e) {
            return Integer.MIN_VALUE;
        }
    }
}
