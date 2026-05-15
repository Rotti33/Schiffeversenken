package pack1;

import javax.swing.*;

import pack1.BattleshipAI.Koordinaten;
import pack1.BattleshipAI.ShotResult;

import java.awt.*;

public class GUI extends JFrame {
    private feld playerField;
    private feld opponentField;
    private JLabel statusLabel;
    private JButton rotationButton; // Button zum Drehen der Schiffe
    private BattleshipAI ki;
    
    private logik spiellogik;

    public GUI() {
        spiellogik = new logik();

        setTitle("Schiffe versenken - Battleship");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // 1. Statuszeile oben aktualisieren
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        add(statusLabel, BorderLayout.NORTH);
        updateStatusText(); // Initialisiert den Text für das erste Schiff

        // 2. Container für die zwei Spielfelder
        JPanel fieldContainer = new JPanel(new GridLayout(1, 2, 30, 0));
        fieldContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // In der Setzphase ist DEIN Feld klickbar
        playerField = new feld("Dein Spielfeld", true, e -> {
            Point koordinaten = (Point) e.getSource();
            handleShipPlacement(koordinaten.x, koordinaten.y);
        });
        
        // Das gegnerische Feld nimmt Klicks an, wertet sie aber erst in handleAttack nach der Setzphase aus
        opponentField = new feld("Gegnerisches Feld (KI)", true, e -> {
            Point koordinaten = (Point) e.getSource();
            handleAttack(koordinaten.x, koordinaten.y);
        });

        fieldContainer.add(playerField);
        fieldContainer.add(opponentField);
        add(fieldContainer, BorderLayout.CENTER);

        // 3. Steuerungs-Panel unten (Süden) für die Ausrichtung
        JPanel southPanel = new JPanel(new FlowLayout());
        rotationButton = new JButton("Ausrichtung: HORIZONTAL");
        rotationButton.setFont(new Font("Arial", Font.PLAIN, 12));
        rotationButton.addActionListener(e -> {
            spiellogik.toggleRichtung();
            if (spiellogik.getIstHorizontal()) {
                rotationButton.setText("Ausrichtung: HORIZONTAL");
            } else {
                rotationButton.setText("Ausrichtung: VERTIKAL");
            }
        });
        southPanel.add(rotationButton);
        add(southPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public GUI(BattleshipAI ki) {
        this();
        this.ki = ki;
    }

    // Hilfsmethode, um den Hinweistext oben anzupassen
    private void updateStatusText() {
        if (!spiellogik.alleSchiffePlatziert()) {
            statusLabel.setText("Setzphase: Platziere ein Schiff der Länge " + spiellogik.getAktuelleSchiffsLaenge());
        } else {
            statusLabel.setText("Kampfphase! Klicke auf das rechte Feld, um die KI anzugreifen.");
        }
    }

    // Verarbeitet die Klicks beim Aufstellen der Schiffe
    private void handleShipPlacement(int r, int c) {
        int laenge = spiellogik.getAktuelleSchiffsLaenge();
        boolean horizontal = spiellogik.getIstHorizontal();

        // Versuche, das Schiff in der Logik einzutragen
        if (spiellogik.platziereSpielerSchiff(r, c)) {
            // Wenn erfolgreich, zeichne das Schiff auf deinem GUI-Feld (Grau)
            for (int i = 0; i < laenge; i++) {
                if (horizontal) {
                    playerField.setZellenFarbe(r, c + i, Color.GRAY);
                } else {
                    playerField.setZellenFarbe(r + i, c, Color.GRAY);
                }
            }

            // Prüfen, ob jetzt alle Schiffe bereitstehen
            if (spiellogik.alleSchiffePlatziert()) {
                starteKampfphase();
            } else {
                updateStatusText();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Ungültige Position! Schiff passt dort nicht hin.", "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Schaltet die Steuerung von Aufstellen auf Kämpfen um
    private void starteKampfphase() {
        updateStatusText();
        rotationButton.setEnabled(false); // Ausrichtungs-Button deaktivieren
    }

    // Verarbeitet die Klicks beim Angreifen (Rechtes Feld)
    private void handleAttack(int r, int c) {
        // Angriffe blockieren, solange Schiffe noch gesetzt werden
        if (!spiellogik.alleSchiffePlatziert()) return;
        
        // Abbrechen, wenn das Spiel schon vorbei ist
        if (spiellogik.hatSpielerGewonnen() || spiellogik.hatKiGewonnen()) return;

        int result = spiellogik.shootAtOpponent(r, c);

        if (result == 1) { 
            opponentField.setZellenFarbe(r, c, Color.LIGHT_GRAY); 
            statusLabel.setText("Fehlschuss auf Reihe " + (r + 1) + ", Spalte " + (c + 1));
            
            if (checkGameEnd()) return; 
            
            executeKiTurn();
        } else if (result == 2) { 
            opponentField.setZellenFarbe(r, c, Color.RED); 
            statusLabel.setText("TREFFER auf Reihe " + (r + 1) + ", Spalte " + (c + 1) + "!");
            
            if (checkGameEnd()) return; 
            
            executeKiTurn();
        } else {
            statusLabel.setText("Hier hast du schon hingeschossen! Wähle ein anderes Feld.");
        }
    }

    // Die KI wählt ein zufälliges Feld auf deinem Brett und feuert
    private void executeKiTurn() {
        // 1. STATT dem alten rand.nextInt fragen wir JETZT deine schlaue KI nach Koordinaten!
        Koordinaten kiSchuss = ki.getNextShot();
        int kiRow = kiSchuss.x; // Wichtig: Prüfen, ob bei deinem Partner Row=X oder Row=Y ist!
        int kiCol = kiSchuss.y;

        // 2. Er schießt über seine Spiellogik auf den Spieler
        int result = spiellogik.shootAtPlayer(kiRow, kiCol);

        // 3. WICHTIG: Du musst DEINER KI sagen, was passiert ist, damit sie lernen kann!
        if (result == 1) { // KI wirft auf Wasser
            ki.update(ShotResult.WASSER);
            playerField.setZellenFarbe(kiRow, kiCol, Color.LIGHT_GRAY);
        } else if (result == 2) { // KI trifft dein Schiff
            // Falls deine KI auch VERSENKT kennt, müsstet ihr hier prüfen, ob es versenkt war.
            // Wenn seine Logik nur "Treffer" (2) zurückgibt, schicken wir TREFFER:
            ki.update(ShotResult.TREFFER);
            playerField.setZellenFarbe(kiRow, kiCol, Color.RED);
        }
    
        // Prüfen, ob die KI mit diesem Schuss gewonnen hat
        checkGameEnd();
    }

    // Hilfsmethode zur Überprüfung und Anzeige des Spielendes.
    private boolean checkGameEnd() {
        if (spiellogik.hatSpielerGewonnen()) {
            statusLabel.setText("SIEG! Du hast alle gegnerischen Schiffe versenkt!");
            JOptionPane.showMessageDialog(this, "Herzlichen Glückwunsch! Du hast gewonnen!", "Spiel vorbei", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } else if (spiellogik.hatKiGewonnen()) {
            statusLabel.setText("NIEDERLAGE! Die KI hat deine Flotte zerstört.");
            JOptionPane.showMessageDialog(this, "Schade! Die KI war schneller.", "Spiel vorbei", JOptionPane.WARNING_MESSAGE);
            return true;
        }
        return false;
    }
}