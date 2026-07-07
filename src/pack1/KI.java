package pack1;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Künstliche Intelligenz für das Schiffe-versenken-Spiel.
 * 
 * Diese Klasse implementiert eine KI-gesteuerte Gegnerfigur mit drei verschiedenen Jagd-Modi:
 * - SEARCH: Suchphase mit zufälligen oder Schachbrett-Schüssen
 * - TARGETING: Zielerfassungsphase nach dem ersten Treffer
 * - DESTROYING: Verfolgungsphase zum Versenken eines Schiffes
 * 
 * Die KI verwaltet:
 * - Eigene Schiffsflotte auf meinBoard (versteckt vor dem Spieler)
 * - Gegnerfeld (gegnerBoard) zur Verfolgung von Schüssen und Treffern
 * - Schuss-Strategien je nach aktuellem Modus
 * - Versenkungserkennung und Umgebungs-Markierung
 * 
 * Board-Formate:
 * - meinBoard: 'S'=Schiff, 'X'=Treffer, ' '=leer
 * - gegnerBoard: ' '=nicht beschossen, 'W'=Wasser, 'T'=Treffer, 'V'=Versenkt
 * 
 * @author Lisa Renner, Rodrigo Malisi Sousa
 * @version 1.0
 * @see Spielzustand
 */
public class KI {
    
    /**
     * Hilfsklasse zur Darstellung von Koordinaten auf dem Spielfeld.
     * 
     * Wird von der KI zur Rückgabe von Schuss-Positionen verwendet.
     */
    public static class Koordinaten {
        /**
         * x-Koordinate (Spalte)
         */
        public int x;
        
        /**
         * y-Koordinate (Zeile)
         */
        public int y;
        
        /**
         * Konstruktor für Koordinaten.
         * 
         * @param x x-Koordinate (Spalte)
         * @param y y-Koordinate (Zeile)
         */
        public Koordinaten(int x, int y) { this.x = x; this.y = y; }
    }

    /**
     * Größe des quadratischen Spielfeldes (z.B. 10 für 10x10).
     */
    private int groesse = 10; 
    
    /**
     * Board mit den vom Gegner bekannten Schüssen und deren Ergebnissen.
     * Wird von der KI verwendet, um die Gegner-Schiffe zu verfolgen.
     */
    private char[][] gegnerBoard;
    
    /**
     * Board mit den eigenen (der KI) Schiffen.
     * Ist für den Gegner nicht sichtbar.
     */
    private char[][] meinBoard;

    /**
     * Aktueller Jagd-Modus der KI (SEARCH, TARGETING oder DESTROYING).
     */
    private Spielzustand.Mode aktuellerModus = Spielzustand.Mode.SEARCH;
    
    /**
     * Position des ersten Treffers im aktuellen Jagdmodus.
     * Wird bei Wechsel zu TARGETING gespeichert.
     */
    private Koordinaten ersterTreffer = null;
    
    /**
     * Position des letzten Treffers im DESTROYING-Modus.
     * Wird verwendet, um die Richtung fortzusetzen.
     */
    private Koordinaten letzterTreffer = null;
    
    /**
     * Position des letzten abgegebenen Schusses.
     */
    private Koordinaten letzterSchuss = null;
    
    /**
     * Aktuelle Richtung im DESTROYING-Modus.
     * Wird nach dem ersten Treffer in TARGETING ermittelt.
     */
    private Spielzustand.Richtung aktuelleRichtung = null;
    
    /**
     * Zufallsgenerator für Schuss-Positionen und Entscheidungen.
     */
    private Random random = new Random();
    
    /**
     * Schwierigkeitsgrad der KI (1=leicht, 2=mittel, 3=schwer).
     * Aktuell nicht vollständig implementiert.
     */
    private int schwierigkeitsgrad = 1; 

    /**
     * Standardkonstruktor mit Spielfeldgröße 10x10.
     */
    public KI() {
        this(10);
    }

    /**
     * Konstruktor mit benutzerdefinierten Spielfeldgrößen.
     * 
     * Initialisiert die KI-Boards und platziert alle Schiffe automatisch
     * basierend auf einer harmonischen Flottenverteilung (ca. 30% Feldabdeckung).
     * 
     * @param gewaehlteGroesse Die Größe des quadratischen Spielfeldes
     */
    public KI(int gewaehlteGroesse) {
        this.groesse = gewaehlteGroesse;
        this.gegnerBoard = new char[groesse][groesse];
        this.meinBoard = new char[groesse][groesse];
        platziereAlleSchiffe();
    }

    /**
     * Platziert alle Schiffe der KI automatisch auf dem Board.
     * 
     * Berechnet eine harmonische Schiffsverteilung (ca. 30% der Feldgröße)
     * und platziert alle Schiffe zufällig mit Abstand.
     * Nutzt Reflection, um Nachbarfelder zu prüfen und Überschneidungen zu vermeiden.
     */
    private void platziereAlleSchiffe() {
        int zielBelegteFelder = (int) Math.round((groesse * groesse) * 0.30);
        List<Integer> botFlotte = new ArrayList<>();
        
        int rest = zielBelegteFelder;
        
        // Die Schiffe werden harmonisch nach dem Verteilungsschlüssel aufgeteilt
        while (rest >= 14) {
            botFlotte.add(5);
            botFlotte.add(4);
            botFlotte.add(3);
            botFlotte.add(2);
            rest -= 14;
        }
        
        while (rest >= 2) {
            if (rest >= 4 && !botFlotte.contains(4)) {
                botFlotte.add(4); rest -= 4;
            } else if (rest >= 3) {
                botFlotte.add(3); rest -= 3;
            } else {
                botFlotte.add(2); rest -= 2;
            }
        }
        
        if (rest == 1 && !botFlotte.isEmpty()) {
            botFlotte.sort((a, b) -> a - b);
            for (int i = 0; i < botFlotte.size(); i++) {
                if (botFlotte.get(i) < 5) {
                    botFlotte.set(i, botFlotte.get(i) + 1);
                    rest = 0;
                    break;
                }
            }
            if (rest == 1) {
                botFlotte.add(2);
            }
        }
        
        botFlotte.sort((a, b) -> b - a);

        for (int laenge : botFlotte) {
            boolean platziert = false;
            int globaleVersuche = 0;
            while (!platziert && globaleVersuche < 1000) {
                int x = random.nextInt(groesse);
                int y = random.nextInt(groesse);
                boolean horizontal = random.nextBoolean();
                if (checkPlatzierung(x, y, laenge, horizontal)) {
                    for (int i = 0; i < laenge; i++) {
                        int nx = horizontal ? x + i : x;
                        int ny = horizontal ? y : y + i;
                        meinBoard[nx][ny] = 'S';
                    }
                    platziert = true;
                }
                globaleVersuche++;
            }
        }
    }

    /**
     * Prüft, ob ein Schiff an der Position gültig platziert werden kann.
     * 
     * Validiert:
     * - Grenzen des Spielfeldes
     * - Keine Überlappung mit anderen Schiffen
     * - Abstand von mindestens 1 Feld zu anderen Schiffen
     * 
     * @param x x-Startposition (Spalte)
     * @param y y-Startposition (Zeile)
     * @param laenge Länge des Schiffes
     * @param horizontal true für horizontale Ausrichtung; false für vertikal
     * @return true wenn Platzierung möglich; false sonst
     */
    private boolean checkPlatzierung(int x, int y, int laenge, boolean horizontal) {
        for (int i = 0; i < laenge; i++) {
            int nx = horizontal ? x + i : x;
            int ny = horizontal ? y : y + i;
            if (!isValid(nx, ny)) return false;
            
            // Scannt alle 8 Nachbarfelder um das Bot-Schiff segmentweise ab
            for (int v = -1; v <= 1; v++) {
                for (int h = -1; h <= 1; h++) {
                    if (isValid(nx + v, ny + h) && meinBoard[nx + v][ny + h] == 'S') {
                        return false; // ABBRUCH: Bot-Schiffe würden sich berühren
                    }
                }
            }
        }
        return true;
    }

    /**
     * Berechnet den nächsten Schuss basierend auf dem aktuellen Jagd-Modus.
     * 
     * Delegiert an:
     * - getSearchShot(): Im SEARCH-Modus
     * - getTargetingShot(): Im TARGETING-Modus
     * - getDestroyingShot(): Im DESTROYING-Modus
     * 
     * @return Koordinaten des nächsten Schusses
     */
    public Koordinaten getNextShot() {
        if (aktuellerModus == Spielzustand.Mode.SEARCH) {
            letzterSchuss = getSearchShot();
        } else if (aktuellerModus == Spielzustand.Mode.TARGETING) {
            letzterSchuss = getTargetingShot();
        } else {
            letzterSchuss = getDestroyingShot();
        }
        return letzterSchuss;
    }

    /**
     * Berechnet einen Schuss im SEARCH-Modus.
     * 
     * Wählt zufällig oder nach Schachbrett-Muster:
     * - 80%: Normaler Zufallsschuss auf unbeschossene Felder
     * - 20%: Schachbrett-Muster (nur Felder wo (x+y) % 2 == 0)
     * 
     * Das Schachbrett-Muster verbessert die Effizienz, da Schiffe
     * mindestens 2 Felder großwächsen sind.
     * 
     * @return Koordinaten des Schusses
     */
    private Koordinaten getSearchShot() {
        int x, y;
        boolean useCheckerboard = random.nextInt(100) < 20;

        do {
            x = random.nextInt(groesse);
            y = random.nextInt(groesse);

            if (gegnerBoard[x][y] != '\u0000') continue;
            if (useCheckerboard && ((x + y) % 2 != 0)) continue;
            break;
        } while (true);

        return new Koordinaten(x, y);
    }

    /**
     * Berechnet einen Schuss im TARGETING-Modus.
     * 
     * Probiert alle vier Nachbarfelder (N, S, O, W) des ersten Treffers.
     * Wechselt auf gefundene Richtung und geht in den DESTROYING-Modus.
     * Falls keine unbeschossenen Nachbarn vorhanden, zurück zu SEARCH.
     * 
     * @return Koordinaten des gezielten Schusses
     */
    private Koordinaten getTargetingShot() {
        for (Spielzustand.Richtung d : Spielzustand.Richtung.values()) {
            int nx = ersterTreffer.x + d.dx;
            int ny = ersterTreffer.y + d.dy;
            if (isValid(nx, ny) && gegnerBoard[nx][ny] == '\u0000') {
                aktuelleRichtung = d;
                return new Koordinaten(nx, ny);
            }
        }
        aktuellerModus = Spielzustand.Mode.SEARCH;
        return getSearchShot();
    }

    /**
     * Berechnet einen Schuss im DESTROYING-Modus.
     * 
     * Verfolgt das Schiff in der erkannten Richtung weiter.
     * Wenn Sackgasse: Kehrt zum ersten Treffer um und schießt in Gegenrichtung.
     * Wenn auch das nicht geht: Zurück zu SEARCH, Zustand zurücksetzen.
     * 
     * @return Koordinaten des verfolgenden Schusses
     */
    private Koordinaten getDestroyingShot() {
        int nx = letzterTreffer.x + aktuelleRichtung.dx;
        int ny = letzterTreffer.y + aktuelleRichtung.dy;

        if (isValid(nx, ny) && gegnerBoard[nx][ny] == '\u0000') {
            return new Koordinaten(nx, ny);
        }

        Spielzustand.Richtung gegenseite = aktuelleRichtung.getGegenseite();
        int ox = ersterTreffer.x + gegenseite.dx;
        int oy = ersterTreffer.y + gegenseite.dy;

        if (isValid(ox, oy) && gegnerBoard[ox][oy] == '\u0000') {
            aktuelleRichtung = gegenseite;
            letzterSchuss = ersterTreffer; 
            return new Koordinaten(ox, oy);
        }

        aktuellerModus = Spielzustand.Mode.SEARCH;
        ersterTreffer = null;
        letzterTreffer = null;
        aktuelleRichtung = null;
        return getSearchShot();
    }

    /**
     * Verarbeitet das Ergebnis des letzten Schusses.
     * 
     * Aktualisiert gegnerBoard und den Jagd-Modus:
     * - WASSER: Markiert Wasser, kehrt ggf. Richtung um
     * - TREFFER: Wechsel zu TARGETING oder DESTROYING
     * - VERSENKT: Markiert Schiff als versenkt (V), umgebendes Wasser, Modus zurücksetzen
     * 
     * @param result Das Ergebnis des Schusses (WASSER, TREFFER, VERSENKT)
     */
    public void update(Spielzustand.ShotResult result) {
        if (result == Spielzustand.ShotResult.WASSER) {
            gegnerBoard[letzterSchuss.x][letzterSchuss.y] = 'W'; 
            if (aktuellerModus == Spielzustand.Mode.DESTROYING && aktuelleRichtung != null) {
                aktuelleRichtung = aktuelleRichtung.getGegenseite();
                letzterTreffer = ersterTreffer;
            }
        } else if (result == Spielzustand.ShotResult.TREFFER) {
            gegnerBoard[letzterSchuss.x][letzterSchuss.y] = 'T'; 
            if (aktuellerModus == Spielzustand.Mode.SEARCH) {
                aktuellerModus = Spielzustand.Mode.TARGETING;
                ersterTreffer = letzterSchuss;
            } else {
                aktuellerModus = Spielzustand.Mode.DESTROYING;
            }
            letzterTreffer = letzterSchuss;
        } else if (result == Spielzustand.ShotResult.VERSENKT) {
            gegnerBoard[letzterSchuss.x][letzterSchuss.y] = 'T';
            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] d : dirs) {
                int nx = letzterSchuss.x;
                int ny = letzterSchuss.y;
                while (isValid(nx, ny) && gegnerBoard[nx][ny] == 'T') {
                    gegnerBoard[nx][ny] = 'V'; 
                    nx += d[0];
                    ny += d[1];
                }
            }
            for (int x = 0; x < groesse; x++) {
                for (int y = 0; y < groesse; y++) {
                    if (gegnerBoard[x][y] == 'T') {
                        for (int v = -1; v <= 1; v++) {
                            for (int h = -1; h <= 1; h++) {
                                int nx = x + v;
                                int ny = y + h;
                                if (isValid(nx, ny) && gegnerBoard[nx][ny] == '\u0000') {
                                    gegnerBoard[nx][ny] = 'W'; 
                                }
                            }
                        }
                    }
                }
            }
            aktuellerModus = Spielzustand.Mode.SEARCH;
            ersterTreffer = null;
            letzterTreffer = null;
            aktuelleRichtung = null;
        }
    }

    /**
     * Verarbeitet einen Schuss des Gegners auf ein KI-Schiff.
     * 
     * Prüft das eigene Board (meinBoard) auf Schiffe und markiert Treffer mit 'X'.
     * Prüft auch, ob ein Schiff durch den Treffer versenkt wurde.
     * 
     * @param x x-Koordinate (Spalte) des Schusses
     * @param y y-Koordinate (Zeile) des Schusses
     * @return WASSER wenn kein Treffer; TREFFER wenn Schiff getroffen; VERSENKT wenn Schiff versenkt
     */
    public Spielzustand.ShotResult empfangeSchuss(int x, int y) {
        if (meinBoard[x][y] == 'S') {
            meinBoard[x][y] = 'X';
            return istVersenkt(x, y) ? Spielzustand.ShotResult.VERSENKT : Spielzustand.ShotResult.TREFFER;
        }
        return Spielzustand.ShotResult.WASSER;
    }

    /**
     * Prüft, ob Koordinaten innerhalb des Spielfeldes liegen.
     * 
     * @param x x-Koordinate (Spalte)
     * @param y y-Koordinate (Zeile)
     * @return true wenn 0 <= x < groesse && 0 <= y < groesse; false sonst
     */
    private boolean isValid(int x, int y) {
        return x >= 0 && x < groesse && y >= 0 && y < groesse;
    }

    /**
     * Gibt das Board mit den KI-Schiffen zurück.
     * 
     * @return Das meinBoard (2D char-Array mit 'S' für Schiffe)
     */
    public char[][] getMyBoard() {
        return meinBoard;
    }

    /**
     * Gibt die Größe des Spielfeldes zurück.
     * 
     * @return Feldgröße (z.B. 10 für 10x10)
     */
    public int getGroesse() {
        return groesse;
    }

    /**
     * Prüft, ob ein Schiff an der Position komplett versenkt ist.
     * 
     * Durchsucht das eigene Board in alle vier Richtungen von der Position,
     * um alle Segmente des Schiffes zu finden und zu zählen.
     * Ein Schiff ist versenkt, wenn alle seine Segmente getroffen wurden ('X').
     * 
     * @param x x-Koordinate des getroffenen Segmentes
     * @param y y-Koordinate des getroffenen Segmentes
     * @return true wenn das Schiff komplett versenkt ist; false sonst
     */
    private boolean istVersenkt(int x, int y) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            while (isValid(nx, ny) && meinBoard[nx][ny] == 'X') {
                nx += d[0];
                ny += d[1];
            }
            if (isValid(nx, ny) && meinBoard[nx][ny] == 'S') {
                return false; 
            }
        }
        return true;
    }

    /**
     * Markiert ein Feld des Gegnerboards als Wasser, falls noch nicht beschossen.
     * 
     * Diese Methode wird verwendet, um das Gegnerboard zu aktualisieren,
     * wenn die KI bereits über ein Feld weiß, dass dort kein Schiff ist.
     * 
     * @param r Zeilenindex
     * @param c Spaltenindex
     */
    public void setFeldBeschossen(int r, int c) {
        if (r >= 0 && r < groesse && c >= 0 && c < groesse) {
            if (gegnerBoard[r][c] == '\u0000') {
                gegnerBoard[r][c] = 'W';
            }
        }
    }
}