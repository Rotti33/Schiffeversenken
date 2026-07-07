package pack1;

/**
 * Enumeration für alle Netzwerk-Protokoll-Befehle.
 * 
 * Diese Enum definiert alle möglichen Befehle, die zwischen zwei Spielern
 * über das Netzwerk ausgetauscht werden. Jeder Befehl wird als String in
 * Netzwerk-Nachrichten übertragen und mit dieser Enum klassifiziert.
 * 
 * Befehle werden case-insensitiv geparst und in Großbuchstaben konvertiert.
 * 
 * @author Lisa Renner, Rodrigo Malisi Sousa
 * @version 1.0
 * @see Netzwerkprotokoll
 */
public enum Command {
    /**
     * SIZE - Übermittelt die Spielfeldgröße.
     * Format: "size [zeilen] [spalten]"
     */
    SIZE,
    
    /**
     * SHIPS - Übermittelt die Längen der Schiffsflotte.
     * Format: "ships [länge1] [länge2] [länge3] ..."
     */
    SHIPS,
    
    /**
     * DONE - Bestätigt die Verarbeitung von Größe oder Flotte.
     * Format: "done"
     */
    DONE,
    
    /**
     * READY - Signalisiert, dass der Spieler bereit ist, das Spiel zu starten.
     * Format: "ready"
     */
    READY,
    
    /**
     * SHOT - Sendet eine Schussposition an den Gegner.
     * Format: "shot [zeile] [spalte]"
     */
    SHOT,
    
    /**
     * ANSWER - Sendet das Ergebnis eines empfangenen Schusses.
     * Format: "answer [ergebnis]" (0=Wasser, 1=Treffer, 2=Versenkt)
     */
    ANSWER,
    
    /**
     * PASS - Signalisiert, dass der aktuelle Spieler den Zug überspringt.
     * Format: "pass"
     */
    PASS,
    
    /**
     * SAVE - Fordert an, das Spiel zu speichern.
     * Format: "save [id]"
     */
    SAVE,
    
    /**
     * LOAD - Fordert an, ein Spiel zu laden.
     * Format: "load [id]"
     */
    LOAD,
    
    /**
     * OK - Allgemeine Bestätigung/Quittung für verschiedene Operationen.
     * Format: "ok"
     */
    OK;

    /**
     * Konvertiert einen String sicher in ein Command-Enum.
     * 
     * Der Input-String wird in Großbuchstaben konvertiert und mit den
     * Enum-Konstanten verglichen. Falls keine Übereinstimmung gefunden wird,
     * wird null zurückgegeben (kein Exception geworfen).
     * 
     * @param str Der extrahierte Befehl aus der Netzwerk-Nachricht (z.B. "shot", "READY")
     * @return Das passende Command-Enum, oder null bei unbekanntem Befehl
     */
    public static Command fromString(String str) {
        try {
            return Command.valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}