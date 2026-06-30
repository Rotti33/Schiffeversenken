package pack1;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class KI {
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
                case WEST: return EAST;
                default: return this;
            }
        }
    }

    public static class Koordinaten {
        public int x, y;
        public Koordinaten(int x, int y) { this.x = x; this.y = y; }
    }

    private int groesse = 10; 
    private char[][] gegnerBoard;
    private char[][] meinBoard;

    private Mode aktuellerModus = Mode.SEARCH;
    private Koordinaten ersterTreffer = null;
    private Koordinaten letzterTreffer = null;
    private Koordinaten letzterSchuss = null;
    private Richtung aktuelleRichtung = null;
    private Random random = new Random();
    private int schwierigkeitsgrad = 1; 

    public KI() {
        this(10);
    }

    public KI(int gewaehlteGroesse) {
        this.groesse = gewaehlteGroesse;
        this.gegnerBoard = new char[groesse][groesse];
        this.meinBoard = new char[groesse][groesse];
        platziereAlleSchiffe();
    }

    private void platziereAlleSchiffe() {
        int zielBelegteFelder = (int) Math.round((groesse * groesse) * 0.30);
        List<Integer> botFlotte = new ArrayList<>();
        
        int rest = zielBelegteFelder;
        
        // BEHOBEN: Exakt derselbe harmonische Verteilungs-Algorithmus wie in der Logik!
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
            letzterSchuss = ersterTreffer; 
            return new Koordinaten(ox, oy);
        }

        aktuellerModus = Mode.SEARCH;
        ersterTreffer = null;
        letzterTreffer = null;
        aktuelleRichtung = null;
        return getSearchShot();
    }

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

    private boolean isValid(int x, int y) {
        return x >= 0 && x < groesse && y >= 0 && y < groesse;
    }

    public char[][] getMyBoard() {
        return meinBoard;
    }

    public int getGroesse() {
        return groesse;
    }

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

    public void setFeldBeschossen(int r, int c) {
        if (r >= 0 && r < groesse && c >= 0 && c < groesse) {
            if (gegnerBoard[r][c] == '\u0000') {
                gegnerBoard[r][c] = 'W';
            }
        }
    }
}