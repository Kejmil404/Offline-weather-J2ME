/**
 * Prosty callback - J2ME (CLDC 1.1) nie ma lambd ani interfejsow funkcyjnych
 * z domyslnymi metodami, wiec robimy to "po staremu".
 */
public interface FileSelectedListener {
    void onFileSelected(String fileUrl);
}
