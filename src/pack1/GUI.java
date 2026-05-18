package pack1;

import javax.swing.*;
import pack1.KI.Koordinaten;
import pack1.KI.ShotResult;
import java.awt.*;

public class GUI extends JFrame {
    //Visuelle Komponenten für die beiden 10x10 Spielfelder
    private Feld spielerFeld;
    private Feld gegnerFeld;
    
    //UI-Elemente für Texte und Steuerung
    private JLabel statusLabel;
    private JButton drehButton; //Button zum Wechseln der Platzierungsrichtung
    
    //Referenzen für die KI und die zentrale Spiellogik
    private KI ki;
    private Logik spiellogik;

    //Standardkonstruktor: Initialisiert das Anwendungsfenster und die Logik
    public GUI() {
        spiellogik = new Logik();

        setTitle("Schiffe versenken");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        //Statuszeile im oberen Bereich (Norden) vorbereiten
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        add(statusLabel, BorderLayout.NORTH);
        aktualisiereStatusText(); //Setzt den Text für das erste zu platzierende Schiff

        //Container-Panel für das Nebeneinanderplatzieren der zwei Spielfelder
        JPanel feldContainer = new JPanel(new GridLayout(1, 2, 30, 0));
        feldContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //Das eigene Spielfeld: Reagiert in der Setzphase auf Mausklicks zum Platzieren
        spielerFeld = new Feld("Dein Spielfeld", true, e -> {
            Point koordinaten = (Point) e.getSource();
            schiffsPlatzierung(koordinaten.x, koordinaten.y);
        });
        
        //Das gegnerische Spielfeld: Nimmt Klicks an und führt in der Kampfphase Angriffe aus
        gegnerFeld = new Feld("Gegnerisches Feld (KI)", true, e -> {
            Point koordinaten = (Point) e.getSource();
            verarbeiteAngriff(koordinaten.x, koordinaten.y);
        });

        //Felder dem Container hinzufügen und mittig im Fenster platzieren
        feldContainer.add(spielerFeld);
        feldContainer.add(gegnerFeld);
        add(feldContainer, BorderLayout.CENTER);

        //Steuerungs-Panel im unteren Bereich (Süden) für die Ausrichtung der Schiffe
        JPanel suedPanel = new JPanel(new FlowLayout());
        drehButton = new JButton("Ausrichtung: HORIZONTAL");
        drehButton.setFont(new Font("Arial", Font.PLAIN, 12));
        drehButton.addActionListener(e -> {
            spiellogik.toggleRichtung(); //Schaltet die Logik zwischen true/false um
            if (spiellogik.getIstHorizontal()) {
                drehButton.setText("Ausrichtung: HORIZONTAL");
            } else {
                drehButton.setText("Ausrichtung: VERTIKAL");
            }
        });
        suedPanel.add(drehButton);
        add(suedPanel, BorderLayout.SOUTH);

        //Fenstergröße automatisch anpassen und mittig auf dem Bildschirm platzieren
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    //Erweiterter Konstruktor: Wird von der Main aufgerufen um die KI-Instanz zu übergeben
    public GUI(KI ki) {
        this(); //Ruft zuerst den parameterlosen Standardkonstruktor darüber auf
        this.ki = ki;
        
        //Liest das verdeckte Schiffs Feld der KI aus und übergibt es an die Logik
        char[][] kiBoard = ki.getMyBoard();
        spiellogik.setGegnerFeld(kiBoard);
    }

    //Hilfsmethode um den Hinweistext in der oberen Statuszeile je nach Spielphase anzupassen
    private void aktualisiereStatusText() {
        if (!spiellogik.alleSchiffePlatziert()) {
            statusLabel.setText("Platziere ein Schiff der Länge " + spiellogik.getAktuelleSchiffsLaenge());
        } else {
            statusLabel.setText("Spieler darf Angreifen");
        }
    }

    //Verarbeitet Mausklicks auf dem eigenen Feld während der Schiffs Aufstellung
    private void schiffsPlatzierung(int r, int c) {
        int laenge = spiellogik.getAktuelleSchiffsLaenge();
        boolean horizontal = spiellogik.getIstHorizontal();

        //Versucht das Schiff über die Logik-Klasse regelkonform auf dem Feld einzutragen
        if (spiellogik.platziereSpielerSchiff(r, c)) {
            //Wenn erfolgreich werden die betroffenen Zellen auf dem GUI-Spielfeld grau gefärbt
            for (int i = 0; i < laenge; i++) {
                if (horizontal) {
                    spielerFeld.setZellenFarbe(r, c + i, Color.GRAY);
                } else {
                    spielerFeld.setZellenFarbe(r + i, c, Color.GRAY);
                }
            }

            //Prüfen, ob nach dieser Platzierung die Setzphase abgeschlossen ist
            if (spiellogik.alleSchiffePlatziert()) {
                starteKampfphase();
            } else {
                aktualisiereStatusText();
            }
        } else {
            //Fehlermeldung anzeigen wenn das Schiff das Feld verlässt oder blockiert wird
            JOptionPane.showMessageDialog(this, "Schiff passt nicht rein", "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    //Steuerung wird von setzen auf Kampf geswitcht
    private void starteKampfphase() {
        aktualisiereStatusText();
        drehButton.setEnabled(false); //Deaktiviert den Ausrichtungs-Button, da er nicht mehr benötigt wird
    }

    //Verarbeitet Angriffe des Spielers auf das gegnerische Spielfeld
    private void verarbeiteAngriff(int r, int c) {
        //Klicks ignorieren solange der Spieler seine eigenen Schiffe noch nicht fertig platziert hat
        if (!spiellogik.alleSchiffePlatziert()) return;
        
        //Sofort abbrechen wenn das Spiel bereits durch einen Sieger beendet wurde
        if (spiellogik.sieg() || spiellogik.kisieg()) return;

        //Schuss in der Logik auswerten lassen
        int ergebnis = spiellogik.schussAufGegner(r, c);

        if (ergebnis == 1) { //Fehlschuss (Wasser getroffen)
            gegnerFeld.setZellenFarbe(r, c, Color.BLUE); //Zelle wird blau gefärbt
            statusLabel.setText("Fehlschuss auf Reihe " + (r + 1) + ", Spalte " + (c + 1));
            
            if (spielende()) return; //Spielende prüfen bevor die KI zieht
            
            kiZugAus();
        } else if (ergebnis == 2) { //Treffer
            gegnerFeld.setZellenFarbe(r, c, Color.RED); //Zelle wird rot gefärbt
            statusLabel.setText("TREFFER auf Reihe " + (r + 1) + ", Spalte " + (c + 1) + "!");
            
            spielende(); // Prüfen, ob der Spieler mit diesem Treffer gewonnen hat 
        } else {
            //Wenn ergebnis == 0, wurde dieses Feld bereits vorher beschossen
            statusLabel.setText("Feld schon gewählt");
        }
    }

    //Berechnet und visualisiert den Gegenangriff der KI
    private void kiZugAus() {
        boolean schussGueltig = false;

        //Die KI fragt in einer Schleife so lange nach neuen Koordinaten bis sie ein unbeschossenes Feld trifft
        while (!schussGueltig) {
            Koordinaten kiSchuss = ki.getNextShot();
            int kiReihe = kiSchuss.x; 
            int kiSpalte = kiSchuss.y;

            //Führt den Schuss auf das Spielerfeld in der Logik aus
            int ergebnis = spiellogik.schussAufSpieler(kiReihe, kiSpalte);

            if (ergebnis == 1) { //KI hat Wasser getroffen
                ki.update(ShotResult.WASSER); //KI über Fehlschuss informieren
                spielerFeld.setZellenFarbe(kiReihe, kiSpalte, Color.BLUE); //Spielerfeld-Zelle wird blau
                schussGueltig = true; //Zug gültig abgeschlossen Schleife bricht ab
            } else if (ergebnis == 2) { //KI hat ein Spielerschiff getroffen
                //Prüfen ob das Spiel vorbei ist
                if (spiellogik.kisieg()) {
                    ki.update(ShotResult.VERSENKT);
                } else {
                    ki.update(ShotResult.TREFFER);
                }
                spielerFeld.setZellenFarbe(kiReihe, kiSpalte, Color.RED); //Spielerfeld-Zelle wird rot
                schussGueltig = true; //Zug gültig abgeschlossen Schleife bricht ab
            } else {
                //KI sucht nicht benutztes Feld
                ki.setFeldBeschossen(kiReihe, kiSpalte);
            }
        }

        //Prüfung ob KI gewonnen hat
        spielende();
    }

    //Hilfsmethode zur Überprüfung und Anzeige des Spielendes
    private boolean spielende() {
        if (spiellogik.sieg()) {
            statusLabel.setText("Gewonnen!");
            JOptionPane.showMessageDialog(this, "Herzlichen Glückwunsch! Du hast gewonnen!", "Spiel vorbei", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } else if (spiellogik.kisieg()) {
            statusLabel.setText("NIEDERLAGE!");
            JOptionPane.showMessageDialog(this, "Die KI hat gewonen", "Spiel vorbei", JOptionPane.WARNING_MESSAGE);
            return true;
        }
        return false;
    }
}