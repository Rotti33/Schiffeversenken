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
    
    //Referenzen für die KI, die zentrale Spiellogik und Netzwerksteuerung
    private KI ki;
    private Logik spiellogik;
    private spiel netzwerkSpiel;
    private int letzterKlickReihe = -1;
    private int letzterKlickSpalte = -1;

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

    public void setNetzwerkSpiel(spiel netzwerkSpiel) {           //jjj
        this.netzwerkSpiel = netzwerkSpiel;
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

        // NEU FÜR PVP: Wenn es ein Netzwerkspiel ist, melden wir dem Gegner, dass wir fertig sind!
        if (this.netzwerkSpiel != null) {
            this.netzwerkSpiel.sendeReadySignal();
        }
    }

    private void refreshSpielerSpielfeldVisuell() {      //jjj
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                int zelle = spiellogik.getSpielerFeldZustand(r, c);
                if (zelle == 1) {
                    spielerFeld.setZellenFarbe(r, c, Color.GRAY);
                }
            }
        }
    }

    //Verarbeitet Angriffe des Spielers auf das gegnerische Spielfeld
    private void verarbeiteAngriff(int r, int c) {
        if (!spiellogik.alleSchiffePlatziert()) return;
        if (spiellogik.sieg() || spiellogik.kisieg()) return;

        if (this.netzwerkSpiel != null) {
            this.letzterKlickReihe = r;
            this.letzterKlickSpalte = c;
            this.netzwerkSpiel.spielerKlicktSpielfeld(r, c);
            return;
        }

        // Normaler Singleplayer-Modus
        int ergebnis = spiellogik.schussAufGegner(r, c);

        if (ergebnis == 1) { 
            gegnerFeld.setZellenFarbe(r, c, Color.BLUE);
            statusLabel.setText("Fehlschuss auf Reihe " + (r + 1) + ", Spalte " + (c + 1));
            if (spielende()) return; 
            kiZugAus();
        } else if (ergebnis == 2) { 
            gegnerFeld.setZellenFarbe(r, c, Color.RED);
            statusLabel.setText("TREFFER auf Reihe " + (r + 1) + ", Spalte " + (c + 1) + "!");
            spielende(); 
        } else {
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
                if (spiellogik.kisieg()) {
                    ki.update(ShotResult.VERSENKT);
                } else {
                    ki.update(ShotResult.TREFFER);
                }
                spielerFeld.setZellenFarbe(kiReihe, kiSpalte, Color.RED); //Spielerfeld-Zelle wird rot
                schussGueltig = true; //Zug gültig abgeschlossen Schleife bricht ab
            } else {
                ki.setFeldBeschossen(kiReihe, kiSpalte);
            }
        }
        spielende();
    }

    //Hilfsmethode zur Überprüfung und Anzeige des Spielendes
    private boolean spielende() {
        if (spiellogik.sieg()) {
            statusLabel.setText("SIEG! Du hast alle gegnerischen Schiffe versenkt!");
            JOptionPane.showMessageDialog(this, "Herzlichen Glückwunsch! Du hast gewonnen!", "Spiel vorbei", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } else if (spiellogik.kisieg()) {
            statusLabel.setText("NIEDERLAGE! Die KI hat deine Flotte zerstört.");
            JOptionPane.showMessageDialog(this, "Schade! Die KI war schneller.", "Spiel vorbei", JOptionPane.WARNING_MESSAGE);
            return true;
        }
        return false;
    }

    // --- NEUE NETZWERK-METHODEN (FÜR KLASSE spiel) ---

    public void initialisiereSpielfeld(int zeilen, int spalten) {
        System.out.println("Netzwerk: Spielfeld initialisiert auf " + zeilen + "x" + spalten);
    }

    public void generiereSchiffsFlotte(int[] laengen) {
        System.out.println("Netzwerk: Flotte empfangen.");
    }

    public int pruefeGegnerSchuss(int r, int c) {
        int ergebnis = spiellogik.schussAufSpieler(r, c);
        if (ergebnis == 1) {
            spielerFeld.setZellenFarbe(r, c, Color.BLUE);
            statusLabel.setText("Gegner schießt ins Wasser bei Reihe " + (r + 1) + ", Spalte " + (c + 1));
            return 0;
        } else if (ergebnis == 2) {
            spielerFeld.setZellenFarbe(r, c, Color.RED);
            statusLabel.setText("Gegner landete einen TREFFER bei Reihe " + (r + 1) + ", Spalte " + (c + 1) + "!");
            if (spiellogik.kisieg()) {
                return 2;
            }
            return 1;
        }
        return 0;
    }

    public void visuelleSchussRueckmeldung(int ergebnis) {
        if (letzterKlickReihe == -1 || letzterKlickSpalte == -1) return;
        
        if (ergebnis == 0) { 
            gegnerFeld.setZellenFarbe(letzterKlickReihe, letzterKlickSpalte, Color.BLUE);
            statusLabel.setText("Fehlschuss auf Gegnerfeld.");
        } else if (ergebnis == 1 || ergebnis == 2) { 
            gegnerFeld.setZellenFarbe(letzterKlickReihe, letzterKlickSpalte, Color.RED);
            statusLabel.setText("Du hast den Gegner getroffen!");
            spiellogik.registriereNetzwerkTreffer();
        }
        spielende();
    }

    public void zeigeVerbindungVerlorenMeldung() {
        JOptionPane.showMessageDialog(this, "Verbindung zum Netzwerk-Spielpartner verloren!", "Fehler", JOptionPane.ERROR_MESSAGE);
    }

// Startet das automatische Match zwischen zwei Bots
    public void starteBotSchleife(KI bot1, KI bot2) {
        new Thread(() -> {
            boolean spielLaeuft = true;
            boolean bot1IstDran = true;

            // Schaltet beide Felder auf inaktiv, damit der Mensch nicht reinklicken kann
            spielerFeld.setAktiv(false);
            gegnerFeld.setAktiv(false);

            while (spielLaeuft) {
                try {
                    Thread.sleep(500); // 0,5 Sekunden Pause zwischen den Schüssen zum Zuschauen
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (bot1IstDran) {
                    // Bot 1 schießt auf das Feld von Bot 2 (rechtes Feld)
                    KI.Koordinaten schuss = bot1.getNextShot();
                    int ergebnis = spiellogik.schussAufGegner(schuss.x, schuss.y);

                    if (ergebnis == 1) { // Wasser
                        bot1.update(KI.ShotResult.WASSER);
                        gegnerFeld.setZellenFarbe(schuss.x, schuss.y, Color.BLUE);
                        statusLabel.setText("Bot 1 schießt ins Wasser bei " + (schuss.x + 1) + "," + (schuss.y + 1));
                        bot1IstDran = false; // Wechsel zu Bot 2
                    } else if (ergebnis == 2) { // Treffer
                        bot1.update(spiellogik.sieg() ? KI.ShotResult.VERSENKT : KI.ShotResult.TREFFER);
                        gegnerFeld.setZellenFarbe(schuss.x, schuss.y, Color.RED);
                        statusLabel.setText("Bot 1 TRIFFT bei " + (schuss.x + 1) + "," + (schuss.y + 1) + "!");
                    } else {
                        bot1.setFeldBeschossen(schuss.x, schuss.y);
                    }
                } else {
                    // Bot 2 schießt auf das Feld von Bot 1 (linkes Feld)
                    KI.Koordinaten schuss = bot2.getNextShot();
                    int ergebnis = spiellogik.schussAufSpieler(schuss.x, schuss.y);

                    if (ergebnis == 1) { // Wasser
                        bot2.update(KI.ShotResult.WASSER);
                        spielerFeld.setZellenFarbe(schuss.x, schuss.y, Color.BLUE);
                        statusLabel.setText("Bot 2 schießt ins Wasser bei " + (schuss.x + 1) + "," + (schuss.y + 1));
                        bot1IstDran = true; // Wechsel zu Bot 1
                    } else if (ergebnis == 2) { // Treffer
                        bot2.update(spiellogik.kisieg() ? KI.ShotResult.VERSENKT : KI.ShotResult.TREFFER);
                        spielerFeld.setZellenFarbe(schuss.x, schuss.y, Color.RED);
                        statusLabel.setText("Bot 2 TRIFFT bei " + (schuss.x + 1) + "," + (schuss.y + 1) + "!");
                    } else {
                        bot2.setFeldBeschossen(schuss.x, schuss.y);
                    }
                }

                // Prüft nach jedem Schuss das Spielende
                if (spiellogik.sieg() || spiellogik.kisieg()) {
                    spielLaeuft = false;
                    spielende();
                }
            }
        }).start();
    }

// NEU FÜR PVP: Schaltet das gegnerische Feld je nach Zugrecht aktiv oder inaktiv
    public void schalteGegnerFeldAktiv(boolean istMeinZug) {
        gegnerFeld.setAktiv(istMeinZug);
        if (istMeinZug) {
            statusLabel.setText("Du bist am Zug! Klicke auf das gegnerische Feld.");
        } else {
            statusLabel.setText("Gegner ist am Zug... Bitte warten.");
        }
    }
}