# Pogoda Offline – J2ME dla Sony Ericsson W595

Aplikacja MIDP 2.0 / CLDC 1.1 wyświetlająca dane pogodowe wczytane
z pliku tekstowego, który przesyłasz na telefon przez Bluetooth.
Ekran zoptymalizowany pod 240x320 (natywna rozdzielczość W595).

## Co jest w paczce

```
src/                 - kod źródłowy (17 klas Javy)
sensor-stub-src/     - minimalne stuby JSR-256 (do kompilacji, patrz "Obrót ekranu")
weather.jad          - deskryptor aplikacji z uprawnieniami do FileConnection
MANIFEST.MF          - manifest do środka JAR-a
build.sh             - skrypt do kompilacji z linii poleceń (zalecany)
build.xml            - skrypt Ant + Antenna do kompilacji (alternatywa)
pogoda_przyklad.txt        - przykładowy plik z danymi do testów (2 miasta)
pogoda_tydzien_test.txt    - przykładowy plik z gęstymi danymi (tydzień co godzinę, 2 miasta)
```

## Dlaczego JSR-75 (FileConnection)

W595 obsługuje JSR-75, więc appka może samodzielnie przeglądać system
plików telefonu i otworzyć wskazany .txt. Bez tego JSR-a zwykły MIDlet
w ogóle nie ma dostępu do systemu plików – dlatego w JAD-zie jest
wpis `MIDlet-Permissions: javax.microedition.io.Connector.file.read`.
Przy pierwszym uruchomieniu telefon zapyta o zgodę na dostęp do plików –
trzeba potwierdzić (najlepiej "zawsze zezwalaj").

## Budowanie JAR-a na Linux Mint

Nie mam tu środowiska z Wireless Toolkit, więc nie mogę Ci oddać
gotowego, skompilowanego .jar – ale masz dwie ścieżki do wyboru.

### Skąd wziąć narzędzia

Potrzebujesz **Sun Java Wireless Toolkit (WTK) 2.5.2** – to jedno
archiwum daje Ci `preverify` oraz biblioteki CLDC-1.1/MIDP-2.0/JSR-75
potrzebne do kompilacji.

1. Pobierz: https://www.oracle.com/java/technologies/java-archive-downloads-javame-downloads.html
   → sekcja "Sun Java Wireless Toolkit 2.5.2_01" → wersja Linux
   (`sun_java_wireless_toolkit-2.5.2_01-linuxi486.bin.sh`). Oracle
   poprosi o zalogowanie się na (darmowe) konto Oracle – to normalne
   przy starych archiwach.
2. To jest binarka **32-bitowa**, więc na 64-bitowym Mincie dorzuć
   biblioteki i386:
   ```
   sudo dpkg --add-architecture i386
   sudo apt update
   sudo apt install libc6:i386 libncurses5:i386 libstdc++6:i386 libx11-6:i386 libxext6:i386 libxi6:i386
   ```
3. Sam instalator WTK też chce 32-bitowej Javy do działania:
   ```
   sudo apt install openjdk-8-jdk:i386
   ```
   (jeśli tego pakietu nie ma w repo Mint, weź 32-bitowy JDK 8 np. z
   adoptium.net i wskaż jego ścieżkę instalatorowi)
4. Zainstaluj do katalogu **bez spacji w ścieżce**, np. `~/wtk252`:
   ```
   chmod +x sun_java_wireless_toolkit-2.5.2_01-linuxi486.bin.sh
   ./sun_java_wireless_toolkit-2.5.2_01-linuxi486.bin.sh
   ```

Do samej kompilacji **nie musisz odpalać GUI WTK** (KToolbar) – potrzebne
jest tylko `bin/preverify` i jary z `lib/`, więc większość problemów
z 32-bit GUI Cię nie dotyczy.

### Kompilacja – prosty skrypt (zalecane)

Dołączyłem `build.sh`, który robi to samo co Ant, ale bez dodatkowej
zależności (Antenna). Ustaw zmienną `WTK_HOME` na katalog, gdzie
zainstalował się WTK, i odpal:

```bash
export WTK_HOME=~/wtk252
./build.sh
```

Skrypt sam znajdzie jar JSR-75 (nazwa pliku bywa różna między
dystrybucjami WTK – np. `jsr75.jar` albo coś z "fileconn" w nazwie),
skompiluje, przeweryfikuje i zapakuje `dist/weather.jar` +
`dist/weather.jad` (z automatycznie zaktualizowanym `MIDlet-Jar-Size`).

Jeśli skrypt nie znajdzie jara JSR-75 automatycznie, sprawdź ręcznie:
```bash
find "$WTK_HOME" -iname "*.jar" | grep -i file
```
i wklej znalezioną ścieżkę bezpośrednio do zmiennej `JSR75_JAR` w
`build.sh`.

### Kompilacja – Ant (alternatywa)

`build.xml` w projekcie też działa, ale wymaga dodatkowo pluginu
Antenna (https://antenna.sourceforge.net/, dorzuć `antenna.jar` do
`ANT_HOME/lib`) i ustawionej zmiennej `WTK_HOME`. Wtedy:
```
ant clean package
```

### Jeśli Oracle nie chce Cię wpuścić / konto sprawia problem

Napisz, spróbuję poszukać zwierciadła binarki WTK 2.5.2 gdzie indziej
(archive.org itp.) – nie chciałem zgadywać linków, które mogą już nie
działać.

## Instalacja na W595

- Podepnij telefon kablem/Bluetooth do PC Suite/File Manager i wrzuć
  `weather.jar` + `weather.jad` do pamięci telefonu lub karty, potem
  z poziomu menedżera plików telefonu zainstaluj (wybierz `.jad`).
- Albo wyślij `.jar` przez Bluetooth bezpośrednio na telefon – W595
  sam zaproponuje instalację po odebraniu pliku.

## Jak przesłać dane pogodowe

1. Na dowolnym urządzeniu z Bluetooth przygotuj plik `.txt` w formacie
   opisanym niżej (patrz `pogoda_przyklad.txt`).
2. Wyślij go przez Bluetooth na W595 (Wyślij plik → Bluetooth →
   wybierz telefon). Telefon zapisze go zwykle w katalogu
   `Inne` / `Other`, na karcie pamięci (`E:/other/...`) albo w pamięci
   telefonu (`C:/other/...`), zależnie od ustawień.
3. Odpal aplikację "Pogoda Offline". Przy pierwszym uruchomieniu (albo
   po kliknięciu "Inny plik") appka pokaże przeglądarkę dysków/katalogów
   – wejdź do katalogu, w którym telefon zapisał odebrany plik, i wybierz
   go strzałką + "Wybierz". Ścieżka zostaje zapamiętana, więc przy
   kolejnym uruchomieniu appka sama go wczyta.
4. "Odśwież" (środkowy klawisz/joystick) wczytuje plik ponownie – przydatne
   gdy podmienisz dane nowszymi przez Bluetooth bez reinstalacji appki.

## Format pliku z danymi

**Opcjonalny znacznik czasu synchronizacji** – jeśli chcesz, żeby appka
pokazywała kiedy plik został wygenerowany, pierwsza (niepusta) linia
pliku może być samym znacznikiem czasu, bez `Klucz=`:

```
11.07.2026 - 17:58
---
Miasto=Warszawa
...
```

Appka nie parsuje tego jako datę – po prostu wyświetla dokładnie ten
tekst pod nagłówkiem jako "Zsynchronizowano: ...". Format jest więc
w pełni dowolny, `DD.MM.YYYY - HH:MM` to tylko propozycja. Jeśli
pierwsza linia pliku od razu zaczyna się od `Miasto=` (albo `---`),
appka po prostu nie pokazuje znacznika – stary format bez tej linii
dalej działa bez zmian.

Reszta pliku: klucz=wartość, jeden wpis (np. jedna prognoza na dany
dzień/godzinę) oddzielony linią z samymi trzema myślnikami `---`.
Kolejność pól dowolna, wielkość liter w kluczach nie ma znaczenia.
Podstawowe pola:

```
Miasto=Warszawa
Data=2026-07-09
Godzina=12:00
TempC=23
Opis=Slonecznie
WiatrKmh=15
Wilgotnosc=45
```

Opcjonalnie appka rozpoznaje też rozszerzony zestaw pól (jeśli ich nie
podasz, po prostu nie są pokazywane – działa też stary, prostszy format):

```
TempOdczuwalnaC=19
OpadMm=0.0
DeszczMm=0.0
SniegCm=0.0
CisnienieHpa=1016
Zachmurzenie=99
SzansaOpaduProc=45
```

`SzansaOpaduProc` to procent szansy opadów (0-100) – appka pokazuje go
jako jedną etykietę przy szczycie krzywej na wykresie godzinowym
(patrz "Funkcje" niżej), tak jak w apkach pogodowych na smartfony.

**Wiele miast w jednym pliku:** appka sama grupuje wpisy po polu
`Miasto` i pozwala się między nimi przełączać (patrz sterowanie niżej).
Wystarczy w jednym pliku pomieszać wpisy dla różnych miast – kolejność
nieistotna, grupowanie jest automatyczne. Zobacz `pogoda_przyklad.txt`
(Warszawa + Kraków, z pełnym zestawem pól) dla przykładu.

Uwaga co do polskich znaków: stare fonty MIDP bywają kapryśne z
diakrytykami. Jeśli po wgraniu zobaczysz krzaki zamiast "ą/ć/ł" itd.,
najprościej jest pisać opisy bez ogonków (`Slonecznie` zamiast
`Słonecznie`) – appka i tak działa, parser nie wymaga polskich znaków.

Zapisuj plik w kodowaniu UTF-8 (parser czyta jako UTF-8).

**Uwaga co do symbolu stopnia (°):** przy tej przebudowie zamieniłem
zapis temperatury z "23 C" na "23°" (symbol stopnia), żeby wyglądało
bardziej jak prawdziwa apka pogodowa. Nie miałem jak sprawdzić, czy
font systemowy W595 ma ten znak – jeśli po instalacji zobaczysz pusty
kwadracik/krzaka zamiast "°", daj znać, cofnę to do zwykłej litery "C"
(to jedna linijka do zmiany w kilku miejscach w `WeatherCanvas.java`).

## Sterowanie w aplikacji

**Ekran główny:**
- Góra/dół/lewo/prawo – przełącza między kolejnymi wpisami czasowymi
  (np. prognoza na kolejne dni/godziny) dla aktualnie wybranego miasta.
  Działa też przy **przytrzymaniu** klawisza (ciągłe przewijanie), o ile
  telefon wysyła zdarzenia powtórzenia klawisza (większość MIDP 2.0 to
  robi)
- `*` / `#` – skok do poprzedniego/następnego dnia (przydatne przy
  danych co godzinę – nie trzeba przewijać pojedynczo)
- Środek/joystick (OK/FIRE) – otwiera prognozę dzienną
- Menu telefonu (lewy softkey/Opcje): "Inny plik", "Prognoza dzienna",
  "Odśwież", "Miasta" (widoczne tylko gdy plik zawiera więcej niż
  jedno miasto), "Zmień motyw", "Obróć ekran" (eksperymentalne, patrz
  niżej)
- Prawy softkey – wyjście

**Ekran prognozy dziennej:**
- Góra/dół – przewija listę dni (też działa przy przytrzymaniu)
- OK/FIRE na wybranym dniu – wraca do ekranu głównego i przeskakuje do
  pierwszego wpisu godzinowego tego dnia
- Lewy softkey – powrót bez zmiany dnia

## Funkcje

- **Ciemny motyw + jasny motyw** – przełączane komendą "Zmień motyw"
  (dostępną na obu ekranach), zapamiętywane w RecordStore, więc appka
  pamięta wybór między uruchomieniami
- **Automatyczny wybór "teraz"** – appka czyta datę/godzinę z zegara
  telefonu (`java.util.Calendar`, jest w CLDC 1.1) i przy wczytaniu
  pliku (albo zmianie miasta) od razu pokazuje najbliższy nadchodzący
  wpis zamiast zawsze zaczynać od początku pliku. Jeśli wszystkie wpisy
  są już w przeszłości, pokazuje ostatni
- **Wiele miast w jednym pliku** – automatyczne grupowanie po polu
  `Miasto`, przełączanie przez menu "Miasta"
- **Kompaktowy wykres godzinowy na ekranie głównym** – krzywa
  temperatury dla najbliższych ~6 wpisów (etykiety godzin, kropki na
  punktach, aktualnie wybrany wpis podświetlony), plus procent szansy
  opadów pokazany jako jedna etykieta przy szczycie krzywej, oraz
  etykieta "Dzisiaj"/"Jutro"/dzień tygodnia nad wykresem pokazująca,
  którego dnia dotyczy aktualnie wybrany wpis – dokładnie w duchu
  wykresu z apki, którą pokazałeś jako inspirację. To jedyny wykres w
  appce – osobny, pełnoekranowy wykres z poprzedniej wersji został
  usunięty, bo się z nim duplikował
- **Prognoza dzienna (osobny ekran)** – lista dni z min/maks
  temperaturą na wspólnej skali (pasek pokazuje zakres danego dnia na
  tle zakresu wszystkich dni w pliku, tak jak w apce-inspiracji) i
  szczytową szansą opadów. Wybranie dnia przeskakuje na ekranie głównym
  do jego pierwszego wpisu godzinowego
- **Min/maks temperatury na dany dzień** – strzałki w nagłówku ekranu
  głównego obok opisu pogody, liczone ze wszystkich wpisów danego dnia
  w pliku
- **Rozszerzone dane** – temperatura odczuwalna, ciśnienie,
  zachmurzenie, opady/deszcz/śnieg, procent szansy opadów (pokazywane
  tylko jeśli są w pliku, siatka 2-kolumnowa z automatycznym
  przycinaniem tekstu, żeby nic nie nachodziło na sąsiednią kolumnę)
- **Znacznik czasu synchronizacji** – jeśli pierwsza niepusta linia
  pliku nie jest polem `Klucz=wartość` ani `---`, appka pokazuje ją
  pod paskiem górnym jako "Zsynchronizowano: ...". Format dowolny
  (proponowany: `11.07.2026 - 17:58`), appka niczego nie parsuje w tym
  polu, po prostu wyświetla jak jest. Tekst jest teraz wyrównany do
  lewej i przycinany do szerokości ekranu (wcześniej wyśrodkowany
  długi tekst potrafił wystawać poza lewą krawędź i obcinać "Z" na
  początku)
- **Kolorowana temperatura** – niebieska poniżej 0°C, czerwona od 25°C
  wzwyż, żeby szybciej "wyczuć" pogodę bez czytania liczb
- **Obrót ekranu (auto + ręczny fallback)** – patrz sekcja niżej

### Obrót ekranu

W595 **faktycznie ma akcelerometr** (używany np. do auto-obrotu w
Media Center i Shake Control w Walkmanie) i jest na platformie Sony
Ericsson **JP-8.3**, która wg dokumentacji SE wspiera **JSR-256
(Mobile Sensor API)** dla aplikacji trzecich – więc appka próbuje
teraz **prawdziwego automatycznego obrotu** przez odczyt czujnika
(`TiltSensor.java`), a dopiero jeśli czujnik okaże się niedostępny,
chowa się za ręczny przełącznik "Obróć ekran" w menu.

Mechanizm odczytu czujnika (`SensorManager`, `SensorConnection`,
`DataListener`) jest wzorowany na kodzie, który **faktycznie działał
na prawdziwym Sony Ericssonie** (W910i, ta sama rodzina platformy) –
opublikowanym przez profesjonalnego dewelopera gier J2ME:
https://gamesdev.wordpress.com/2008/11/03/circuit-simple-j2me-game/
Nie jest to więc czysta spekulacja co do kształtu API.

**Mimo to, kilka rzeczy jest niepewnych i mogą wymagać dostrojenia:**
- Progi czułości (`TILT_ON`/`TILT_OFF` w `TiltSensor.java`) są moim
  zgadywaniem "na oko" – jeśli auto-obrót będzie zbyt czuły (miga) albo
  za mało czuły (nie reaguje), to pierwsze miejsce do poprawki
  (podaj mi dokładnie jak się zachowuje, dostroimy razem)
- Sama dokumentacja Sony Ericsson SDK określa wsparcie JSR-256 jako
  "ograniczone" (limited runtime support) – możliwe że na Twoim
  konkretnym firmware zwyczajnie nie zadziała mimo że platforma
  teoretycznie powinna to wspierać. Appka wtedy **automatycznie**
  wykryje brak czujnika i pokaże z powrotem ręczny przełącznik –
  nic się nie wywali, po prostu nie będzie auto-obrotu
- Klawisze nawigacji **nie są przemapowane** w trybie obróconym

**Dodatkowy krok do kompilacji:** klasy JSR-256
(`javax.microedition.sensor.*`) **nie są częścią** zwykłego Sun WTK
2.5.2. Zamiast każdemu kazać ściągać cały Sony Ericsson SDK tylko po
jeden plik, dołączyłem **własne minimalne stuby** tych klas
(`sensor-stub-src/`) – same sygnatury metod, wystarczające żeby `javac`
sprawdził typy przy kompilacji. `build.sh` kompiluje je automatycznie
przy pierwszym uruchomieniu (widzisz to jako "Nie znalazlem
prawdziwego jara JSR-256 - kompiluje wlasne stuby" w konsoli) i używa
jako część bootclasspath – **dokładnie tak samo** jak już działające
`cldcapi11.jar`/`midpapi20.jar`. Te stuby nigdy nie trafiają do
finalnego `weather.jar` (leżą tylko w `build/`, który jest tymczasowy)
– w runtime na telefonie liczą się **prawdziwe klasy z ROM-u
urządzenia**, bo to firmware faktycznie implementuje JSR-256, a nie coś
co appka ze sobą niesie.

Jeśli mimo wszystko masz gdzieś prawdziwy jar z Sony Ericsson SDK,
`build.sh` go wykryje pierwszeństwo (szuka w `WTK_HOME` i opcjonalnym
`SE_SDK_HOME`) i użyje zamiast stubów – nie jest to jednak konieczne.

Jeśli po testach na telefonie uznasz, że auto-obrót nie ma sensu, po
prostu nie przejmuj się tym – appka i tak sama pokaże ręczny
przełącznik, gdyby czujnik był niedostępny w runtime.

## Ile danych appka udźwignie

Testowałem to logicznie (nie na sprzęcie, patrz zastrzeżenia w tym
README), ale konkretne mechanizmy są przygotowane pod większe pliki:

- **Wskaźnik pozycji** na ekranie głównym przełącza się automatycznie
  z kropek na cienki pasek postępu, gdy wpisów jest za dużo żeby
  zmieściły się jako osobne kropki (np. tydzień co godzinę = 168
  wpisów) – więc nic nie wyjeżdża poza ekran.
- **Wykres** radzi sobie z gęstymi danymi – linia i tak się rysuje,
  etykiety osi X są przerzedzane, a na granicach dni dostają datę
  zamiast godziny plus cienką pionową linię, żeby było widać gdzie
  kończy się jeden dzień.
- **Parser** wymusza kopiowanie Stringów przy odczycie pliku (zamiast
  polegać na klasycznym, "dzielonym" zachowaniu `String.substring()` z
  Javy tamtej epoki), żeby cały wczytany plik nie siedział w pamięci
  przez cały czas działania appki tylko dlatego, że trzymamy w
  pamięci małe fragmenty z niego wycięte.

Realistycznie: **tydzień co godzinę dla jednego, dwóch miast** (do
~350 wpisów) powinien być bez problemu. Jeśli chcesz wrzucić dane co
godzinę dla wielu miast na dłuższy okres (np. miesiąc x 10 miast =
kilka tysięcy wpisów), to już warto to najpierw przetestować na
telefonie – pamięć na W595 jest skromna i w pewnym momencie może
zabraknąć, zanim appka zdąży cokolwiek wyświetlić.

Przy gęstych danych zamiast przewijać strzałką pojedynczo, użyj `*`/`#`
żeby skakać całymi dniami (patrz sekcja "Sterowanie" wyżej).



- Automatyczne przeszukiwanie typowych katalogów (`C:/other/`,
  `E:/other/`) przy starcie, żeby nie trzeba było ręcznie klikać przez
  przeglądarkę plików za pierwszym razem.
- Odbiór pliku bezpośrednio przez OBEX z poziomu samej aplikacji
  (JSR-82) zamiast polegania na wbudowanym menedżerze Bluetooth
  telefonu – możliwe, ale wymaga sporo więcej kodu i telefon i tak
  musi zaakceptować połączenie.
- Wykres innej wielkości niż temperatura (np. wilgotność, ciśnienie) –
  łatwe do dodania, `ChartCanvas` już ma cały mechanizm skalowania osi,
  trzeba by tylko sparametryzować które pole rysuje.

