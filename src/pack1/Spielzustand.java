package pack1;

public class Spielzustand {

    // Zentrales Enum für die Schussergebnisse (Wasser, Treffer, Versenkt)
    public enum ShotResult {
        WASSER, TREFFER, VERSENKT
    }

    // Zentrales Enum für die verschiedenen KI-Jagd-Modi
    public enum Mode {
        SEARCH, TARGETING, DESTROYING
    }

    // Zentrales Enum für die vier Himmelsrichtungen inklusive der mathematischen Vektoren
    public enum Richtung {
        NORTH(0, -1), SOUTH(0, 1), EAST(1, 0), WEST(-1, 0);
        
        public final int dx, dy;
        
        Richtung(int dx, int dy) { 
            this.dx = dx; 
            this.dy = dy; 
        }
        
        // Hilfsmethode, um im Jagdmodus bei einem Fehlschuss sofort die Kehrtwende zu machen
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