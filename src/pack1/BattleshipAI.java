package pack1;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;

public class BattleshipAI {

    // --- ENUMS ---
    public enum Mode { SEARCH, TARGETING, DESTROYING }
    public enum ShotResult { WASSER, TREFFER, VERSENKT }
    
    public enum Richtung{
        NORTH(0, -1), SOUTH(0, 1), EAST(1, 0), WEST(-1, 0);
        public final int dx, dy;
        Richtung(int dx, int dy) { this.dx = dx; this.dy = dy; }
        public Richtung getOpposite() {
            switch(this) {
                case NORTH: return SOUTH;
                case SOUTH: return NORTH;
                case EAST: return WEST;
                default: return EAST;
            }
        }
    }

    // --- HILFSKLASSE ---
    public static class Koordinaten {
        public int x, y;
        public Koordinaten(int x, int y) { this.x = x; this.y = y; }
    }

    // --- VARIABLEN ---
    private char[][] enemyBoard = new char[10][10]; // Wissen über den Gegner
    private char[][] myBoard = new char[10][10];    // Eigene Schiffe
    
    private Mode currentMode = Mode.SEARCH;
    private Koordinaten firstHit = null;
    private Koordinaten lastHit = null;
    private Koordinaten lastShot = null;
    private Richtung currentDirection = null;
    private Random random = new Random();

    // --- KONSTRUKTOR ---
    public BattleshipAI() {
        platziereAlleSchiffe();
    }

    // --- EIGENE SCHIFFE PLATZIEREN ---
    //muss gemacht werden: schiffe sitzen zu dicht aufeinander -> muss 1 leeres feld um die schiffen geben
    private void platziereAlleSchiffe() {
        int[] flotte = {5, 4, 3, 3, 2}; // Schiffslängen
        for (int laenge : flotte) {
            boolean platziert = false;
            while (!platziert) {
                int x = random.nextInt(10);
                int y = random.nextInt(10);
                boolean horiz = random.nextBoolean();
                if (checkPlatzierung(x, y, laenge, horiz)) {
                    for (int i = 0; i < laenge; i++) {
                        int nx = horiz ? x + i : x;
                        int ny = horiz ? y : y + i;
                        myBoard[nx][ny] = 'S';
                    }
                    platziert = true;
                }
            }
        }
    }

    private boolean checkPlatzierung(int x, int y, int laenge, boolean horiz) {
        for (int i = 0; i < laenge; i++) {
            int nx = horiz ? x + i : x;
            int ny = horiz ? y : y + i;
            if (!isValid(nx, ny)) return false;
            for (int v = -1; v <= 1; v++) {
                for (int h = -1; h <= 1; h++) {
                    if (isValid(nx + v, ny + h) && myBoard[nx + v][ny + h] == 'S') return false;
                }
            }
        }
        return true;
    }

    // --- SCHUSS-LOGIK (KI-GEHIRN) ---
    public Koordinaten getNextShot() {
        if (currentMode == Mode.SEARCH) lastShot = getSearchShot();
        else if (currentMode == Mode.TARGETING) lastShot = getTargetingShot();
        else lastShot = getDestroyingShot();
        return lastShot;
    }

    private Koordinaten getSearchShot() {
    int x, y;
    // 50% Chance: true = Schachbrett, false = purer Zufall
    //Schwierigkeitsgrad mittel: 50% Schachbrett, 50% Zufall
    //boolean useCheckerboard = random.nextBoolean(); 

    //80% Chance: Schachbrett, 20% Chance: purer Zufall
    //Schwierigkeitsgrad schwer: 80% Schachbrett, 20% Zufall
    //boolean useCheckerboard = random.nextInt(100) < 80;

    //20% Chance: Schachbrett, 80% Chance: purer Zufall
    //Schwierigkeitsgrad leicht: 20% Schachbrett, 80% Zufall
    boolean useCheckerboard = random.nextInt(100) < 20;

    do {
        x = random.nextInt(10);
        y = random.nextInt(10);

        if (useCheckerboard) {
            // Schachbrett-Logik: Nur jedes zweite Feld
            if ((x + y) % 2 != 0 || enemyBoard[x][y] != '\u0000') {
                continue;
             }
        } else {
            // Random-Logik: Jedes freie Feld ist okay
            if (enemyBoard[x][y] != '\u0000') {
                continue;
            }
        }

        // Wenn wir hier landen, haben wir ein gültiges Feld gefunden
        break; 
    } while (true);

    return new Koordinaten(x, y);
}
    private Koordinaten getTargetingShot() {
        for (Richtung d : Richtung.values()) {
            int nx = firstHit.x + d.dx;
            int ny = firstHit.y + d.dy;
            if (isValid(nx, ny) && enemyBoard[nx][ny] == '\u0000') {
                currentDirection = d;
                return new Koordinaten(nx, ny);
            }
        }
        currentMode = Mode.SEARCH;
        return getSearchShot();
    }

    private Koordinaten getDestroyingShot() {
        int nx = lastHit.x + currentDirection.dx;
        int ny = lastHit.y + currentDirection.dy;


        if (isValid(nx, ny) && enemyBoard[nx][ny] == '\u0000') {
            return new Koordinaten(nx, ny);
        } 

        Richtung gegenseite = currentDirection.getOpposite();
        int ox = firstHit.x + gegenseite.dx;
        int oy = firstHit.y + gegenseite.dy;

        if(isValid(ox, oy) && enemyBoard[ox][oy] == '\u0000') {
            currentDirection = gegenseite;
            lastHit = firstHit; // Zurück zum ersten Treffer, um in die andere Richtung zu feuern
            return new Koordinaten(ox, oy);
        }
        currentMode = Mode.SEARCH;
        firstHit = null;
        lastHit = null;
        currentDirection = null;
        return getSearchShot();
    }

    // --- UPDATE & RÜCKMELDUNG ---
    public void update(ShotResult result) {
        if (result == ShotResult.WASSER) {
            enemyBoard[lastShot.x][lastShot.y] = 'W';
            if (currentMode == Mode.DESTROYING) {
                currentDirection = currentDirection.getOpposite();
                lastHit = firstHit;
            }
        } else if (result == ShotResult.TREFFER) {
            enemyBoard[lastShot.x][lastShot.y] = 'T';
            if (currentMode == Mode.SEARCH) {
                currentMode = Mode.TARGETING;
                firstHit = lastShot;
            } else {
                currentMode = Mode.DESTROYING;
            }
            lastHit = lastShot;
        } else if (result == ShotResult.VERSENKT) {
            enemyBoard[lastShot.x][lastShot.y] = 'T';
            
            for(int x = 0; x < 10; x++){
                for(int y = 0; y< 10; y++){
                    if(enemyBoard[x][y] == 'T') {
                        for(int v = -1; v <= 1; v++) {
                            for(int h = -1; h <= 1; h++) {
                                int nx = x + v, ny = y + h;
                                if(isValid(nx, ny) && enemyBoard[nx][ny] == '\u0000') {
                                    enemyBoard[nx][ny] = 'W'; // Markiere angrenzende Felder als Wasser
                                }
                            }
                        }
                    }
                }
            }
            currentMode = Mode.SEARCH;
            firstHit = null; lastHit = null; currentDirection = null;
        }
    }

    public ShotResult empfangeSchuss(int x, int y) {
        if (myBoard[x][y] == 'S') {
            myBoard[x][y] = 'X';
            return istVersenkt(x, y) ? ShotResult.VERSENKT : ShotResult.TREFFER;
        }
        return ShotResult.WASSER;
    }

    private boolean istVersenkt(int x, int y) {
        int[][] dirs = {{1,0}, {-1,0}, {0,1}, {0,-1}};
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            while (isValid(nx, ny) && (myBoard[nx][ny] == 'S' || myBoard[nx][ny] == 'X')) {
                if (myBoard[nx][ny] == 'S') return false;
                nx += d[0]; ny += d[1];
            }
        }
        return true;
    }

    private boolean isValid(int x, int y) {
        return x >= 0 && x < 10 && y >= 0 && y < 10;
    }

    // --- GETTER FÜR DIE KOLLEGEN ---
    public Mode getCurrentMode() { return currentMode; }
    public char getFeldStatus(int x, int y) { return enemyBoard[x][y]; }
    public char[][] getMyBoard() { return myBoard; }
}