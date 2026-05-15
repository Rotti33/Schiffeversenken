package pack1;

import java.util.Random;
//import java.util.ArrayList;
//import java.util.List;

public class BattleshipAI {

    // --- ENUMS ---
    public enum Mode { SEARCH, TARGETING, DESTROYING }
    public enum ShotResult { WASSER, TREFFER, VERSENKT }
    
    public enum Direction {
        NORTH(0, -1), SOUTH(0, 1), EAST(1, 0), WEST(-1, 0);
        public final int dx, dy;
        Direction(int dx, int dy) { this.dx = dx; this.dy = dy; }
        public Direction getOpposite() {
            switch(this) {
                case NORTH: return SOUTH;
                case SOUTH: return NORTH;
                case EAST: return WEST;
                default: return EAST;
            }
        }
    }

    // --- HILFSKLASSE ---
    public static class Coordinate {
        public int x, y;
        public Coordinate(int x, int y) { this.x = x; this.y = y; }
    }

    // --- VARIABLEN ---
    private char[][] enemyBoard = new char[10][10]; // Wissen über den Gegner
    private char[][] myBoard = new char[10][10];    // Eigene Schiffe
    
    private Mode currentMode = Mode.SEARCH;
    private Coordinate firstHit = null;
    private Coordinate lastHit = null;
    private Coordinate lastShot = null;
    private Direction currentDirection = null;
    private Random random = new Random();

    // --- KONSTRUKTOR ---
    public BattleshipAI() {
        platziereAlleSchiffe();
    }

    // --- EIGENE SCHIFFE PLATZIEREN ---
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
    public Coordinate getNextShot() {
        if (currentMode == Mode.SEARCH) lastShot = getSearchShot();
        else if (currentMode == Mode.TARGETING) lastShot = getTargetingShot();
        else lastShot = getDestroyingShot();
        return lastShot;
    }

    private Coordinate getSearchShot() {
        int x, y;
        do {
            x = random.nextInt(10);
            y = random.nextInt(10);
        } while ((x + y) % 2 != 0 || enemyBoard[x][y] != '\u0000');
        return new Coordinate(x, y);
    }

    private Coordinate getTargetingShot() {
        for (Direction d : Direction.values()) {
            int nx = firstHit.x + d.dx;
            int ny = firstHit.y + d.dy;
            if (isValid(nx, ny) && enemyBoard[nx][ny] == '\u0000') {
                currentDirection = d;
                return new Coordinate(nx, ny);
            }
        }
        currentMode = Mode.SEARCH;
        return getSearchShot();
    }

    private Coordinate getDestroyingShot() {
        int nx = lastHit.x + currentDirection.dx;
        int ny = lastHit.y + currentDirection.dy;
        if (isValid(nx, ny) && enemyBoard[nx][ny] == '\u0000') {
            return new Coordinate(nx, ny);
        } else {
            currentDirection = currentDirection.getOpposite();
            lastHit = firstHit;
            return getNextShot(); 
        }
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
}