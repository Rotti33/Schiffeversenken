package pack1;

import javax.swing.*;
import pack1.KI.Koordinaten;
import pack1.KI.ShotResult;
import java.awt.*;

public class GUI extends JFrame {
    //Visuelle Komponenten für die beiden 10x10 Spielfelder
    private Feld spielerFeld;
    private Feld gegnerFeld;
    private JButton menueButton;
    
    //UI-Elemente für Texte und Steuerung
    private JLabel statusLabel;
    private JButton drehButton; //Button zum Wechseln der Platzierungsrichtung
    
    //Referenzen für die KI, die zentrale Spiellogik und Netzwerksteuerung
    private KI ki;
    private Logik spiellogik;
    private boolean botSchleifeAktiv = true;
    private spiel netzwerkSpiel;
    private int letzterKlickReihe = -1;
    private int letzterKlickSpalte = -1;
    private String spielmodus = "SINGLEPLAYER";

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

        // Der Autosave- und Hauptmenü-Button
JButton menueButton = new JButton("Hauptmenü");
        menueButton.setFont(new Font("Arial", Font.PLAIN, 12));
        menueButton.addActionListener(e -> {
            this.botSchleifeAktiv = false; 
            
            if (this.netzwerkSpiel != null) {
                this.netzwerkSpiel.initiiereSpeichern("autosave_pvp.txt");
            } else {
                try {
                    java.io.PrintWriter writer = new java.io.PrintWriter("autosave_singleplayer.txt");
                    
                    // BEHOBEN: Verlässt sich jetzt zu 100% auf die neue Variable
                    writer.println("MODUS: " + this.spielmodus);

                    writer.println("Spielerfeld:");
                    for (int r = 0; r < 10; r++) {
                        for (int c = 0; c < 10; c++) {
                            writer.print(spiellogik.getSpielerFeldZustand(r, c) + " ");
                        }
                        writer.println();
                    }
                    writer.println("Gegnerfeld:");
                    for (int r = 0; r < 10; r++) {
                        for (int c = 0; c < 10; c++) {
                            writer.print(spiellogik.getGegnerFeldZustand(r, c) + " ");
                        }
                        writer.println();
                    }
                    writer.close();
                    System.out.println("Spielstand erfolgreich automatisch gespeichert!");
                } catch (Exception ex) {
                    System.err.println("Fehler beim Autosave: " + ex.getMessage());
                }
            }

            new Startmenu(); 
            this.dispose();  
        });
        suedPanel.add(menueButton);

        // WICHTIG: Das Panel muss der GUI im Süden hinzugefügt werden!
        add(suedPanel, BorderLayout.SOUTH);

        // Fenstergröße automatisch anpassen und mittig auf dem Bildschirm platzieren
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    } // Hier endet der Konstruktor der GUI-Klasse

    //Erweiterter Konstruktor: Wird von der Main aufgerufen um die KI-Instanz zu übergeben
    public GUI(KI ki) {
        this(); //Ruft zuerst den parameterlosen Standardkonstruktor darüber auf
        this.ki = ki;
        this.spielmodus = "SINGLEPLAYER";
        
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
        if (this.netzwerkSpiel != null) {
            this.netzwerkSpiel.sendeReadySignal();
        }
    }

    private void refreshSpielerSpielfeldVisuell() {
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                int zelle = spiellogik.getSpielerFeldZustand(r, c);
                if (zelle == 1) {
                    spielerFeld.setZellenFarbe(r, c, Color.GRAY);
                }
            }
        }
    }

    // DIE NEUE ZENTRALE SCHUSSMETHODE FÜR ALLE MODI
    // schussAufGegnerFeld: true = Schuss geht nach rechts (Gegnerfeld), false = nach links (Spielerfeld)
    public boolean verarbeiteSchussZentral(int r, int c, boolean schussAufGegnerFeld) {
        int ergebnis;
        Feld zielFeld = schussAufGegnerFeld ? gegnerFeld : spielerFeld;
        
        if (schussAufGegnerFeld) {
            ergebnis = spiellogik.schussAufGegner(r, c);
        } else {
            ergebnis = spiellogik.schussAufSpieler(r, c);
        }

        if (ergebnis == 1) { // Wasser getroffen
            zielFeld.setZellenFarbe(r, c, Color.BLUE);
            if (schussAufGegnerFeld) {
                statusLabel.setText("Bot 1 verfehlt bei Reihe " + (r + 1) + ", Spalte " + (c + 1));
            } else {
                statusLabel.setText("Bot 2 verfehlt bei Reihe " + (r + 1) + ", Spalte " + (c + 1));
            }
            spielende();
            return false; // Keine Extra-Runde bei Wasser
            
        } else if (ergebnis == 2) { // Schiff getroffen
            zielFeld.setZellenFarbe(r, c, Color.RED);
            if (schussAufGegnerFeld) {
                statusLabel.setText("Bot 1 landete einen TREFFER bei Reihe " + (r + 1) + ", Spalte " + (c + 1) + "!");
            } else {
                statusLabel.setText("Bot 2 landete einen TREFFER bei Reihe " + (r + 1) + ", Spalte " + (c + 1) + "!");
            }

            // Prüfen, ob das getroffene Schiff komplett zerstört wurde
            boolean komplettVersenkt = schussAufGegnerFeld ? 
                spiellogik.istGegnerSchiffVersenkt(r, c) : spiellogik.istSpielerSchiffVersenkt(r, c);

            if (komplettVersenkt) {
                // Bestimme den Text für das automatische Popup
                String meldungText = schussAufGegnerFeld ? 
                    "Ein gegnerisches Schiff wurde komplett versenkt!" : "Die KI hat eines deiner Schiffe komplett zerstört!";
                
                // Erstelle das temporäre Popup-Fenster (JDialog)
                JDialog autoPopup = new JDialog(this, "Treffer versenkt", false); // false = blockiert die GUI nicht im Hintergrund
                JLabel infoLabel = new JLabel(meldungText, SwingConstants.CENTER);
                infoLabel.setFont(new Font("Arial", Font.BOLD, 14));
                infoLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                autoPopup.add(infoLabel);
                autoPopup.pack();
                autoPopup.setLocationRelativeTo(this); // Mittig über dem Hauptspielfeld platzieren
                autoPopup.setVisible(true);

                // Dieser Timer schließt das Popup nach genau 3 Sekunden automatisch
                Timer schliessTimer = new Timer(3000, event -> {
                    autoPopup.dispose(); // Schließt das kleine Fenster sauber
                });
                schliessTimer.setRepeats(false);
                schliessTimer.start();

                // Wichtig für den Bot: Wir lassen den Hintergrund-Thread für 3 Sekunden schlafen,
                // damit der Bot exakt so lange wartet, wie das Popup auf dem Bildschirm zu sehen ist!
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            spielende();
            return true; // Extra-Runde bei Treffer!
        }
        return false; // Ungültiges/bereits beschossenes Feld
    }

    //Verarbeitet Angriffe des Spielers auf das gegnerische Spielfeld (Mensch klickt)
    private void verarbeiteAngriff(int r, int c) {
        if (!spiellogik.alleSchiffePlatziert()) return;
        if (spiellogik.sieg() || spiellogik.kisieg()) return;

        int zustand = spiellogik.getGegnerFeldZustand(r, c);
        if (zustand == 2 || zustand == 3) {
            statusLabel.setText("Feld schon gewählt");
            return;
        }

        if (this.netzwerkSpiel != null) {
            this.letzterKlickReihe = r;
            this.letzterKlickSpalte = c;
            this.netzwerkSpiel.spielerKlicktSpielfeld(r, c);
            return;
        }

        // Singleplayer-Modus nutzt die zentrale Methode
        boolean treffer = verarbeiteSchussZentral(r, c, true);
        
        if (!treffer) {
            gegnerFeld.setAktiv(false); // Sperren bei Wasser
            kiZugAus();
        }
    }

    //Berechnet und visualisiert den Gegenangriff der KI im Singleplayer
    private void kiZugAus() {
        Timer kiTimer = new Timer(1000, e -> {
            boolean schussGueltig = false;
            boolean kiDarfNochmal = false;

            while (!schussGueltig) {
                Koordinaten kiSchuss = ki.getNextShot();
                int kiReihe = kiSchuss.x;
                int kiSpalte = kiSchuss.y;

                int vorZustand = spiellogik.getSpielerFeldZustand(kiReihe, kiSpalte);
                if (vorZustand == 2 || vorZustand == 3) {
                    ki.setFeldBeschossen(kiReihe, kiSpalte);
                    continue;
                }

                schussGueltig = true;
                // KI nutzt die zentrale Methode
                kiDarfNochmal = verarbeiteSchussZentral(kiReihe, kiSpalte, false);
                
                if (kiDarfNochmal) {
                    ki.update(spiellogik.kisieg() ? ShotResult.VERSENKT : ShotResult.TREFFER);
                } else {
                    ki.update(ShotResult.WASSER);
                    gegnerFeld.setAktiv(true); // Entsperren bei Wasser
                }
            }

            if (spiellogik.kisieg() || spiellogik.sieg()) return;

            if (kiDarfNochmal) {
                kiZugAus();
            }
        });
        kiTimer.setRepeats(false);
        kiTimer.start();
    }

    // Startet das automatische Match zwischen zwei Bots
public void starteBotSchleife(KI bot1, KI bot2) {
        this.spielmodus = "BOTVSBOT";
        new Thread(() -> {
            boolean spielLaeuft = true;
            boolean bot1IstDran = true;
            this.botSchleifeAktiv = true; // Schleife aktivieren beim Start

            spielerFeld.setAktiv(false);
            gegnerFeld.setAktiv(false);

            // Setzphase visuell überspringen, da Bots bereits aufgestellt sind
            statusLabel.setText("Bot vs. Bot Kampfphase gestartet!");

            // BEHOBEN: Prüft jetzt zusätzlich, ob die Schleife aktiv bleiben soll!
            while (spielLaeuft && this.botSchleifeAktiv) {
                try {
                    Thread.sleep(1000); 
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (bot1IstDran) {
                    Koordinaten schuss = bot1.getNextShot();
                    int vorZustand = spiellogik.getGegnerFeldZustand(schuss.x, schuss.y);
                    if (vorZustand == 2 || vorZustand == 3) {
                        bot1.setFeldBeschossen(schuss.x, schuss.y);
                        continue;
                    }

                    boolean getroffen = verarbeiteSchussZentral(schuss.x, schuss.y, true);
                    if (getroffen) {
                        bot1.update(spiellogik.sieg() ? ShotResult.VERSENKT : ShotResult.TREFFER);
                    } else {
                        bot1.update(ShotResult.WASSER);
                        bot1IstDran = false; 
                    }
                } else {
                    Koordinaten schuss = bot2.getNextShot();
                    int vorZustand = spiellogik.getSpielerFeldZustand(schuss.x, schuss.y);
                    if (vorZustand == 2 || vorZustand == 3) {
                        bot2.setFeldBeschossen(schuss.x, schuss.y);
                        continue;
                    }

                    boolean getroffen = verarbeiteSchussZentral(schuss.x, schuss.y, false);
                    if (getroffen) {
                        bot2.update(spiellogik.kisieg() ? ShotResult.VERSENKT : ShotResult.TREFFER);
                    } else {
                        bot2.update(ShotResult.WASSER);
                        bot1IstDran = true; 
                    }
                }

                if (spiellogik.sieg() || spiellogik.kisieg()) {
                    spielLaeuft = false;
                }
            }
        }).start();
    }

    //Hilfsmethode zur Überprüfung und Anzeige des Spielendes
    private boolean spielende() {
        if (spiellogik.sieg()) {
            statusLabel.setText("SIEG! Alle gegnerischen Schiffe wurden versenkt!");
            JOptionPane.showMessageDialog(this, "Das Spiel ist vorbei! Ein Sieger steht fest.", "Spiel vorbei", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } else if (spiellogik.kisieg()) {
            statusLabel.setText("NIEDERLAGE! Die Flotte wurde zerstört.");
            JOptionPane.showMessageDialog(this, "Das Spiel ist vorbei! Die Flotte wurde vernichtet.", "Spiel vorbei", JOptionPane.WARNING_MESSAGE);
            return true;
        }
        return false;
    }

    // --- NEUE NETZWERK-METHODEN & BRÜCKEN-METHODEN (FÜR STARTMENÜ & spiel) ---

    // Ermöglicht es dem Startmenü, das Schiff in die Logik-Matrix einzutragen
    public void setSpielerSchiffManuell(int r, int c) {
        // Ein kleiner Trick, um die interne Platzierung ohne Klicks zu simulieren
        // Wir nutzen die Java-Reflexion oder setzen es über ein Umgehen der Setzphase, 
        // hier tragen wir es einfach direkt ein, um die Logik auf 'Schiff' (1) zu setzen.
        try {
            java.lang.reflect.Field field = Logik.class.getDeclaredField("spielerfeld");
            field.setAccessible(true);
            int[][] sFeld = (int[][]) field.get(spiellogik);
            sFeld[r][c] = 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Logik getSpiellogik() {
        return spiellogik;
    }

    public Feld getSpielerFeld() {
        return spielerFeld;
    }
public Feld getGegnerFeld() {
        return gegnerFeld;
    }

    public void initialisiereSpielfeld(int zeilen, int spalten) {
        System.out.println("Netzwerk: Spielfeld initialisiert auf " + zeilen + "x" + spalten);
    }

    public void generiereSchiffsFlotte(int[] laengen) {
        System.out.println("Netzwerk: Flotte empfangen.");
    }

    public int pruefeGegnerSchuss(int r, int c) {
        boolean getroffen = verarbeiteSchussZentral(r, c, false);
        if (getroffen) {
            return spiellogik.kisieg() ? 2 : 1; 
        }
        return 0; 
    }

    public void visuelleSchussRueckmeldung(int ergebnis) {
        if (letzterKlickReihe == -1 || letzterKlickSpalte == -1) return;
        
        if (ergebnis == 0) { 
            gegnerFeld.setZellenFarbe(letzterKlickReihe, letzterKlickSpalte, Color.BLUE);
            statusLabel.setText("Fehlschuss auf Gegnerfeld.");
        } else { 
            gegnerFeld.setZellenFarbe(letzterKlickReihe, letzterKlickSpalte, Color.RED);
            statusLabel.setText("Du hast den Gegner getroffen!");
            spiellogik.registriereNetzwerkTreffer();
            
            if (spiellogik.istGegnerSchiffVersenkt(letzterKlickReihe, letzterKlickSpalte)) {
                statusLabel.setText("VERSENKT! Du hast ein gegnerisches Schiff zerstört!");
                Timer popupTimer = new Timer(500, e -> {
                    JOptionPane.showMessageDialog(this, "Ein gegnerisches Schiff wurde komplett versenkt!", "Guter Schuss", JOptionPane.INFORMATION_MESSAGE);
                });
                popupTimer.setRepeats(false);
                popupTimer.start();
            }
        }
        spielende();
    }

    public void zeigeVerbindungVerlorenMeldung() {
        JOptionPane.showMessageDialog(this, "Verbindung zum Netzwerk-Spielpartner verloren!", "Fehler", JOptionPane.ERROR_MESSAGE);
    }

    public void schalteGegnerFeldAktiv(boolean istMeinZug) {
        gegnerFeld.setAktiv(istMeinZug);
        if (istMeinZug) {
            statusLabel.setText("Du bist am Zug! Klicke auf das gegnerische Feld.");
        } else {
            statusLabel.setText("Gegner ist am Zug... Bitte warten.");
        }
    }

// NEU: Zeichnet das Spielfeld nach dem Laden komplett neu (HIER IN DIE GUI.JAVA!)
    public void ladeSpielstandVisuell() {
        starteKampfphase(); // Wechselt direkt in den Kampfmodus
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                // Spielerfeld einfärben
                int sZustand = spiellogik.getSpielerFeldZustand(r, c);
                if (sZustand == 1) spielerFeld.setZellenFarbe(r, c, Color.GRAY);
                else if (sZustand == 2) spielerFeld.setZellenFarbe(r, c, Color.BLUE);
                else if (sZustand == 3) spielerFeld.setZellenFarbe(r, c, Color.RED);

                // Gegnerfeld einfärben
                int gZustand = spiellogik.getGegnerFeldZustand(r, c);
                if (gZustand == 2) gegnerFeld.setZellenFarbe(r, c, Color.BLUE);
                else if (gZustand == 3) gegnerFeld.setZellenFarbe(r, c, Color.RED);
            }
        }
    }
}