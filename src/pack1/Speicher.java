package pack1;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;

/**
 * Verwaltet das Speichern und Laden von Spielständen für Schiffe-versenken.
 * 
 * Diese Klasse bietet statische Methoden zur Persistierung von Spielzuständen
 * in Dateien und zum Wiederherstellen dieser Zustände. Sie unterstützt sowohl
 * Singleplayer- als auch Bot-vs-Bot-Modi.
 * 
 * Das Speicherformat umfasst:
 * - Spielmodus (SINGLEPLAYER oder BOTVSBOT)
 * - Spielerfeld-Zustände
 * - Gegnerfeld-Zustände
 * - Trefferanzahlen für beide Seiten
 * 
 * @author Lisa Renner, Rodrigo Malisi Sousa
 * @version 1.0
 */
public class Speicher {

    /**
     * Speichert den aktuellen Zustand eines Singleplayer- oder Bot-Spiels in eine Datei.
     * 
     * Schreibt die Spiellogik in das folgende Format:
     * - Erste Zeile: Spielmodus (SINGLEPLAYER oder BOTVSBOT)
     * - Spielerfeld: Alle Zellenstaaten in einem NxN-Grid
     * - Gegnerfeld: Alle Zellenstaaten in einem NxN-Grid
     * 
     * Zellenstaaten:
     * - 0: Wasser (unberührt)
     * - 1: Schiff (unberührt)
     * - 2: Wasser (beschossen)
     * - 3: Schiff (getroffen)
     * - 4: Schiff (versenkt)
     * 
     * @param spiellogik Die Logik-Instanz mit dem aktuellen Spielzustand
     * @param spielmodus Der Spielmodus als String (z.B. "SINGLEPLAYER" oder "BOTVSBOT")
     * @param dateiname Der Pfad und Name der Datei, in die gespeichert wird
     */
    public static void speichereSpiel(Logik spiellogik, String spielmodus, String dateiname) {
        try {
            PrintWriter writer = new PrintWriter(dateiname);
            writer.println("MODUS: " + spielmodus);
            writer.println("Spielerfeld:");
            
            int N = spiellogik.getGroesse();
            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    writer.print(spiellogik.getSpielerFeldZustand(r, c) + " ");
                }
                writer.println();
            }
            
            writer.println("Gegnerfeld:");
            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    writer.print(spiellogik.getGegnerFeldZustand(r, c) + " ");
                }
                writer.println();
            }
            writer.close();
            System.out.println("Spielstand erfolgreich gespeichert in " + dateiname);
        } catch (Exception ex) {
            System.err.println("Fehler beim Speichern: " + ex.getMessage());
        }
    }

    /**
     * Lädt einen gespeicherten Spielstand aus einer Datei wieder.
     * 
     * Liest die Datei im Speicherformat und rekonstruiert die Spiellogik.
     * Die Feldgröße wird automatisch aus der ersten Zeile des Spielerfeldes ermittelt.
     * Trefferanzahlen werden gezählt und im übergebenen Array gespeichert.
     * 
     * Format der Eingabedatei (siehe {@link #speichereSpiel}):
     * - Erste Zeile: Spielmodus
     * - "Spielerfeld:" Header
     * - NxN Spielerfeld-Daten
     * - "Gegnerfeld:" Header
     * - NxN Gegnerfeld-Daten
     * 
     * Das trefferArray wird wie folgt gefüllt:
     * - trefferArray[0]: Anzahl der Spieler-Treffer (Wert 3 im Feld)
     * - trefferArray[1]: Anzahl der KI-Treffer (Wert 3 im Gegnerfeld)
     * - trefferArray[2]: Modus-Flag (1 für BOTVSBOT, 0 sonst)
     * 
     * @param dateiname Der Pfad und Name der zu ladenden Datei
     * @param trefferArray Ein int-Array der Größe >= 3 zur Speicherung der Trefferanzahlen
     * @return Eine neue Logik-Instanz mit den geladenen Daten, oder null bei Fehler
     */
    public static Logik ladeSpiel(String dateiname, int[] trefferArray) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(dateiname));
            String modusZeile = reader.readLine(); 
            //Modus wird hier gemerkt
            trefferArray[2] = modusZeile.contains("BOTVSBOT") ? 1 : 0;

            reader.readLine(); //"Spielerfeld:"
            
            int ermittelteGroesse = 0;
            reader.mark(10000);
            String testZeile = reader.readLine();
            if (testZeile != null && !testZeile.startsWith("Gegnerfeld:")) {
                ermittelteGroesse = testZeile.trim().split(" ").length;
            }
            reader.reset();

            int[][] geladenesSpielerFeld = new int[ermittelteGroesse][ermittelteGroesse];
            int[][] geladenesGegnerFeld = new int[ermittelteGroesse][ermittelteGroesse];
            int sTreffer = 0;
            int kTreffer = 0;

            for (int r = 0; r < ermittelteGroesse; r++) {
                String zeile = reader.readLine();
                String[] werte = zeile.trim().split(" ");
                for (int c = 0; c < ermittelteGroesse; c++) {
                    geladenesSpielerFeld[r][c] = Integer.parseInt(werte[c]);
                    if (geladenesSpielerFeld[r][c] == 3) kTreffer++; 
                }
            }

            reader.readLine(); //"Gegnerfeld:"
            for (int r = 0; r < ermittelteGroesse; r++) {
                String zeile = reader.readLine();
                String[] werte = zeile.trim().split(" ");
                for (int c = 0; c < ermittelteGroesse; c++) {
                    geladenesGegnerFeld[r][c] = Integer.parseInt(werte[c]);
                    if (geladenesGegnerFeld[r][c] == 3) sTreffer++; 
                }
            }
            reader.close();

            //Erstellt die Logik mit der ermittelten Größe und füllt sie mit den alten Treffern
            Logik geladeneLogik = new Logik(ermittelteGroesse);
            geladeneLogik.ladeSpielfeldManuell(geladenesSpielerFeld, geladenesGegnerFeld, sTreffer, kTreffer);
            
            //Trefferanzahl für die GUI im Array zwischenspeichern [Spielertreffer, KI-Treffer]
            trefferArray[0] = sTreffer;
            trefferArray[1] = kTreffer;
            
            return geladeneLogik;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}