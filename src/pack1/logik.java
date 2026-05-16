package pack1;

public class logik {
    // 0 = Wasser, 1 = Schiff, 2 = Fehlschuss, 3 = Treffer
    private int[][] playerBoard = new int[10][10];
    private int[][] opponentBoard = new int[10][10];

    // Schiffs-Konfigurationen für das Platzieren
    private int[] schiffsLaengen = {5, 4, 3, 3, 2}; // Ein 5er, ein 4er, zwei 3er, ein 2er
    private int aktuellesSchiffIndex = 0; // Welches Schiff platziert der Spieler gerade?
    private boolean istHorizontal = true; // Richtung für das Platzieren

    private int spielerTreffer = 0;
    private int kiTreffer = 0;
    private final int MAX_TREFFER = 17;

    public logik() {
        // Die Arrays starten automatisch komplett mit 0 (Wasser).
        // Wir rufen hier eine Methode auf, die die KI-Schiffe automatisch zufällig versteckt.
        generiereKISchiffeZufaellig();
    }

    // Ändert die Ausrichtung beim Drücken einer Taste/Button
    public void toggleRichtung() {
        istHorizontal = !istHorizontal;
    }

    public boolean getIstHorizontal() {
        return istHorizontal;
    }

    // Gibt die Länge des Schiffes zurück, das der Spieler GERADE setzen muss
    public int getAktuelleSchiffsLaenge() {
        if (aktuellesSchiffIndex < schiffsLaengen.length) {
            return schiffsLaengen[aktuellesSchiffIndex];
        }
        return 0; // Alle Schiffe platziert
    }

    public boolean alleSchiffePlatziert() {
        return aktuellesSchiffIndex >= schiffsLaengen.length;
    }

    /**
     * Versucht, das aktuelle Schiff des Spielers auf dem Feld zu platzieren.
     * @return true, wenn das Platzieren erfolgreich war.
     */
    public boolean platziereSpielerSchiff(int row, int col) {
        if (alleSchiffePlatziert()) return false;

        int laenge = schiffsLaengen[aktuellesSchiffIndex];

        // 1. Prüfen, ob das Schiff ins Spielfeld passt und Platz frei ist
        if (istHorizontal) {
            if (col + laenge > 10) return false; // Steht rechts über
            for (int i = 0; i < laenge; i++) {
                if (playerBoard[row][col + i] != 0) return false; // Feld besetzt
            }
            // 2. Schiff eintragen
            for (int i = 0; i < laenge; i++) {
                playerBoard[row][col + i] = 1;
            }
        } else { // Vertikal
            if (row + laenge > 10) return false; // Steht unten über
            for (int i = 0; i < laenge; i++) {
                if (playerBoard[row + i][col] != 0) return false; // Feld besetzt
            }
            // 2. Schiff eintragen
            for (int i = 0; i < laenge; i++) {
                playerBoard[row + i][col] = 1;
            }
        }

        // Nächstes Schiff aktivieren
        aktuellesSchiffIndex++;
        return true;
    }

    public void setOpponentBoard(char[][] generiertesBoard) {
       for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (generiertesBoard[x][y] == 'S') {
                    opponentBoard[x][y] = 1; // Schiff
                } else {
                    opponentBoard[x][y] = 0; // Wasser
                } 
            }
       }
    }       

    // Hilfsmethode: Platziert die KI-Schiffe vollautomatisch und zufällig
    /*private void generiereKISchiffeZufaellig() {
        java.util.Random rand = new java.util.Random();
        for (int laenge : schiffsLaengen) {
            boolean erfolgreich = false;
            while (!erfolgreich) {
                int r = rand.nextInt(10);
                int c = rand.nextInt(10);
                boolean horiz = rand.nextBoolean();

                if (horiz) {
                    if (c + laenge <= 10) {
                        boolean frei = true;
                        for (int i = 0; i < laenge; i++) {
                            if (opponentBoard[r][c + i] != 0) frei = false;
                        }
                        if (frei) {
                            for (int i = 0; i < laenge; i++) opponentBoard[r][c + i] = 1;
                            erfolgreich = true;
                        }
                    }
                } else {
                    if (r + laenge <= 10) {
                        boolean frei = true;
                        for (int i = 0; i < laenge; i++) {
                            if (opponentBoard[r + i][c] != 0) frei = false;
                        }
                        if (frei) {
                            for (int i = 0; i < laenge; i++) opponentBoard[r + i][c] = 1;
                            erfolgreich = true;
                        }
                    }
                }
            }
        }
    }*/

    public int shootAtOpponent(int row, int col) {
        if (opponentBoard[row][col] == 0) {
            opponentBoard[row][col] = 2;
            return 1; 
        } else if (opponentBoard[row][col] == 1) {
            opponentBoard[row][col] = 3;
            spielerTreffer++;
            return 2;
        }
        return 0;
    }

    public int shootAtPlayer(int row, int col) {
        if (playerBoard[row][col] == 0) {
            playerBoard[row][col] = 2;
            return 1;
        } else if (playerBoard[row][col] == 1) {
            playerBoard[row][col] = 3;
            kiTreffer++;
            return 2;
        }
        return 0;
    }

// NEU: Prüft, ob der Spieler alle gegnerischen Segmente getroffen hat
    public boolean hatSpielerGewonnen() {
        return spielerTreffer >= MAX_TREFFER;
    }

    // NEU: Prüft, ob die KI alle Spieler-Segmente getroffen hat
    public boolean hatKiGewonnen() {
        return kiTreffer >= MAX_TREFFER;
    }
}

