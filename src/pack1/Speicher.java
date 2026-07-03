package pack1;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;

public class Speicher {

    //Speichert den aktuellen Zustand eines Singleplayer- oder Bot-Spiels
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

    //Lädt die Daten aus der Datei wieder
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