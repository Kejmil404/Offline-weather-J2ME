/**
 * Prosty, statyczny motyw kolorystyczny - zeby nie przekazywac boolean
 * "dark" przez konstruktory wszystkich ekranow. Przelaczany z menu
 * ("Jasny/Ciemny motyw"), stan trzyma MIDlet w RecordStore.
 */
public final class AppTheme {

    private AppTheme() {
    }

    public static boolean dark = true;

    public static void toggle() {
        dark = !dark;
    }

    public static int bg() {
        return dark ? 0x000000 : 0xFFFFFF;
    }

    public static int textPrimary() {
        return dark ? 0xEEEEEE : 0x111111;
    }

    public static int textSecondary() {
        return dark ? 0xAAAAAA : 0x555555;
    }

    public static int textMuted() {
        return dark ? 0x777777 : 0x999999;
    }

    public static int divider() {
        return dark ? 0x2A2A2A : 0xDDDDDD;
    }

    public static int dotInactive() {
        return dark ? 0x444444 : 0xCCCCCC;
    }

    public static int errorColor() {
        return dark ? 0xFF6666 : 0xCC0000;
    }

    // akcenty sa te same w obu motywach - wystarczajaco kontrastowe na obu tlach
    public static int accentBlue() {
        return 0x4D8FE0;
    }

    public static int accentRed() {
        return 0xE05A4E;
    }
}
