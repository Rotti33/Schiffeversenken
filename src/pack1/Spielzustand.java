package pack1;

/**
 * Zentrale Klasse für Spielzustands-Enums und Konstanten.
 * 
 * Diese Klasse definiert die wichtigsten Enumerationen für das Schiffe-versenken-Spiel:
 * - Schussergebnisse (ShotResult)
 * - KI-Jagd-Modi (Mode)
 * - Richtungsvektoren (Richtung)
 * 
 * Alle Enums sind statisch und dienen als zentrale Referenzen
 * für die KI, das Netzwerkprotokoll und die Spiellogik.
 * 
 * @author Lisa Renner, Rodrigo Malisi Sousa
 * @version 1.0
 */
public class Spielzustand {

    /**
     * Enum für die möglichen Ergebnisse eines Schusses.
     * 
     * Verwendet von der KI und dem Netzwerkprotokoll zur Klassifizierung
     * von Schussergebnissen:
     * - WASSER: Schuss traf kein Schiff
     * - TREFFER: Schuss traf ein Schiff, das Schiff wurde aber nicht versenkt
     * - VERSENKT: Schuss traf das letzte verbleibende Feld eines Schiffes
     */
    public enum ShotResult {
        WASSER, TREFFER, VERSENKT
    }

    /**
     * Enum für die verschiedenen KI-Jagd-Modi.
     * 
     * Definiert die Verhaltensmodi der KI während des Spiels:
     * - SEARCH: Suchphase - KI schießt zufällig oder nach Schachbrett-Muster
     * - TARGETING: Zielerfassungsphase - KI hat einen Treffer gefunden und sucht die Richtung
     * - DESTROYING: Zerstörungsphase - KI hat die Schiff-Richtung gefunden und verfolgt das Schiff
     */
    public enum Mode {
        SEARCH, TARGETING, DESTROYING
    }

    /**
     * Enum für die vier Himmelsrichtungen mit Richtungsvektoren.
     * 
     * Definiert die möglichen Schießrichtungen der KI im Jagdmodus.
     * Jede Richtung hat ein Richtungspaar (dx, dy):
     * - NORTH: Norden (0, -1)
     * - SOUTH: Süden (0, +1)
     * - EAST: Osten (+1, 0)
     * - WEST: Westen (-1, 0)
     * 
     * Diese Vektoren werden verwendet, um Nachbarfelder zu berechnen
     * und die Schiffe während der Jagd zu verfolgen.
     */
    public enum Richtung {
        /**
         * Norden: Nach oben (y-Koordinate verringert sich)
         */
        NORTH(0, -1), 
        
        /**
         * Süden: Nach unten (y-Koordinate erhöht sich)
         */
        SOUTH(0, 1), 
        
        /**
         * Osten: Nach rechts (x-Koordinate erhöht sich)
         */
        EAST(1, 0), 
        
        /**
         * Westen: Nach links (x-Koordinate verringert sich)
         */
        WEST(-1, 0);
        
        /**
         * x-Komponente des Richtungsvektors
         */
        public final int dx;
        
        /**
         * y-Komponente des Richtungsvektors
         */
        public final int dy;
        
        /**
         * Konstruktor für die Richtungs-Enums.
         * 
         * @param dx x-Versatz der Richtung
         * @param dy y-Versatz der Richtung
         */
        Richtung(int dx, int dy) { 
            this.dx = dx; 
            this.dy = dy; 
        }
        
        /**
         * Liefert die Gegenrichtung.
         * 
         * Wird im Jagdmodus verwendet: Wenn die KI bei einem Schuss auf Wasser trifft,
         * kehrt sie sofort zur letzten Trefferposition um und schießt in die
         * entgegengesetzte Richtung.
         * 
         * @return Die Gegenrichtung (z.B. SOUTH für NORTH)
         */
        public Richtung getGegenseite() {
            switch (this) {
                case NORTH: return SOUTH;
                case SOUTH: return NORTH;
                case EAST: return WEST;
                case WEST: return EAST;
                default: return this;
            }
        }
    }
}