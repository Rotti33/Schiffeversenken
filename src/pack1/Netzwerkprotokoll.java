package pack1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

public abstract class Netzwerkprotokoll {

    private final BufferedReader netzwerkReader;
    private final PrintWriter netzwerkWriter;

    public Netzwerkprotokoll(BufferedReader netzwerkReader, PrintWriter netzwerkWriter) {
        this.netzwerkReader = netzwerkReader;
        this.netzwerkWriter = netzwerkWriter;
    }

    /**
     * Sendet eine formatierte Nachricht an den Spielpartner.
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
     * Liest die nächste Nachricht und verarbeitet sie im Case-System.
     * @return true, wenn die Verbindung offen bleibt; false bei Beendigung.
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
    protected abstract void verarbeiteSpielfeldGroesse(int zeilen, int spalten);
    protected abstract void verarbeiteSchiffsFlotte(int[] laengen);
    protected abstract void verarbeiteDone();
    protected abstract void verarbeiteReady();
    protected abstract void verarbeiteSchuss(int zeile, int spalte);
    protected abstract void verarbeiteAntwort(int ergebnis);
    protected abstract void verarbeitePass();
    protected abstract void verarbeiteSpeichern(long id);
    protected abstract void verarbeiteLaden(long id);
    protected abstract void verarbeiteOk();
    protected abstract void verarbeiteVerbindungGeschlossen();
}