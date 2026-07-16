#!/bin/bash
# Prosty build bez Anta/Antenny: javac -> preverify -> jar.
# Wystarczy ustawic WTK_HOME ponizej (albo jako zmienna srodowiskowa)
# i odpalic:   ./build.sh
#
# Sciezki bibliotek w WTK 2.5.2 (typowo, wzgledem WTK_HOME):
#   lib/cldcapi11.jar
#   lib/midpapi20.jar
#   lib/jsr75.jar        (albo "fileconnection.jar" - nazwa zalezy od
#                          wersji dystrybucji, sprawdz `find $WTK_HOME -iname "*jsr75*" -o -iname "*fileconn*"`)
#
# Narzedzie preverify siedzi w: bin/preverify

set -e

WTK_HOME="${WTK_HOME:-/home/$USER/wtk252}"

SRC_DIR="src"
BUILD_DIR="build/classes"
PREVERIFIED_DIR="build/preverified"
DIST_DIR="dist"
MIDLET_NAME="weather"

# Nowoczesne JDK (11+) nie wspiera juz -source/-target 1.3, wiec do
# kompilacji potrzebny jest JDK 8 (albo starszy).
JAVA8_HOME="${JAVA8_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}"
JAVAC="$JAVA8_HOME/bin/javac"
JAR_BIN="$JAVA8_HOME/bin/jar"

if [ ! -x "$JAVAC" ]; then
    echo "Nie znaleziono JDK 8 pod $JAVA8_HOME"
    echo "Zainstaluj: sudo apt install openjdk-8-jdk"
    echo "albo ustaw: export JAVA8_HOME=/sciezka/do/jdk8"
    exit 1
fi

if [ ! -d "$WTK_HOME" ]; then
    echo "Nie znaleziono WTK_HOME: $WTK_HOME"
    echo "Ustaw poprawna sciezke: export WTK_HOME=/sciezka/do/wtk252"
    exit 1
fi

CLDC_JAR="$WTK_HOME/lib/cldcapi11.jar"
MIDP_JAR="$WTK_HOME/lib/midpapi20.jar"

# JSR-75 (FileConnection) - nazwa pliku bywa rozna, szukamy automatycznie
JSR75_JAR=$(find "$WTK_HOME" -iname "*fileconn*.jar" -o -iname "*jsr75*.jar" 2>/dev/null | head -n 1)

if [ -z "$JSR75_JAR" ]; then
    echo "UWAGA: nie znalazlem jara JSR-75 (FileConnection) w $WTK_HOME"
    echo "Sprawdz recznie: find \"$WTK_HOME\" -iname '*.jar' | grep -i file"
    exit 1
fi

rm -rf build "$DIST_DIR"
mkdir -p "$BUILD_DIR" "$PREVERIFIED_DIR" "$DIST_DIR"

# JSR-256 (Mobile Sensor API, do auto-obrotu) - NIE jest czescia zwyklego
# Sun WTK. Najpierw probujemy realnego jara (gdybys mial Sony Ericsson
# SDK), a jak nie ma - kompilujemy wlasne minimalne stuby z
# sensor-stub-src/ (same sygnatury metod, wystarczajace zeby javac
# sprawdzil typy; w runtime na telefonie i tak uzywana jest prawdziwa
# implementacja z ROM-u urzadzenia, bo stuby nigdy nie trafiaja do
# finalnego weather.jar - tylko na bootclasspath przy kompilacji).
SENSOR_JAR=$(find "$WTK_HOME" "${SE_SDK_HOME:-/nonexistent}" -iname "*sensor*.jar" -o -iname "*jsr256*.jar" -o -iname "*jsr-256*.jar" 2>/dev/null | head -n 1)

if [ -z "$SENSOR_JAR" ]; then
    echo "Nie znalazlem prawdziwego jara JSR-256 - kompiluje wlasne stuby"
    echo "z sensor-stub-src/ (wystarczajace do kompilacji, w runtime"
    echo "uzyte zostana prawdziwe klasy z ROM-u telefonu)."

    STUB_BUILD_DIR="build/sensor-stub-classes"
    mkdir -p "$STUB_BUILD_DIR"
    # Bez -source/-target 1.3 celowo: te stuby nigdy nie traifaja na
    # telefon (nie sa preweryfikowane ani pakowane do weather.jar),
    # sluza tylko do typecheckingu przy kompilacji glownego kodu ponizej.
    # -source 1.3 bez towarzyszacego -bootclasspath gubi -classpath
    # przy tym konkretnym javacu, stad zwykla, nowoczesna kompilacja.
    "$JAVAC" \
        -classpath "$CLDC_JAR:$MIDP_JAR" \
        -d "$STUB_BUILD_DIR" \
        sensor-stub-src/javax/microedition/sensor/*.java

    SENSOR_JAR="build/sensor-stub.jar"
    (cd "$STUB_BUILD_DIR" && "$JAR_BIN" cf "../../$SENSOR_JAR" .)
fi

echo "Uzywam:"
echo "  CLDC: $CLDC_JAR"
echo "  MIDP: $MIDP_JAR"
echo "  JSR-75: $JSR75_JAR"
echo "  JSR-256: $SENSOR_JAR"

BOOTCLASSPATH="$CLDC_JAR:$MIDP_JAR:$JSR75_JAR:$SENSOR_JAR"

echo "== Kompilacja (javac) =="
# -source/-target 1.3: format bajtkodu ktory rozumie stary preverify.
# Nowoczesny javac (JDK 8/11) wciaz to potrafi.
"$JAVAC" -source 1.3 -target 1.3 \
    -bootclasspath "$BOOTCLASSPATH" \
    -d "$BUILD_DIR" \
    "$SRC_DIR"/*.java

echo "== Preweryfikacja (preverify) =="
"$WTK_HOME/bin/preverify" -classpath "$BOOTCLASSPATH" -d "$PREVERIFIED_DIR" "$BUILD_DIR"

echo "== Pakowanie JAR =="
cp MANIFEST.MF "$PREVERIFIED_DIR/../MANIFEST.MF" 2>/dev/null || cp MANIFEST.MF build/MANIFEST.MF
cd "$PREVERIFIED_DIR"
"$JAR_BIN" cfm "../../$DIST_DIR/$MIDLET_NAME.jar" ../MANIFEST.MF .
cd - > /dev/null

echo "== Kopiowanie JAD =="
cp weather.jad "$DIST_DIR/$MIDLET_NAME.jad"

JAR_SIZE=$(stat -c%s "$DIST_DIR/$MIDLET_NAME.jar" 2>/dev/null || stat -f%z "$DIST_DIR/$MIDLET_NAME.jar")
sed -i.bak "s/MIDlet-Jar-Size:.*/MIDlet-Jar-Size: $JAR_SIZE/" "$DIST_DIR/$MIDLET_NAME.jad"
rm -f "$DIST_DIR/$MIDLET_NAME.jad.bak"

echo ""
echo "Gotowe: $DIST_DIR/$MIDLET_NAME.jar oraz $DIST_DIR/$MIDLET_NAME.jad"
echo "(MIDlet-Jar-Size w JAD zostal automatycznie zaktualizowany)"
