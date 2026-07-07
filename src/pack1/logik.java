package pack1;

import java.util.ArrayList;
import java.util.List;

/**
 * Zentrale Spiellogik für Schiffe-versenken.
 * 
 * Diese Klasse verwaltet den Spielzustand, einschließlich:
 * - Spieler- und Gegnerfeld (int-Arrays mit codierten Zellenzuständen)
 * - Schiffsplatzierung und -verfolgung
 * - Schussverarbeitung und Treffererfassung
 * - Schiff-Versenkungserkennung
 * - Harmonische Flottenverteilung (automatische Berechnung der Schiffslängen)
 * 
 * Zellenzustände:
 * - 0: Wasser (unberührt)
 * - 1: Schiff (unberührt)
 * - 2: Wasser (beschossen)
 * - 3: Schiff (getroffen/Treffer)
 * - 4: Schiff (versenkt)
 * 
 * @author Lisa Renner, Rodrigo Malisi Sousa
 * @version 1.0
 */
public class Logik {
    /**
     * Das Spielfeld des menschlichen Spielers.
     * 2D-Array mit codierten Zellenzuständen (0-4).
     */
    private int[][] spielerfeld;
    
    /**
     * Das Spielfeld des Gegners (KI oder Netzwerk-Gegner).
     * 2D-Array mit codierten Zellenzuständen (0-4).
     */
    private int[][] gegnerfeld;
    
    /**
     * Größe des quadratischen Spielfeldes (z.B. 10 für 10x10).
     */
    private int groesse = 10; 
    
    /**
     * Anzahl der Treffer, die der Spieler auf dem Gegnerfeld erzielt hat.
     */
    private int spielerTreffer = 0;
    
    /**
     * Anzahl der Treffer, die die KI/der Gegner auf dem Spielerfeld erzielt hat.
     */
    private int kiTreffer = 0;
    
    /**
     * Zielanzahl der belegten Felder (ca. 30% der Spielfeldfläche).
     * Wird verwendet, um den Spielsieg zu bestimmen.
     */
    private int zielBelegteFelder = 0;   
    
    /**
     * Aktuell belegte Felder durch platzierte Schiffe des Spielers.
     */
    private int aktuellBelegteFelder = 0; 
    
    /**
     * Flag: true für horizontale Schiffsausrichtung, false für vertikal.
     */
    private boolean istHorizontal = true;

    /**
     * Liste der noch aufzustellenden Schiffslängen.
     * Wird während der Schiffsplatzierung geleert.
     */
    private List<Integer> verbleibendeFlotte = new ArrayList<>();

    /**
     * Standardkonstruktor mit Spielfeldgröße 10x10.
     */
    public Logik() {
        this(10);
    }

    /**
     * Konstruktor mit benutzerdefinierten Spielfeldgrößen.
     * 
     * Initialisiert die Spielfelder und berechnet eine harmonische Flottenverteilung
     * basierend auf der angegebenen Größe (ca. 30% Feldabdeckung).
     * 
     * @param gewaehlteGroesse Die Größe des quadratischen Spielfeldes (z.B. 10 für 10x10)
     */
    public Logik(int gewaehlteGroesse) {
        this.groesse = gewaehlteGroesse;
        this.spielerfeld = new int[groesse][groesse];
        this.gegnerfeld = new int[groesse][groesse];
        
        // 30%-Ziel berechnen
        this.zielBelegteFelder = (int) Math.round((groesse * groesse) * 0.30);
        
        berechneSinnvolleFlotte();
    }

    /**
     * Berechnet eine harmonische Schiffsverteilung basierend auf Feldgröße.
     * 
     * Verteilt die Schiffe so, dass:
     * - Standard-Pakete (5er, 4er, 3er, 2er) wiederholt werden
     * - Der Rest sinnvoll aufgefüllt wird
     * - Kleinere Schiffe in der Überzahl sind
     * - Schiffe absteigend nach Länge sortiert werden (große zuerst)
     * 
     * Ziel: Ca. 30% der Spielfeldfläche mit Schiffen belegen.
     */
    private void berechneSinnvolleFlotte() {
        int rest = this.zielBelegteFelder;
        
        // Wir packen Schiffe in "Paketen" hinein, um ein perfektes Verhältnis zu erzielen:
        // Ein Standard-Set besteht aus: 1x 2er, 1x 3er, 1x 4er, 1x 5er (Summe = 14 Felder)
        // Wir loopen so lange, wie genug Platz für ein harmonisches Set ist
        while (rest >= 14) {
            verbleibendeFlotte.add(5);
            verbleibendeFlotte.add(4);
            verbleibendeFlotte.add(3);
            verbleibendeFlotte.add(2);
            rest -= 14;
        }
        
        // Den verbleibenden Rest füllen wir jetzt schrittweise von klein nach groß auf,
        // damit kleinere Schiffe IMMER in der Überzahl bleiben!
        while (rest >= 2) {
            if (rest >= 4 && !verbleibendeFlotte.contains(4)) {
                verbleibendeFlotte.add(4); rest -= 4;
            } else if (rest >= 3) {
                verbleibendeFlotte.add(3); rest -= 3;
            } else {
                verbleibendeFlotte.add(2); rest -= 2;
            }
        }
        
        // Falls durch Rundungen noch genau 1 Feld fehlt, erweitern wir das kleinste Schiff
        if (rest == 1 && !verbleibendeFlotte.isEmpty()) {
            verbleibendeFlotte.sort((a, b) -> a - b); // Kleinstes zuerst
            for (int i = 0; i < verbleibendeFlotte.size(); i++) {
                if (verbleibendeFlotte.get(i) < 5) {
                    verbleibendeFlotte.set(i, verbleibendeFlotte.get(i) + 1);
                    rest = 0;
                    break;
                }
            }
            // Fallback falls alles schon 5er sind
            if (rest == 1) {
                verbleibendeFlotte.add(2);
                this.zielBelegteFelder += 1;
            }
        }
        
        // Sortiert die Schiffe absteigend (Große Schiffe zuerst aufstellen)
        verbleibendeFlotte.sort((a, b) -> b - a);
    }

    /**
     * Gibt die aktuelle Größe des Spielfeldes zurück.
     * 
     * @return Feldgröße (z.B. 10 für 10x10)
     */
    public int getGroesse() {
        return this.groesse;
    }

    /**
     * Gibt die Zielanzahl der belegten Felder zurück.
     * 
     * @return Anzahl der Felder, die mit Schiffen belegt sein sollten
     */
    public int getZielBelegteFelder() {
        return this.zielBelegteFelder;
    }

    /**
     * Gibt einen formatierte Textbeschreibung der noch aufzustellenden Schiffe zurück.
     * 
     * Format beispielsweise: "Noch aufzustellen: 1x [5er]  1x [4er]  1x [3er]  2x [2er]"
     * 
     * @return Text mit Auflistung der verbleibenden Schiffe, oder Meldung wenn alle platziert
     */
    public String getFlottenText() {
        if (verbleibendeFlotte.isEmpty()) return "Keine Schiffe mehr aufzustellen!";
        
        int f5 = 0, f4 = 0, f3 = 0, f2 = 0;
        for (int l : verbleibendeFlotte) {
            if (l == 5) f5++;
            else if (l == 4) f4++;
            else if (l == 3) f3++;
            else if (l == 2) f2++;
        }
        
        StringBuilder sb = new StringBuilder("Noch aufzustellen: ");
        if (f5 > 0) sb.append(f5).append("x [5er]  ");
        if (f4 > 0) sb.append(f4).append("x [4er]  ");
        if (f3 > 0) sb.append(f3).append("x [3er]  ");
        if (f2 > 0) sb.append(f2).append("x [2er]  ");
        return sb.toString();
    }

    /**
     * Gibt die Länge des nächsten aufzustellenden Schiffes zurück.
     * 
     * @return Schiffslänge aus der verbleibenden Flotte, oder 0 wenn alle platziert
     */
    public int getAktuelleSchiffsLaenge() {
        if (!verbleibendeFlotte.isEmpty()) {
            return verbleibendeFlotte.get(0); 
        }
        return 0;
    }

    /**
     * Prüft, ob alle Schiffe platziert wurden.
     * 
     * @return true wenn die verbleibende Flotte leer ist; false sonst
     */
    public boolean alleSchiffePlatziert() {
        return verbleibendeFlotte.isEmpty();
    }

    /**
     * Platziert ein Schiff des Spielers auf dem Spielerfeld.
     * 
     * Das Schiff wird basierend auf der aktuellen Ausrichtung (horizontal/vertikal)
     * und der aktuellen Schiffslänge platziert. Nach erfolgreichem Platzieren wird
     * das Schiff aus der verbleibenden Flotte entfernt.
     * 
     * @param r Zeilenindex des Startpunktes (0-basiert)
     * @param c Spaltenindex des Startpunktes (0-basiert)
     * @return true wenn Platzierung erfolgreich; false bei ungültiger Position
     */
    public boolean platziereSpielerSchiff(int r, int c) {
        if (alleSchiffePlatziert()) return false;
        int laenge = getAktuelleSchiffsLaenge();

        // 1. VORAB-PRÜFUNG: Passt das Schiff überhaupt auf das Feld und hält es den 1-Block-Abstand ein?
        if (istHorizontal) {
            if (c + laenge > groesse) return false; // Schiff ragt über den rechten Rand
            
            // Schleife über jedes Segment des geplanten Schiffes
            for (int i = 0; i < laenge; i++) {
                int currentR = r;
                int currentC = c + i;
                
                // Prüft alle 8 umliegenden Felder (von -1 bis +1 in Reihe und Spalte)
                for (int v = -1; v <= 1; v++) {
                    for (int h = -1; h <= 1; h++) {
                        int checkR = currentR + v;
                        int checkC = currentC + h;
                        
                        // Wenn das Nachbarfeld innerhalb des Spielfelds liegt und bereits ein Schiff (1) enthält
                        if (checkR >= 0 && checkR < groesse && checkC >= 0 && checkC < groesse) {
                            if (spielerfeld[checkR][checkC] == 1) {
                                return false; // ABBRUCH: Ein anderes Schiff ist zu nah dran!
                            }
                        }
                    }
                }
            }
            
            // 2. PLATZIERUNG: Wenn der Abstand überall stimmt, wird das Schiff gebaut
            for (int i = 0; i < laenge; i++) {
                spielerfeld[r][c + i] = 1;
            }
        } else {
            if (r + laenge > groesse) return false; // Schiff ragt über den unteren Rand
            
            // Schleife über jedes Segment des geplanten Schiffes
            for (int i = 0; i < laenge; i++) {
                int currentR = r + i;
                int currentC = c;
                
                // Prüft alle 8 umliegenden Felder (von -1 bis +1 in Reihe und Spalte)
                for (int v = -1; v <= 1; v++) {
                    for (int h = -1; h <= 1; h++) {
                        int checkR = currentR + v;
                        int checkC = currentC + h;
                        
                        if (checkR >= 0 && checkR < groesse && checkC >= 0 && checkC < groesse) {
                            if (spielerfeld[checkR][checkC] == 1) {
                                return false; // ABBRUCH: Ein anderes Schiff ist zu nah dran!
                            }
                        }
                    }
                }
            }
            
            // 2. PLATZIERUNG
            for (int i = 0; i < laenge; i++) {
                spielerfeld[r + i][c] = 1;
            }
        }

        // Wenn das Schiff erfolgreich gesetzt wurde, aus der Flottenliste streichen
        this.aktuellBelegteFelder += laenge;
        verbleibendeFlotte.remove(0); 
        return true;
    }

    /**
     * Verarbeitet einen Schuss des Spielers auf das Gegnerfeld.
     * 
     * @param r Zeilenindex (0-basiert)
     * @param c Spaltenindex (0-basiert)
     * @return 0 = kein Treffer (bereits beschossen), 1 = Wasser, 2 = Treffer auf Schiff
     */
    public int schussAufGegner(int r, int c) {
        if (gegnerfeld[r][c] == 0) {
            gegnerfeld[r][c] = 2; 
            return 1;
        } else if (gegnerfeld[r][c] == 1) {
            gegnerfeld[r][c] = 3; 
            spielerTreffer++;
            return 2;
        }
        return 0;
    }

    /**
     * Verarbeitet einen Schuss der KI/des Gegners auf das Spielerfeld.
     * 
     * @param r Zeilenindex (0-basiert)
     * @param c Spaltenindex (0-basiert)
     * @return 0 = kein Treffer (bereits beschossen), 1 = Wasser, 2 = Treffer auf Schiff
     */
    public int schussAufSpieler(int r, int c) {
        if (spielerfeld[r][c] == 0) {
            spielerfeld[r][c] = 2; 
            return 1;
        } else if (spielerfeld[r][c] == 1) {
            spielerfeld[r][c] = 3; 
            kiTreffer++;
            return 2;
        }
        return 0;
    }

    /**
     * Gibt den Zellenzustand auf dem Spielerfeld zurück.
     * 
     * Zellenzustände: 0=Wasser, 1=Schiff, 2=Wasser(beschossen), 3=Treffer, 4=Versenkt
     * 
     * @param r Zeilenindex (0-basiert)
     * @param c Spaltenindex (0-basiert)
     * @return Zellenzustand (0-4)
     */
    public int getSpielerFeldZustand(int r, int c) {
        return spielerfeld[r][c];
    }

    /**
     * Gibt den Zellenzustand auf dem Gegnerfeld zurück.
     * 
     * Zellenzustände: 0=Wasser, 1=Schiff, 2=Wasser(beschossen), 3=Treffer, 4=Versenkt
     * 
     * @param r Zeilenindex (0-basiert)
     * @param c Spaltenindex (0-basiert)
     * @return Zellenzustand (0-4)
     */
    public int getGegnerFeldZustand(int r, int c) {
        return gegnerfeld[r][c];
    }

    /**
     * Prüft, ob der Spieler das Spiel gewonnen hat.
     * 
     * Der Spieler gewinnt, wenn er genug Treffer auf dem Gegnerfeld erzielt hat.
     * 
     * @return true wenn spielerTreffer >= zielBelegteFelder; false sonst
     */
    public boolean sieg() {
        return spielerTreffer >= zielBelegteFelder;
    }

    /**
     * Prüft, ob die KI/der Gegner das Spiel gewonnen hat.
     * 
     * Die KI gewinnt, wenn sie genug Treffer auf dem Spielerfeld erzielt hat.
     * 
     * @return true wenn kiTreffer >= zielBelegteFelder; false sonst
     */
    public boolean kisieg() {
        return kiTreffer >= zielBelegteFelder;
    }

    /**
     * Togglet die Schiffsausrichtung zwischen horizontal und vertikal.
     * 
     * @return true nach dem Umschalten (true=horizontal, false=vertikal)
     */
    public boolean toggleRichtung() {
        istHorizontal = !istHorizontal;
        return istHorizontal;
    }

    /**
     * Gibt die aktuelle Schiffsausrichtung zurück.
     * 
     * @return true wenn horizontal; false wenn vertikal
     */
    public boolean getIstHorizontal() {
        return istHorizontal;
    }

    /**
     * Prüft, ob ein Schiff an der gegebenen Position komplett versenkt ist.
     * 
     * Durchsucht das Feld in beide Richtungen von der Position,
     * um alle Segmente des Schiffes zu finden und zu zählen.
     * Ein Schiff ist versenkt, wenn alle seine Segmente getroffen wurden.
     * 
     * @param feld Das zu prüfende Spielfeld (Spieler- oder Gegnerfeld)
     * @param row Zeilenindex des getroffenen Schiffssegmentes
     * @param col Spaltenindex des getroffenen Schiffssegmentes
     * @return true wenn das Schiff komplett versenkt ist; false sonst
     */
    public boolean pruefeObSchiffVersenkt(int[][] feld, int row, int col) {
        if (feld[row][col] != 3) return false;

        boolean istHorizontal = false;
        boolean istVertikal = false;

        if ((col > 0 && (feld[row][col - 1] == 1 || feld[row][col - 1] == 3)) ||
            (col < (groesse - 1) && (feld[row][col + 1] == 1 || feld[row][col + 1] == 3))) {
            istHorizontal = true;
        }
        if ((row > 0 && (feld[row - 1][col] == 1 || feld[row - 1][col] == 3)) ||
            (row < (groesse - 1) && (feld[row + 1][col] == 1 || feld[row + 1][col] == 3))) {
            istVertikal = true;
        }

        if (!istHorizontal && !istVertikal) {
            return true; 
        }

        int gesamtSegmente = 1;
        int erlitteneTreffer = 1;

        int[][] scanRichtungen = istHorizontal ? new int[][]{{0, 1}, {0, -1}} : new int[][]{{1, 0}, {-1, 0}};

        for (int[] dir : scanRichtungen) {
            int r = row + dir[0];
            int c = col + dir[1];

            while (r >= 0 && r < groesse && c >= 0 && c < groesse) {
                if (feld[r][c] == 1) {
                    gesamtSegmente++;
                    r += dir[0];
                    c += dir[1];
                } else if (feld[r][c] == 3) {
                    gesamtSegmente++;
                    erlitteneTreffer++;
                    r += dir[0];
                    c += dir[1];
                } else {
                    break;
                }
            }
        }

        return erlitteneTreffer == gesamtSegmente;
    }

    /**
     * Prüft, ob ein Schiff des Gegners an der Position versenkt ist.
     * 
     * @param row Zeilenindex (0-basiert)
     * @param col Spaltenindex (0-basiert)
     * @return true wenn das Schiff versenkt ist; false sonst
     */
    public boolean istGegnerSchiffVersenkt(int row, int col) {
        return pruefeObSchiffVersenkt(gegnerfeld, row, col);
    }

    /**
     * Prüft, ob ein Schiff des Spielers an der Position versenkt ist.
     * 
     * @param row Zeilenindex (0-basiert)
     * @param col Spaltenindex (0-basiert)
     * @return true wenn das Schiff versenkt ist; false sonst
     */
    public boolean istSpielerSchiffVersenkt(int row, int col) {
        return pruefeObSchiffVersenkt(spielerfeld, row, col);
    }

    /**
     * Registriert einen Treffer in einem Netzwerkspiel für den Spieler.
     * Erhöht die Trefferanzahl des Spielers um 1.
     */
    public void registriereNetzwerkTreffer() {
        spielerTreffer++;
    }

    /**
     * Registriert einen Treffer in einem Netzwerkspiel für die KI/den Gegner.
     * Erhöht die Trefferanzahl des Gegners um 1.
     */
    public void registriereNetzwerkKiTreffer() {
        kiTreffer++;
    }

    /**
     * Setzt das Gegnerfeld basierend auf dem KI-Board.
     * 
     * Konvertiert das char-Array der KI ('S' für Schiff) in das int-Format
     * des Gegnerfeldes (1 für Schiff).
     * 
     * @param kiBoard Das char-Array der KI-Schiffe
     */
    public void setGegnerFeld(char[][] kiBoard) {
        for (int r = 0; r < groesse; r++) {
            for (int c = 0; c < groesse; c++) {
                if (r < kiBoard.length && c < kiBoard[r].length && kiBoard[r][c] == 'S') {
                    gegnerfeld[r][c] = 1;
                }
            }
        }
    }

    /**
     * Lädt ein gespeichertes Spiel manuell.
     * 
     * Überschreibt die aktuellen Spielfelder und Trefferanzahlen mit den
     * geladenen Daten aus einer Speicherdatei. Die verbleibende Flotte wird geleert.
     * 
     * @param neuesSpielerFeld Das zu ladende Spielerfeld
     * @param neuesGegnerFeld Das zu ladende Gegnerfeld
     * @param sTreffer Die zu ladende Trefferanzahl des Spielers
     * @param kTreffer Die zu ladende Trefferanzahl der KI
     */
    public void ladeSpielfeldManuell(int[][] neuesSpielerFeld, int[][] neuesGegnerFeld, int sTreffer, int kTreffer) {
        this.spielerfeld = neuesSpielerFeld;
        this.gegnerfeld = neuesGegnerFeld;
        this.spielerTreffer = sTreffer;
        this.kiTreffer = kTreffer;
        this.groesse = neuesSpielerFeld.length;
        this.zielBelegteFelder = (int) Math.round((groesse * groesse) * 0.30);
        this.verbleibendeFlotte.clear(); 
    }
}