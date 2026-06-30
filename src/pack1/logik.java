package pack1;

import java.util.ArrayList;
import java.util.List;

public class Logik {
    private int[][] spielerfeld;
    private int[][] gegnerfeld;
    
    private int groesse = 10; 
    
    private int spielerTreffer = 0;
    private int kiTreffer = 0;
    
    private int zielBelegteFelder = 0;   
    private int aktuellBelegteFelder = 0; 
    private boolean istHorizontal = true;

    private List<Integer> verbleibendeFlotte = new ArrayList<>();

    public Logik() {
        this(10);
    }

    public Logik(int gewaehlteGroesse) {
        this.groesse = gewaehlteGroesse;
        this.spielerfeld = new int[groesse][groesse];
        this.gegnerfeld = new int[groesse][groesse];
        
        // 30%-Ziel berechnen
        this.zielBelegteFelder = (int) Math.round((groesse * groesse) * 0.30);
        
        berechneSinnvolleFlotte();
    }

    // BEHOBEN FÜR SCHIFFSVERTEILUNG: Verteilt die Schiffe harmonisch (viele kleine, wenige große)
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

    public int getGroesse() {
        return this.groesse;
    }

    public int getZielBelegteFelder() {
        return this.zielBelegteFelder;
    }

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

    public int getAktuelleSchiffsLaenge() {
        if (!verbleibendeFlotte.isEmpty()) {
            return verbleibendeFlotte.get(0); 
        }
        return 0;
    }

    public boolean alleSchiffePlatziert() {
        return verbleibendeFlotte.isEmpty();
    }

    public boolean platziereSpielerSchiff(int r, int c) {
        if (alleSchiffePlatziert()) return false;
        int laenge = getAktuelleSchiffsLaenge();

        if (istHorizontal) {
            if (c + laenge > groesse) return false;
            for (int i = 0; i < laenge; i++) {
                if (spielerfeld[r][c + i] != 0) return false;
            }
            for (int i = 0; i < laenge; i++) {
                spielerfeld[r][c + i] = 1;
            }
        } else {
            if (r + laenge > groesse) return false;
            for (int i = 0; i < laenge; i++) {
                if (spielerfeld[r + i][c] != 0) return false;
            }
            for (int i = 0; i < laenge; i++) {
                spielerfeld[r + i][c] = 1;
            }
        }

        this.aktuellBelegteFelder += laenge;
        verbleibendeFlotte.remove(0); 
        return true;
    }

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

    public int getSpielerFeldZustand(int r, int c) {
        return spielerfeld[r][c];
    }

    public int getGegnerFeldZustand(int r, int c) {
        return gegnerfeld[r][c];
    }

    public boolean sieg() {
        return spielerTreffer >= zielBelegteFelder;
    }

    public boolean kisieg() {
        return kiTreffer >= zielBelegteFelder;
    }

    public boolean toggleRichtung() {
        istHorizontal = !istHorizontal;
        return istHorizontal;
    }

    public boolean getIstHorizontal() {
        return istHorizontal;
    }

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

    public boolean istGegnerSchiffVersenkt(int row, int col) {
        return pruefeObSchiffVersenkt(gegnerfeld, row, col);
    }

    public boolean istSpielerSchiffVersenkt(int row, int col) {
        return pruefeObSchiffVersenkt(spielerfeld, row, col);
    }

    public void registriereNetzwerkTreffer() {
        spielerTreffer++;
    }

    public void registriereNetzwerkKiTreffer() {
        kiTreffer++;
    }

    public void setGegnerFeld(char[][] kiBoard) {
        for (int r = 0; r < groesse; r++) {
            for (int c = 0; c < groesse; c++) {
                if (r < kiBoard.length && c < kiBoard[r].length && kiBoard[r][c] == 'S') {
                    gegnerfeld[r][c] = 1;
                }
            }
        }
    }

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