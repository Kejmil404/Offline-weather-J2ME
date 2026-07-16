/**
 * Pojedynczy wpis pogodowy wczytany z pliku tekstowego.
 * CLDC 1.1 nie ma generics ani autoboxingu w starym stylu, wiec trzymamy
 * wszystko jako Stringi - upraszcza to parsowanie i wyswietlanie.
 *
 * Pola ponizej TempC/Opis/WiatrKmh/Wilgotnosc sa opcjonalne - jesli
 * ich nie ma w pliku, zostaja puste i po prostu nie sa pokazywane.
 */
public class WeatherRecord {
    public String miasto = "";
    public String data = "";
    public String godzina = "";
    public String tempC = "";
    public String opis = "";
    public String wiatrKmh = "";
    public String wilgotnosc = "";

    // rozszerzone pola (opcjonalne)
    public String tempOdczuwalnaC = "";
    public String opadMm = "";
    public String deszczMm = "";
    public String sniegCm = "";
    public String cisnienieHpa = "";
    public String zachmurzenie = "";
    public String szansaOpaduProc = "";
}

