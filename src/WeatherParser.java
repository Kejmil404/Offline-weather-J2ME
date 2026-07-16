import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/**
 * Parsuje plik tekstowy w formacie:
 *
 *   Miasto=Warszawa
 *   Data=2026-07-09
 *   Godzina=12:00
 *   TempC=23
 *   Opis=Slonecznie
 *   WiatrKmh=15
 *   Wilgotnosc=60
 *   ---
 *   Miasto=Warszawa
 *   Data=2026-07-10
 *   ...
 *
 * Kolejnosc pol jest dowolna, klucze nie rozrozniaja wielkosci liter.
 * Linia "---" (dokladnie trzy minusy) oddziela kolejne wpisy.
 * Puste linie sa ignorowane. Linie bez znaku '=' sa ignorowane
 * (mozna wiec dopisywac komentarze) - z JEDNYM wyjatkiem: jesli
 * PIERWSZA niepusta linia calego pliku nie zawiera znaku '=' i nie jest
 * "---", jest traktowana jako znacznik czasu synchronizacji (np.
 * "11.07.2026 - 17:58") i trafia do ParsedFile.syncTime zamiast byc
 * zignorowana. Jesli plik od razu zaczyna sie od "Miasto=..." albo
 * "---", to pole zostaje puste - appka po prostu nie pokaze znacznika.
 *
 * Uwaga implementacyjna: CLDC 1.1 nie ma klasy BufferedReader (to
 * pelnoprawna Java SE, nie CLDC), wiec czytamy caly plik recznie jako
 * bajty i dzielimy na linie samemu - bez String.split(), bo tego tez
 * CLDC nie ma.
 */
public final class WeatherParser {

    private WeatherParser() {
    }

    public static ParsedFile parse(InputStream is) throws IOException {
        String content = readAll(is);
        ParsedFile result = new ParsedFile();

        WeatherRecord rec = new WeatherRecord();
        boolean hasData = false;
        boolean checkedSyncLine = false;

        int pos = 0;
        int len = content.length();
        while (pos <= len) {
            int nl = content.indexOf('\n', pos);
            String line;
            if (nl < 0) {
                if (pos >= len) {
                    break;
                }
                line = content.substring(pos);
                pos = len + 1;
            } else {
                line = content.substring(pos, nl);
                pos = nl + 1;
            }

            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }
            line = line.trim();

            if (!checkedSyncLine && line.length() > 0) {
                checkedSyncLine = true;
                if (!line.equals("---") && line.indexOf('=') < 0) {
                    result.syncTime = new String(line);
                    continue;
                }
                // w przeciwnym razie to normalna linia danych - leci dalej
                // do zwyklego przetwarzania ponizej
            }

            if (line.length() == 0) {
                continue;
            }
            if (line.equals("---")) {
                if (hasData) {
                    result.records.addElement(rec);
                    rec = new WeatherRecord();
                    hasData = false;
                }
                continue;
            }
            int idx = line.indexOf('=');
            if (idx < 0) {
                continue;
            }
            String key = line.substring(0, idx).trim().toLowerCase();
            // "new String(...)" wymusza kopie znakow zamiast dzielenia
            // bufora z cala tresc pliku (klasyczna Java/CLDC potrafi
            // dzielic char[] miedzy substring() a oryginalem) - bez tego
            // caly wczytany plik zostalby w pamieci tak dlugo, jak dlugo
            // trzymamy chocby jedno pole ktoregokolwiek rekordu.
            String val = new String(line.substring(idx + 1).trim());
            hasData = true;

            if (key.equals("miasto")) {
                rec.miasto = val;
            } else if (key.equals("data")) {
                rec.data = val;
            } else if (key.equals("godzina")) {
                rec.godzina = val;
            } else if (key.equals("tempc")) {
                rec.tempC = val;
            } else if (key.equals("opis")) {
                rec.opis = val;
            } else if (key.equals("wiatrkmh")) {
                rec.wiatrKmh = val;
            } else if (key.equals("wilgotnosc")) {
                rec.wilgotnosc = val;
            } else if (key.equals("tempodczuwalnac")) {
                rec.tempOdczuwalnaC = val;
            } else if (key.equals("opadmm")) {
                rec.opadMm = val;
            } else if (key.equals("deszczmm")) {
                rec.deszczMm = val;
            } else if (key.equals("sniegcm")) {
                rec.sniegCm = val;
            } else if (key.equals("cisnieniehpa")) {
                rec.cisnienieHpa = val;
            } else if (key.equals("zachmurzenie")) {
                rec.zachmurzenie = val;
            } else if (key.equals("szansaopaduproc")) {
                rec.szansaOpaduProc = val;
            }
            // nieznane klucze sa po prostu ignorowane
        }
        if (hasData) {
            result.records.addElement(rec);
        }
        return result;
    }

    /**
     * Grupuje plaska liste wpisow po polu "Miasto", zachowujac kolejnosc
     * pierwszego wystapienia kazdego miasta (a wiec i kolejnosc z pliku).
     * Wpisy bez podanego miasta trafiaja do wspolnej grupy "" (pusta nazwa).
     */
    public static Vector groupByCity(Vector records) {
        Vector groups = new Vector();
        for (int i = 0; i < records.size(); i++) {
            WeatherRecord rec = (WeatherRecord) records.elementAt(i);
            String miasto = rec.miasto == null ? "" : rec.miasto;
            CityGroup grp = findGroup(groups, miasto);
            if (grp == null) {
                grp = new CityGroup();
                grp.miasto = miasto;
                groups.addElement(grp);
            }
            grp.records.addElement(rec);
        }
        return groups;
    }

    private static CityGroup findGroup(Vector groups, String miasto) {
        for (int i = 0; i < groups.size(); i++) {
            CityGroup g = (CityGroup) groups.elementAt(i);
            if (g.miasto.equals(miasto)) {
                return g;
            }
        }
        return null;
    }

    /**
     * Grupuje wpisy (jednego miasta) po polu "Data", liczac min/maks temp
     * i szczytowa szanse opadow dla kazdego dnia - do prognozy dziennej.
     */
    public static Vector groupByDay(Vector records) {
        Vector days = new Vector();
        for (int i = 0; i < records.size(); i++) {
            WeatherRecord rec = (WeatherRecord) records.elementAt(i);
            String date = rec.data == null ? "" : rec.data;
            DaySummary found = findDay(days, date);
            if (found == null) {
                found = new DaySummary();
                found.date = date;
                days.addElement(found);
            }
            int t = NumUtil.parseIntOrMin(rec.tempC);
            if (t != Integer.MIN_VALUE) {
                if (t < found.minTemp) {
                    found.minTemp = t;
                }
                if (t > found.maxTemp) {
                    found.maxTemp = t;
                }
            }
            int rp = NumUtil.parseIntOrMin(rec.szansaOpaduProc);
            if (rp != Integer.MIN_VALUE && rp > found.rainProc) {
                found.rainProc = rp;
            }
        }
        return days;
    }

    private static DaySummary findDay(Vector days, String date) {
        for (int i = 0; i < days.size(); i++) {
            DaySummary d = (DaySummary) days.elementAt(i);
            if (d.date.equals(date)) {
                return d;
            }
        }
        return null;
    }

    private static String readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[512];
        int n;
        while ((n = is.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        return new String(baos.toByteArray(), "UTF-8");
    }
}

