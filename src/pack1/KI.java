package pack1;

import java.util.Random;

public class KI {
    //enums - feste Variablen
    public enum Mode { SEARCH, TARGETING, DESTROYING }
    public enum ShotResult { WASSER, TREFFER, VERSENKT }

    public enum Richtung {
        NORTH(0, -1), SOUTH(0, 1), EAST(1, 0), WEST(-1, 0);
        public final int dx, dy;
        Richtung(int dx, int dy) { this.dx = dx; this.dy = dy; }
        public Richtung getGegenseite() {
            switch (this) {
                case NORTH: return SOUTH;
                case SOUTH: return NORTH;
                case EAST: return WEST;
                default: return EAST;
            }
        }
    }

    public static class Koordinaten {
        public int x, y;
        public Koordinaten(int x, int y) { this.x = x; this.y = y; }
    }

    private char[][] gegnerBoard = new char[10][10];
    private char[][] meinBoard = new char[10][10];

    private Mode aktuellerModus = Mode.SEARCH;
    private Koordinaten ersterTreffer = null;
    private Koordinaten letzterTreffer = null;
    private Koordinaten letzterSchuss = null;
    private Richtung aktuelleRichtung = null;
    private Random random = new Random();
    private int schwierigkeitsgrad = 1; //1 = leicht, 2 = mittel, 3 = schwer, für später vorbereitet

    public KI() {
        platziereAlleSchiffe();
    }

    //eigene schiffe plazieren mit 1 block abstand um die schiffe
    private void platziereAlleSchiffe() {
        int[] flotte = {5, 4, 3, 3, 2};
        for (int laenge : flotte) {
            boolean platziert = false;
            while (!platziert) {
                int x = random.nextInt(10);
                int y = random.nextInt(10);
                boolean horizontal = random.nextBoolean();
                if (checkPlatzierung(x, y, laenge, horizontal)) {
                    for (int i = 0; i < laenge; i++) {
                        int nx = horizontal ? x + i : x;
                        int ny = horizontal ? y : y + i;
                        meinBoard[nx][ny] = 'S';
                    }
                    platziert = true;
                }
            }
        }
    }

    private boolean checkPlatzierung(int x, int y, int laenge, boolean horizontal) {
        for (int i = 0; i < laenge; i++) {
            int nx = horizontal ? x + i : x;
            int ny = horizontal ? y : y + i;
            if (!isValid(nx, ny)) return false;
            for (int v = -1; v <= 1; v++) {
                for (int h = -1; h <= 1; h++) {
                    if (isValid(nx + v, ny + h) && meinBoard[nx + v][ny + h] == 'S') return false;
                }
            }
        }
        return true;
    }

    //schuss - logik
    public Koordinaten getNextShot() {
        if (aktuellerModus == Mode.SEARCH) {
            letzterSchuss = getSearchShot();
        } else if (aktuellerModus == Mode.TARGETING) {
            letzterSchuss = getTargetingShot();
        } else {
            letzterSchuss = getDestroyingShot();
        }
        return letzterSchuss;
    }

    private Koordinaten getSearchShot() {
        int x, y;
    //zurzeit noch manuell auswählen wie der Schwierigkeitsgrad gewünscht ist

    //Schwierigkeitsgrad mittel: 50% Schachbrett, 50% Zufall
    //boolean useCheckerboard = random.nextBoolean(); 

    //Schwierigkeitsgrad schwer: 80% Schachbrett, 20% Zufall
    //boolean useCheckerboard = random.nextInt(100) < 80;

    //Schwierigkeitsgrad leicht: 20% Schachbrett, 80% Zufall
    boolean useCheckerboard = random.nextInt(100) < 20;


    //auskommentiert bis die Schwierigkeitsgrad in der GUI eingebaut ist
    /*boolean useCheckerboard = false;
    //Zufallswert würfelnt, um den Schwierigkeitsgrad zu bestimmen
    int chance = random.nextInt(100);

    if (schwierigkeitsgrad == 1) { // Leicht: 20% Schachbrett
        useCheckerboard = chance < 20;
    } else if (schwierigkeitsgrad == 3) { // schwer: 80% Schachbrett
        useCheckerboard = chance < 80;
    } else { // Mittel: 50% Schachbrett
        useCheckerboard = chance < 50;
    }*/

        do {
            x = random.nextInt(10);
            y = random.nextInt(10);

            if (gegnerBoard[x][y] != '\u0000') continue;
            if (useCheckerboard && ((x + y) % 2 != 0)) continue;
            break;
        } while (true);

        return new Koordinaten(x, y);
    }

    private Koordinaten getTargetingShot() {
        for (Richtung d : Richtung.values()) {
            int nx = ersterTreffer.x + d.dx;
            int ny = ersterTreffer.y + d.dy;
            if (isValid(nx, ny) && gegnerBoard[nx][ny] == '\u0000') {
                aktuelleRichtung = d;
                return new Koordinaten(nx, ny);
            }
        }
        aktuellerModus = Mode.SEARCH;
        return getSearchShot();
    }

    private Koordinaten getDestroyingShot() {
        int nx = letzterTreffer.x + aktuelleRichtung.dx;
        int ny = letzterTreffer.y + aktuelleRichtung.dy;

        if (isValid(nx, ny) && gegnerBoard[nx][ny] == '\u0000') {
            return new Koordinaten(nx, ny);
        }

        Richtung gegenseite = aktuelleRichtung.getGegenseite();
        int ox = ersterTreffer.x + gegenseite.dx;
        int oy = ersterTreffer.y + gegenseite.dy;

        if (isValid(ox, oy) && gegnerBoard[ox][oy] == '\u0000') {
            aktuelleRichtung = gegenseite;
            letzterSchuss = ersterTreffer; //zurück zum ersten treffer -> andere richtung probieren
            return new Koordinaten(ox, oy);
        }

        aktuellerModus = Mode.SEARCH;
        ersterTreffer = null;
        letzterTreffer = null;
        aktuelleRichtung = null;
        return getSearchShot();
    }

    //update und rückmeldung
    public void update(ShotResult result) {
        if (result == ShotResult.WASSER) {
            gegnerBoard[letzterSchuss.x][letzterSchuss.y] = 'W';
            if (aktuellerModus == Mode.DESTROYING && aktuelleRichtung != null) {
                aktuelleRichtung = aktuelleRichtung.getGegenseite();
                letzterTreffer = ersterTreffer;
            }
        } else if (result == ShotResult.TREFFER) {
            gegnerBoard[letzterSchuss.x][letzterSchuss.y] = 'T';
            if (aktuellerModus == Mode.SEARCH) {
                aktuellerModus = Mode.TARGETING;
                ersterTreffer = letzterSchuss;
            } else {
                aktuellerModus = Mode.DESTROYING;
            }
            letzterTreffer = letzterSchuss;
        } else if (result == ShotResult.VERSENKT) {
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
            for (int x = 0; x < 10; x++) {
                for (int y = 0; y < 10; y++) {
                    if (gegnerBoard[x][y] == 'T') {
                        for (int v = -1; v <= 1; v++) {
                            for (int h = -1; h <= 1; h++) {
                                int nx = x + v;
                                int ny = y + h;
                                if (isValid(nx, ny) && gegnerBoard[nx][ny] == '\u0000') {
                                    gegnerBoard[nx][ny] = 'W'; //angrenzende felder sind wasser
                                }
                            }
                        }
                    }
                }
            }
            aktuellerModus = Mode.SEARCH;
            ersterTreffer = null;
            letzterTreffer = null;
            aktuelleRichtung = null;
        }
    }

    public ShotResult empfangeSchuss(int x, int y) {
        if (meinBoard[x][y] == 'S') {
            meinBoard[x][y] = 'X';
            return istVersenkt(x, y) ? ShotResult.VERSENKT : ShotResult.TREFFER;
        }
        return ShotResult.WASSER;
    }

    private boolean istVersenkt(int x, int y) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            while (isValid(nx, ny) && (meinBoard[nx][ny] == 'S' || meinBoard[nx][ny] == 'X')) {
                if (meinBoard[nx][ny] == 'S') return false;
                nx += d[0];
                ny += d[1];
            }
        }
        return true;
    }

    private boolean isValid(int x, int y) {
        return x >= 0 && x < 10 && y >= 0 && y < 10;
    }

    //get und set
    public void setFeldBeschossen(int x, int y) {
        if (isValid(x, y) && gegnerBoard[x][y] == '\u0000') {
            gegnerBoard[x][y] = 'W';
        }
    }
    public char[][] getMyBoard() {
        return meinBoard;
    }
    public void setSchwierigkeitsgrad(int stufe) {
        this.schwierigkeitsgrad = stufe;
    }
}
