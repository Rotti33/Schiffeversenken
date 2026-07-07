package pack1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

/**
 * Abstrakte Klasse für die Netzwerkkommunikation im Schiffe-versenken-Spiel.
 * 
 * Diese Klasse definiert das Protokoll zur Kommunikation zwischen zwei Spielern
 * über ein Netzwerk. Sie verarbeitet ein- und ausgehende Nachrichten in einem
 * definierten Format und leitet sie an abstrakte Methoden weiter, die von
 * konkreten Implementierungen überschrieben werden.
 * 
 * <p>Unterstützte Befehle: SIZE, SHIPS, DONE, READY, SHOT, ANSWER, PASS, SAVE, LOAD, OK</p>
 * 
 * @author Lisa Reenner, Rodrigo Malisi Sousa
 * @version 1.0
 * @see Command
 */
public abstract class Netzwerkprotokoll {

    /**
     * Reader für eingehende Netzwerk-Nachrichten.
     */
    private final BufferedReader netzwerkReader;
    
    /**
     * Writer für ausgehende Netzwerk-Nachrichten.
     */
    private final PrintWriter netzwerkWriter;

    /**
     * Konstruktor für die Netzwerkkommunikation.
     * 
     * @param netzwerkReader Reader zum Lesen von Nachrichten vom Netzwerk
     * @param netzwerkWriter Writer zum Senden von Nachrichten ins Netzwerk
     */
    public Netzwerkprotokoll(BufferedReader netzwerkReader, PrintWriter netzwerkWriter) {
        this.netzwerkReader = netzwerkReader;
        this.netzwerkWriter = netzwerkWriter;
    }

    /**
     * Sendet eine formatierte Nachricht an den Spielpartner.
     * 
     * Die Nachricht wird nach folgendem Schema aufgebaut:
     * Befehl (lowercase) gefolgt von Parametern, jeweils durch Leerzeichen getrennt.
     * Die Nachricht wird mit einem Zeilentrenner abgeschlossen.
     * 
     * @param befehl Der Protokollbefehl (z.B. "SHOT", "ANSWER")
     * @param parameter Variable Anzahl von Parametern für diesen Befehl
     */
    public void sendeNachricht(String befehl, Object... parameter) {
        StringBuilder sb = new StringBuilder(befehl.toLowerCase());
        for (Object param : parameter) {
            sb.append(" ").append(param); // Trennung durch Leerzeichen
        }
        netzwerkWriter.println(sb.toString()); // Nachricht endet mit Zeilentrenner
        netzwerkWriter.flush();
    }

    /**
     * Liest die nächste Nachricht vom Netzwerk und verarbeitet sie im Case-System.
     * 
     * Die Methode parst die eingehende Nachricht in Bestandteile auf und leitet sie
     * an die entsprechende abstrakte Methode weiter. Wenn die Verbindung geschlossen
     * wird (readLine liefert null), wird {@link #verarbeiteVerbindungGeschlossen()} aufgerufen.
     * 
     * @return true, wenn die Verbindung offen bleibt; false bei Beendigung
     * @throws IOException bei Kommunikationsfehlern mit dem Netzwerk
     */
    public boolean verarbeiteNaechsteNachricht() throws IOException {
        String zeile = netzwerkReader.readLine(); // Liest Nachricht bequem ein
        
        // Wenn die Verbindung geschlossen wurde, liefert readLine null
        if (zeile == null) {
            verarbeiteVerbindungGeschlossen();
            return false;
        }

        // Nachricht in Bestandteile zerlegen
        String[] teile = zeile.trim().split(" "); // Trennung mittels split
        if (teile.length == 0 || teile[0].isEmpty()) {
            return true; 
        }

        String befehlStr = teile[0];
        Command befehl = Command.fromString(befehlStr);

        if (befehl == null) {
            System.err.println("Unbekannter Befehl empfangen: " + befehlStr);
            return true; 
        }

        // Case-Verarbeitung der einzelnen Protokoll-Befehle
        switch (befehl) {
            case SIZE:
                if (teile.length == 3) {
                    int zeilen = Integer.parseInt(teile[1]);
                    int spalten = Integer.parseInt(teile[2]);
                    verarbeiteSpielfeldGroesse(zeilen, spalten);
                }
                break;

            case SHIPS:
                int[] schiffsLaengen = Arrays.stream(teile, 1, teile.length)
                                             .mapToInt(Integer::parseInt)
                                             .toArray();
                verarbeiteSchiffsFlotte(schiffsLaengen);
                break;

            case DONE:
                verarbeiteDone();
                break;

            case READY:
                verarbeiteReady();
                break;

            case SHOT:
                if (teile.length == 3) {
                    int zeileIndex = Integer.parseInt(teile[1]);
                    int spalteIndex = Integer.parseInt(teile[2]);
                    verarbeiteSchuss(zeileIndex, spalteIndex);
                }
                break;

            case ANSWER:
                if (teile.length == 2) {
                    int ergebnis = Integer.parseInt(teile[1]);
                    verarbeiteAntwort(ergebnis);
                }
                break;

            case PASS:
                verarbeitePass();
                break;

            case SAVE:
                if (teile.length == 2) {
                    long speicherId = Long.parseLong(teile[1]); // Wertebereich long
                    verarbeiteSpeichern(speicherId);
                }
                break;

            case LOAD:
                if (teile.length == 2) {
                    long ladeId = Long.parseLong(teile[1]); // Wertebereich long
                    verarbeiteLaden(ladeId);
                }
                break;

            case OK:
                verarbeiteOk();
                break;
        }

        return true;
    }

    // --- Abstrakte Methoden zur Weiterleitung an die Spiellogik ---
    
    /**
     * Wird aufgerufen, wenn die Größe des Spielfeldes übermittelt wird.
     * 
     * @param zeilen Anzahl der Zeilen des Spielfeldes
     * @param spalten Anzahl der Spalten des Spielfeldes
     */
    protected abstract void verarbeiteSpielfeldGroesse(int zeilen, int spalten);
    
    /**
     * Wird aufgerufen, wenn die Längen der gegnerischen Schiffsflotte empfangen werden.
     * 
     * @param laengen Array mit den Längen der Schiffe des Gegners
     */
    protected abstract void verarbeiteSchiffsFlotte(int[] laengen);
    
    /**
     * Wird aufgerufen, wenn der Gegner die Platzierung seiner Schiffe abgeschlossen hat.
     */
    protected abstract void verarbeiteDone();
    
    /**
     * Wird aufgerufen, wenn der Gegner bereit ist, das Spiel zu beginnen.
     */
    protected abstract void verarbeiteReady();
    
    /**
     * Wird aufgerufen, wenn der Gegner einen Schuss abfeuert.
     * 
     * @param zeile Zeilenindex des Schusses (0-basiert)
     * @param spalte Spaltenindex des Schusses (0-basiert)
     */
    protected abstract void verarbeiteSchuss(int zeile, int spalte);
    
    /**
     * Wird aufgerufen, wenn die Antwort auf einen eigenen Schuss empfangen wird.
     * 
     * @param ergebnis Ergebniscode: 0=Wasser, 1=Treffer, 2=Versenkt
     */
    protected abstract void verarbeiteAntwort(int ergebnis);
    
    /**
     * Wird aufgerufen, wenn der Gegner den Zug überspringt.
     */
    protected abstract void verarbeitePass();
    
    /**
     * Wird aufgerufen, wenn das Spiel gespeichert werden soll.
     * 
     * @param id Eindeutige ID für den Speicherplatz
     */
    protected abstract void verarbeiteSpeichern(long id);
    
    /**
     * Wird aufgerufen, wenn das Spiel geladen werden soll.
     * 
     * @param id Eindeutige ID des Speicherplatzes, der geladen werden soll
     */
    protected abstract void verarbeiteLaden(long id);
    
    /**
     * Wird aufgerufen, wenn eine Bestätigung (OK) vom Gegner empfangen wird.
     */
    protected abstract void verarbeiteOk();
    
    /**
     * Wird aufgerufen, wenn die Netzwerkverbindung geschlossen wird oder unterbrochen wird.
     */
    protected abstract void verarbeiteVerbindungGeschlossen();
}