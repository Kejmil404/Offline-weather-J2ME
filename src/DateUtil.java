import java.util.Calendar;

/**
 * Pomocnicze metody dat oparte o zegar telefonu (java.util.Calendar -
 * jest w CLDC 1.1, w odroznieniu od np. BufferedReader). Uzywane do:
 * - znalezienia najblizszego "teraz" wpisu przy starcie appki
 * - etykiet "Dzisiaj"/"Jutro"/dzien tygodnia w prognozie dziennej
 *
 * Skroty dni tygodnia sa CELOWO bez polskich znakow diakrytycznych
 * (Sr zamiast Śr itp.) - stare fonty MIDP bywaja z nimi kapryśne,
 * a to pole pojawia sie w kilku miejscach interfejsu naraz.
 */
public final class DateUtil {

    private DateUtil() {
    }

    /** {rok, miesiac 1-12, dzien, godzina 0-23, minuta} wg zegara telefonu. */
    public static int[] now() {
        Calendar cal = Calendar.getInstance();
        return new int[]{
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE)
        };
    }

    public static String nowIsoDate() {
        int[] n = now();
        return pad4(n[0]) + "-" + pad2(n[1]) + "-" + pad2(n[2]);
    }

    /** "YYYY-MM-DD HH:MM" wg zegara telefonu - do porownan leksykograficznych z Data+Godzina z pliku. */
    public static String nowIsoDateTime() {
        int[] n = now();
        return pad4(n[0]) + "-" + pad2(n[1]) + "-" + pad2(n[2]) + " " + pad2(n[3]) + ":" + pad2(n[4]);
    }

    /**
     * Zwraca "Dzisiaj"/"Jutro", sam skrot dnia tygodnia (dla najblizszych
     * dni biezacego tygodnia), albo skrot + data (np. "Pon. 20.07") gdy
     * data jest dalsza niz tydzien - sam skrot dnia tygodnia zaczyna byc
     * niejednoznaczny ("ktory poniedzialek?"), wiec doklejamy date.
     * Jesli daty nie da sie sparsowac, zwraca oryginalny tekst bez zmian.
     */
    public static String dayLabel(String isoDate) {
        int[] parts = parseIsoDate(isoDate);
        if (parts == null) {
            return isoDate == null ? "" : isoDate;
        }
        try {
            int[] n = now();
            long diff = toDayNumber(parts[0], parts[1], parts[2]) - toDayNumber(n[0], n[1], n[2]);
            if (diff == 0) {
                return "Dzisiaj";
            }
            if (diff == 1) {
                return "Jutro";
            }
            String weekday = weekdayAbbrev(parts[0], parts[1], parts[2]);
            if (diff >= 2 && diff <= 6) {
                return weekday;
            }
            String dateSuffix = pad2(parts[2]) + "." + pad2(parts[1]);
            return weekday.length() > 0 ? (weekday + ". " + dateSuffix) : dateSuffix;
        } catch (Exception e) {
            return weekdayAbbrev(parts[0], parts[1], parts[2]);
        }
    }

    /**
     * Numer dnia w kalendarzu proleptycznym gregoriańskim (Julian Day
     * Number) - czysta arytmetyka calkowitoliczbowa, bez zadnych metod
     * Calendar, wiec nie zalezy od tego, ktore z nich akurat sa dostepne
     * w konkretnym CLDC. Uzywana tylko do liczenia roznicy dni miedzy
     * dwiema datami (dzien = wynik1 - wynik2).
     */
    private static long toDayNumber(int year, int month, int day) {
        int a = (14 - month) / 12;
        int y = year + 4800 - a;
        int m = month + 12 * a - 3;
        return day + (153 * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045;
    }

    /** Skrot dnia tygodnia (Pn/Wt/Sr/Czw/Pt/Sob/Nie) dla podanej daty. Pusty string, jesli sie nie uda. */
    public static String weekdayAbbrev(int year, int month, int day) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month - 1);
            cal.set(Calendar.DAY_OF_MONTH, day);
            int dow = cal.get(Calendar.DAY_OF_WEEK); // 1=Niedziela .. 7=Sobota
            String[] names = {"", "Nie", "Pon", "Wt", "Sr", "Czw", "Pt", "Sob"};
            if (dow >= 1 && dow <= 7) {
                return names[dow];
            }
        } catch (Exception e) {
            // ignorujemy - zwrocimy pusty string ponizej
        }
        return "";
    }

    /** Parsuje "YYYY-MM-DD" na {rok, miesiac, dzien}, albo null jesli sie nie da. */
    public static int[] parseIsoDate(String iso) {
        if (iso == null || iso.length() < 10) {
            return null;
        }
        try {
            int year = Integer.parseInt(iso.substring(0, 4));
            int month = Integer.parseInt(iso.substring(5, 7));
            int day = Integer.parseInt(iso.substring(8, 10));
            return new int[]{year, month, day};
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Szuka wzorca "HH:MM" (godzina:minuta) gdziekolwiek w podanym tekscie
     * i zwraca go znormalizowany (2 cyfry godziny). Uzywane do wyciagniecia
     * godziny ze znacznika synchronizacji (ktorego format jest dowolny -
     * appka go nie parsuje jako date, ale godzine z niego mozemy odczytac
     * do pozycjonowania wykresu). Zwraca null, jesli nic pasujacego nie
     * znaleziono.
     */
    public static String extractHHMM(String text) {
        if (text == null) {
            return null;
        }
        int colonIdx = text.lastIndexOf(':');
        if (colonIdx < 0 || colonIdx + 3 > text.length()) {
            return null;
        }
        String mm = text.substring(colonIdx + 1, colonIdx + 3);
        if (!isTwoDigits(mm)) {
            return null;
        }
        int start = colonIdx;
        while (start > 0 && isDigitChar(text.charAt(start - 1)) && (colonIdx - start) < 2) {
            start--;
        }
        String hh = text.substring(start, colonIdx);
        if (hh.length() == 0 || !isDigitsOnly(hh)) {
            return null;
        }
        if (hh.length() == 1) {
            hh = "0" + hh;
        }
        return hh + ":" + mm;
    }

    private static boolean isDigitChar(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isDigitsOnly(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!isDigitChar(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isTwoDigits(String s) {
        return s.length() == 2 && isDigitsOnly(s);
    }

    private static String pad2(int n) {
        return (n < 10 ? "0" : "") + n;
    }

    private static String pad4(int n) {
        String s = String.valueOf(n);
        while (s.length() < 4) {
            s = "0" + s;
        }
        return s;
    }
}
