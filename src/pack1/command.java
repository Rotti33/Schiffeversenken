package pack1;

public enum Command {
    SIZE,
    SHIPS,
    DONE,
    READY,
    SHOT,
    ANSWER,
    PASS,
    SAVE,
    LOAD,
    OK;

    /**
     * Konvertiert einen String sicher in ein Command-Enum.
     * @param str Der extrahierte Befehl aus der Nachricht
     * @return Das passende Command oder null, falls unbekannt.
     */
    public static Command fromString(String str) {
        try {
            return Command.valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}