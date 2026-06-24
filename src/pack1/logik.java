package pack1;

public class Logik {
    //0 = Wasser, 1 = Schiff, 2 = Fehlschuss, 3 = Treffer
    private int[][] spielerfeld = new int[10][10];
    private int[][] gegnerfeld = new int[10][10];

    //Schiffs-Konfigurationen für das Platzieren
    private int[] schiffsLaengen = {5, 4, 3, 3, 2}; //Ein 5er, ein 4er, zwei 3er, ein 2er
    private int aktuellesSchiffIndex = 0; //Welches Schiff platziert der Spieler gerade?
    private boolean istHorizontal = true; //Richtung für das Platzieren

    private int spielerTreffer = 0;
    private int kiTreffer = 0;
    private final int MAX_TREFFER = 17; //Gesamtzahl, wenn man alle Schiffs-Felder zusammenzählt

    public Logik() {
        //Die Arrays starten automatisch komplett mit 0 (Wasser)
        //Methode setzt automatisch KI-Schiffe
    }

    //Ändert die Ausrichtung beim Drücken einer Taste/Button
    public void toggleRichtung() {
        istHorizontal = !istHorizontal;
    }

    public boolean getIstHorizontal() {
        return istHorizontal;
    }

    //Gibt die Länge des Schiffes zurück, das der Spieler setzen muss
    public int getAktuelleSchiffsLaenge() {
        if (aktuellesSchiffIndex < schiffsLaengen.length) {
            return schiffsLaengen[aktuellesSchiffIndex];
        }
        return 0; //Alle Schiffe platziert
    }

    //Prüft, ob der Platzierungsprozess für den Spieler abgeschlossen ist
    public boolean alleSchiffePlatziert() {
        return aktuellesSchiffIndex >= schiffsLaengen.length;
    }

    //Versucht, das aktuelle Schiff des Spielers auf dem Feld zu platzieren
    public boolean platziereSpielerSchiff(int row, int col) {
        if (alleSchiffePlatziert()) return false;

        int laenge = schiffsLaengen[aktuellesSchiffIndex];

        //1. Prüfen, ob das Schiff ins Spielfeld passt und Platz frei ist
        if (istHorizontal) {
            if (col + laenge > 10) return false; //Steht rechts über
            for (int i = 0; i < laenge; i++) {
                if (spielerfeld[row][col + i] != 0) return false; //Feld besetzt
            }
            //2. Schiff eintragen
            for (int i = 0; i < laenge; i++) {
                spielerfeld[row][col + i] = 1;
            }
        } else { //Vertikal
            if (row + laenge > 10) return false; //Steht unten über
            for (int i = 0; i < laenge; i++) {
                if (spielerfeld[row + i][col] != 0) return false; //Feld besetzt
            }
            //2. Schiff eintragen
            for (int i = 0; i < laenge; i++) {
                spielerfeld[row + i][col] = 1;
            }
        }

        //Nächstes Schiff aktivieren
        aktuellesSchiffIndex++;
        return true;
    }

    //Übernimmt das von der KI generierte Spielfeld (konvertiert 'S' in die Zahl 1)
    public void setGegnerFeld(char[][] generiertesFeld) {
       for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (generiertesFeld[x][y] == 'S') {
                    gegnerfeld[x][y] = 1; //Schiff
                } else {
                    gegnerfeld[x][y] = 0; //Wasser
                } 
            }
       }
    }       

    //Verarbeitet den Schuss des Spielers auf das KI-Feld (0 = bereits beschossen, 1 = Wasser, 2 = Treffer)
    public int schussAufGegner(int row, int col) {
        if (gegnerfeld[row][col] == 0) {
            gegnerfeld[row][col] = 2; //Fehlschuss markieren
            return 1; 
        } else if (gegnerfeld[row][col] == 1) {
            gegnerfeld[row][col] = 3; //Treffer markieren
            spielerTreffer++;
            return 2;
        }
        return 0; //Ungültig (bereits beschossen)
    }

    //Verarbeitet den Schuss der KI auf das Spieler-Feld (0 = bereits beschossen, 1 = Wasser, 2 = Treffer)
    public int schussAufSpieler(int row, int col) {
        if (spielerfeld[row][col] == 0) {
            spielerfeld[row][col] = 2; //Fehlschuss markieren
            return 1;
        } else if (spielerfeld[row][col] == 1) {
            spielerfeld[row][col] = 3; //Treffer markieren
            kiTreffer++;
            return 2;
        }
        return 0; //Ungültig (bereits beschossen)
    }

    //Prüft ob der Spieler alle gegnerischen Segmente getroffen hat
    public boolean sieg() {
        return spielerTreffer >= MAX_TREFFER;
    }

    //Prüft ob die KI alle Spieler-Segmente getroffen hat
    public boolean kisieg() {
        return kiTreffer >= MAX_TREFFER;
    }

    //Gibt den Zustand einer Zelle im Spielerfeld zurück (0=Wasser,1=Schiff,2=Fehlschuss,3=Treffer)
    public int getSpielerFeldZustand(int row, int col) {
        return spielerfeld[row][col];
    }

    // NEU hinzugefügt für zusätzliche Netzwerk- & Bot-Abfragen:
    public int getGegnerFeldZustand(int row, int col) {
        return gegnerfeld[row][col];
    }

    public void registriereNetzwerkTreffer() {
        spielerTreffer++;
    }

    // NEU hinzugefügt für KI-Erfassung im Netzwerk/Bot-Modus:
    public void registriereNetzwerkKiTreffer() {
        kiTreffer++;
    }

// NEU: Prüft von einer getroffenen Position aus, ob das gesamte Schiff zerstört ist
    public boolean pruefeObSchiffVersenkt(int[][] feld, int row, int col) {
        // Richtungen für die Suche: oben, unten, links, rechts
        int[][] richtungen = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        
        for (int[] dir : richtungen) {
            int r = row + dir[0];
            int c = col + dir[1];
            
            // Schleife läuft so lange entlang des getroffenen Schiffs, wie Teile existieren
            while (r >= 0 && r < 10 && c >= 0 && c < 10) {
                if (feld[r][c] == 1) {
                    return false; // Ein intaktes Schiffsegment (1) wurde gefunden -> Schiff noch am Leben!
                }
                if (feld[r][c] != 3 && feld[r][c] != 1) {
                    break; // Ende des Schiffes erreicht (Wasser oder Fehlschuss)
                }
                r += dir[0];
                c += dir[1];
            }
        }
        return true; // Keine intakten Teile mehr gefunden -> Schiff ist komplett versenkt!
    }

    // Hilfsmethoden, um die Prüfung von außen für das jeweilige Feld zu starten
    public boolean istGegnerSchiffVersenkt(int row, int col) {
        return pruefeObSchiffVersenkt(gegnerfeld, row, col);
    }

    public boolean istSpielerSchiffVersenkt(int row, int col) {
        return pruefeObSchiffVersenkt(spielerfeld, row, col);
    }

// NEU: Ermöglicht das Laden des Feldes aus der Speicherdatei
    public void ladeSpielfeldManuell(int[][] neuesSpielerFeld, int[][] neuesGegnerFeld, int sTreffer, int kTreffer) {
        this.spielerfeld = neuesSpielerFeld;
        this.gegnerfeld = neuesGegnerFeld;
        this.spielerTreffer = sTreffer;
        this.kiTreffer = kTreffer;
        this.aktuellesSchiffIndex = schiffsLaengen.length; // Setzphase direkt überspringen
    }
}