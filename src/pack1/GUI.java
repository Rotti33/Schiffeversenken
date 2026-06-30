package pack1;

import javax.swing.*;
import java.awt.*;

public class GUI extends JFrame {
    //Visuelle Komponenten für die beiden flexiblen Spielfelder
    private Feld spielerFeld;
    private Feld gegnerFeld;
    private JButton menueButton;
    
    //UI-Elemente für Texte und Steuerung
    private JLabel statusLabel;
    private JButton drehButton; //Button zum Wechseln der Platzierungsrichtung
    
    //Referenzen für die KI, die zentrale Spiellogik und Netzwerksteuerung
    private KI ki;
    private Logik spiellogik;
    private Netzwerkspiel netzwerkSpiel;
    private int letzterKlickReihe = -1;
    private int letzterKlickSpalte = -1;
    
    private boolean botSchleifeAktiv = true;
    private String spielmodus = "SINGLEPLAYER"; 
    private boolean popupAktiv = false;

    // Standardkonstruktor (nutzt weiterhin Größe 10, falls nichts übergeben wird)
    public GUI() {
        this(10);
    }

    // Flexibler Konstruktor, der die Wunschgröße direkt verarbeitet
    public GUI(int gewaehlteGroesse) {
        spiellogik = new Logik(gewaehlteGroesse); // Logik mit Wunschgröße starten

        setTitle("Schiffe versenken");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        //Statuszeile im oberen Bereich (Norden) vorbereiten
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        add(statusLabel, BorderLayout.NORTH);

        //Container-Panel für das Nebeneinanderplatzieren der zwei Spielfelder
        JPanel feldContainer = new JPanel(new GridLayout(1, 2, 30, 0));
        feldContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Das eigene Spielfeld wird mit der dynamischen Größe erstellt
        spielerFeld = new Feld("Dein Spielfeld", gewaehlteGroesse, true, e -> {
            Point koordinaten = (Point) e.getSource();
            schiffsPlatzierung(koordinaten.x, koordinaten.y);
        });
        
        // Das gegnerische Spielfeld wird mit der dynamischen Größe erstellt
        gegnerFeld = new Feld("Gegnerisches Feld (KI)", gewaehlteGroesse, true, e -> {
            Point koordinaten = (Point) e.getSource();
            verarbeiteAngriff(koordinaten.x, koordinaten.y);
        });
        
        gegnerFeld.setAktiv(false); // Startet gesperrt!

        //Felder dem Container hinzufügen und mittig im Fenster platzieren
        feldContainer.add(spielerFeld);
        feldContainer.add(gegnerFeld);
        add(feldContainer, BorderLayout.CENTER);

        //Steuerungs-Panel im unteren Bereich (Süden) für die Ausrichtung der Schiffe
        JPanel suedPanel = new JPanel(new FlowLayout());
        
        drehButton = new JButton("Ausrichtung: HORIZONTAL");
        drehButton.setFont(new Font("Arial", Font.PLAIN, 12));
        drehButton.addActionListener(e -> {
            spiellogik.toggleRichtung(); 
            if (spiellogik.getIstHorizontal()) {
                drehButton.setText("Ausrichtung: HORIZONTAL");
            } else {
                drehButton.setText("Ausrichtung: VERTIKAL");
            }
        });
        suedPanel.add(drehButton);

        // Der Autosave- und Hauptmenü-Button
        menueButton = new JButton("Hauptmenü");
        menueButton.setFont(new Font("Arial", Font.PLAIN, 12));
        menueButton.addActionListener(e -> {
            this.botSchleifeAktiv = false; // Stoppt den Thread der Bots
            
            if (this.netzwerkSpiel != null) {
                this.netzwerkSpiel.initiiereSpeichern("autosave_pvp.txt");
            } else {
                Speicher.speichereSpiel(this.spiellogik, this.spielmodus, "autosave_singleplayer.txt");
            }

            new Startmenu(); 
            this.dispose();  
        });
        suedPanel.add(menueButton);

        add(suedPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Der KI-Konstruktor akzeptiert nun die flexible Größe
    public GUI(KI ki, int gewaehlteGroesse) {
        this(gewaehlteGroesse); 
        this.ki = ki;
        this.spielmodus = "SINGLEPLAYER";
        
        char[][] kiBoard = ki.getMyBoard();
        spiellogik.setGegnerFeld(kiBoard);
        aktualisiereStatusText();
    }

    public void setNetzwerkSpiel(Netzwerkspiel netzwerkSpiel) {           
        this.netzwerkSpiel = netzwerkSpiel;
    }

    private void aktualisiereStatusText() {
        if (!spiellogik.alleSchiffePlatziert()) {
            statusLabel.setText("Nächstes Schiff: " + spiellogik.getAktuelleSchiffsLaenge() + " Felder.  |  " + spiellogik.getFlottenText());
        } else {
            statusLabel.setText("Alle Schiffe platziert! Kampfphase läuft.");
        }
    }

    private void schiffsPlatzierung(int r, int c) {
        int laenge = spiellogik.getAktuelleSchiffsLaenge();
        boolean horizontal = spiellogik.getIstHorizontal();

        if (spiellogik.platziereSpielerSchiff(r, c)) {
            for (int i = 0; i < laenge; i++) {
                if (horizontal) {
                    spielerFeld.setZellenFarbe(r, c + i, Color.GRAY);
                } else {
                    spielerFeld.setZellenFarbe(r + i, c, Color.GRAY);
                }
            }

            if (spiellogik.alleSchiffePlatziert()) {
                starteKampfphase();
            } else {
                aktualisiereStatusText();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Schiff passt nicht rein (Überlappung oder Spielfeldrand)", "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    //Steuerung wird von setzen auf Kampf geswitcht
    private void starteKampfphase() {
        aktualisiereStatusText();
        drehButton.setEnabled(false); //Deaktiviert den Ausrichtungs-Button, da er nicht mehr benötigt wird
        
        if (this.netzwerkSpiel != null) {
            // 1. Dem Gegner über das Internet signalisieren, dass wir fertig sind
            this.netzwerkSpiel.sendeReadySignal();
            
            // 2. Wir schalten das Feld NUR DANN frei, wenn BEIDE Seiten absolut fertig aufgestellt haben!
            if (this.netzwerkSpiel.sindBeideBereit()) {
                schalteGegnerFeldAktiv(this.netzwerkSpiel.istMeinZug());
            } else {
                schalteGegnerFeldAktiv(false);
                statusLabel.setText("Warte, bis der Spielpartner alle Schiffe platziert hat...");
            }
        } else {
            // Im normalen Singleplayer-Modus darf der Spieler nach der Setzphase sofort das gegnerfeld attackieren!
            gegnerFeld.setAktiv(true);
        }
    }

    // DIE ZENTRALE SCHUSSMETHODE FÜR ALLE MODI (Komplett klammer-korrigiert!)
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
                boolean spielerFeldVorherAktiv = spielerFeld.isEnabled();
                boolean gegnerFeldVorherAktiv = gegnerFeld.isEnabled();
                
                spielerFeld.setAktiv(false);
                gegnerFeld.setAktiv(false);

                String meldungText = schussAufGegnerFeld ? 
                    "Ein gegnerisches Schiff wurde komplett versenkt!" : "Die KI hat eines deiner Schiffe komplett zerstört!";
                
                // Modusabhängige Weiche für modale Blockade
                boolean istModal = "BOTVSBOT".equals(this.spielmodus);
                JDialog autoPopup = new JDialog(this, "Treffer versenkt", istModal); 
                
                JLabel infoLabel = new JLabel(meldungText, SwingConstants.CENTER);
                infoLabel.setFont(new Font("Arial", Font.BOLD, 14));
                infoLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                autoPopup.add(infoLabel);
                autoPopup.pack();
                autoPopup.setLocationRelativeTo(this); 

                // Unabhängiger Schließ-Thread
                new Thread(() -> {
                    try {
                        Thread.sleep(3000); 
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    SwingUtilities.invokeLater(() -> {
                        autoPopup.dispose();
                        if (!spiellogik.sieg() && !spiellogik.kisieg()) {
                            spielerFeld.setAktiv(spielerFeldVorherAktiv);
                            if (!"BOTVSBOT".equals(this.spielmodus) && !schussAufGegnerFeld) {
                                gegnerFeld.setAktiv(true);
                            } else {
                                gegnerFeld.setAktiv(gegnerFeldVorherAktiv);
                            }
                        }
                    });
                }).start();

                autoPopup.setVisible(true); 
            } // <-- Diese Klammer schließt das if(komplettVersenkt)

            spielende();
            return true; // Extra-Runde bei Treffer!
        } // <-- Diese Klammer schließt das else if(ergebnis == 2)
        
        return false; 
    } // <-- Diese Klammer schließt die gesamte Methode verarbeiteSchussZentral

    //Verarbeitet Angriffe des Spielers auf das gegnerische Spielfeld (Mensch klickt)
    private void verarbeiteAngriff(int r, int c) {
        if (this.popupAktiv) return;

        if (!spiellogik.alleSchiffePlatziert()) return;
        if (spiellogik.sieg() || spiellogik.kisieg()) return;

        // FÜR PVP: Wenn ein Netzwerkspiel läuft, prüfen wir zuerst, ob wir überhaupt am Zug sind!
        if (this.netzwerkSpiel != null && !this.netzwerkSpiel.istMeinZug()) {
            statusLabel.setText("Gegner ist am Zug... Bitte warten.");
            return; 
        }

        int zustand = spiellogik.getGegnerFeldZustand(r, c);
        if (zustand == 2 || zustand == 3) {
            statusLabel.setText("Feld schon gewählt");
            return;
        }

        if (this.netzwerkSpiel != null) {
            this.letzterKlickReihe = r;
            this.letzterKlickSpalte = c;
            this.netzwerkSpiel.spielerKlicktSpielfeld(r, c);
            gegnerFeld.setAktiv(false); 
            return;
        }

        // Normaler Singleplayer-Modus nutzt die zentrale Methode
        boolean treffer = verarbeiteSchussZentral(r, c, true);
        
        if (!treffer) {
            gegnerFeld.setAktiv(false); 
            kiZugAus();
        }
    }

    // Berechnet und visualisiert den Gegenangriff der KI im Singleplayer
    private void kiZugAus() {
        int verzoegerung = 1000;
        if (letzterKlickReihe != -1 && letzterKlickSpalte != -1 && spiellogik.istSpielerSchiffVersenkt(letzterKlickReihe, letzterKlickSpalte)) {
            verzoegerung = 4000;
        }

        Timer kiTimer = new Timer(verzoegerung, e -> {
            boolean schussGueltig = false;
            boolean kiDarfNochmal = false;

            while (!schussGueltig) {
                KI.Koordinaten kiSchuss = ki.getNextShot();
                int kiReihe = kiSchuss.x;
                int kiSpalte = kiSchuss.y;

                int vorZustand = spiellogik.getSpielerFeldZustand(kiReihe, kiSpalte);
                if (vorZustand == 2 || vorZustand == 3) {
                    ki.setFeldBeschossen(kiReihe, kiSpalte);
                    continue;
                }

                schussGueltig = true;
                
                this.letzterKlickReihe = kiReihe;
                this.letzterKlickSpalte = kiSpalte;
                
                kiDarfNochmal = verarbeiteSchussZentral(kiReihe, kiSpalte, false);
                
                // KORREKTUR: Nutzt jetzt dein neues, kleingeschriebenes Spielzustand
                if (kiDarfNochmal) {
                    ki.update(spiellogik.kisieg() ? Spielzustand.ShotResult.VERSENKT : Spielzustand.ShotResult.TREFFER);
                } else {
                    ki.update(Spielzustand.ShotResult.WASSER);
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

            statusLabel.setText("Bot vs. Bot Kampfphase gestartet!");

            while (spielLaeuft && this.botSchleifeAktiv) {
                try {
                    Thread.sleep(1000); // 1 Sekunde Pause zwischen den regulären Schüssen
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (!this.botSchleifeAktiv) break;

                if (bot1IstDran) {
                    KI.Koordinaten schuss = bot1.getNextShot();
                    int vorZustand = spiellogik.getGegnerFeldZustand(schuss.x, schuss.y);
                    if (vorZustand == 2 || vorZustand == 3) {
                        bot1.setFeldBeschossen(schuss.x, schuss.y);
                        continue;
                    }

                    boolean getroffen = verarbeiteSchussZentral(schuss.x, schuss.y, true);
                    
                    if (getroffen) {
                        bot1.update(spiellogik.sieg() ? Spielzustand.ShotResult.VERSENKT : Spielzustand.ShotResult.TREFFER);
                        // BEHOBEN: Das blockierende Thread.sleep(3000) nach dem Versenken wurde gelöscht!
                    } else {
                        bot1.update(Spielzustand.ShotResult.WASSER);
                        bot1IstDran = false; 
                    }
                } else {
                    KI.Koordinaten schuss = bot2.getNextShot();
                    int vorZustand = spiellogik.getSpielerFeldZustand(schuss.x, schuss.y);
                    if (vorZustand == 2 || vorZustand == 3) {
                        bot2.setFeldBeschossen(schuss.x, schuss.y);
                        continue;
                    }

                    boolean getroffen = verarbeiteSchussZentral(schuss.x, schuss.y, false);
                    
                    if (getroffen) {
                        bot2.update(spiellogik.kisieg() ? Spielzustand.ShotResult.VERSENKT : Spielzustand.ShotResult.TREFFER);
                        // BEHOBEN: Das blockierende Thread.sleep(3000) nach dem Versenken wurde gelöscht!
                    } else {
                        bot2.update(Spielzustand.ShotResult.WASSER);
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

    // --- NETZWERK-METHODEN & BRÜCKEN-METHODEN ---

    public void setSpielerSchiffManuell(int r, int c) {
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
            
            if (spiellogik.istSpielerSchiffVersenkt(r, c)) {
                statusLabel.setText("Gegner hat eines deiner Schiffe VERSENKT!");
                Timer popupTimer = new Timer(500, e -> {
                    JOptionPane.showMessageDialog(this, "Die KI/Gegner hat eines deiner Schiffe komplett zerstört!", "Treffer versenkt", JOptionPane.WARNING_MESSAGE);
                });
                popupTimer.setRepeats(false);
                popupTimer.start();
                
                return 2; 
            }
            return 1; 
        }
        return 0; 
    }

    public void visuelleSchussRueckmeldung(int ergebnis) {
        if (letzterKlickReihe == -1 || letzterKlickSpalte == -1) return;
        
        try {
            java.lang.reflect.Field field = Logik.class.getDeclaredField("gegnerfeld");
            field.setAccessible(true);
            int[][] gFeld = (int[][]) field.get(spiellogik);
            
            if (ergebnis == 0) { 
                gegnerFeld.setZellenFarbe(letzterKlickReihe, letzterKlickSpalte, Color.BLUE);
                statusLabel.setText("Fehlschuss auf Gegnerfeld.");
                gFeld[letzterKlickReihe][letzterKlickSpalte] = 2; 
                
                schalteGegnerFeldAktiv(false);
            } else { 
                gegnerFeld.setZellenFarbe(letzterKlickReihe, letzterKlickSpalte, Color.RED);
                statusLabel.setText("Du hast den Gegner getroffen!");
                spiellogik.registriereNetzwerkTreffer();
                gFeld[letzterKlickReihe][letzterKlickSpalte] = 3; 
                
                if (ergebnis == 2) {
                    this.popupAktiv = true;

                    boolean spielerFeldVorherAktiv = spielerFeld.isEnabled();
                    spielerFeld.setAktiv(false);
                    gegnerFeld.setAktiv(false);

                    statusLabel.setText("💥 VERSENKT! Du hast ein gegnerisches Schiff zerstört! 💥");
                    
                    JDialog autoPopup = new JDialog(this, "Treffer versenkt", false); 
                    JLabel infoLabel = new JLabel("Ein gegnerisches Schiff wurde komplett versenkt!", SwingConstants.CENTER);
                    infoLabel.setFont(new Font("Arial", Font.BOLD, 14));
                    infoLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                    autoPopup.add(infoLabel);
                    autoPopup.pack();
                    autoPopup.setLocationRelativeTo(this);
                    autoPopup.setVisible(true);

                    Timer schliessTimer = new Timer(3000, event -> {
                        autoPopup.dispose();
                        this.popupAktiv = false;

                        if (!spiellogik.sieg() && !spiellogik.kisieg()) {
                            spielerFeld.setAktiv(spielerFeldVorherAktiv);
                            schalteGegnerFeldAktiv(true);
                        }
                    });
                    schliessTimer.setRepeats(false);
                    schliessTimer.start();
                } else {
                    schalteGegnerFeldAktiv(true);
                }
            }
        } catch(Exception e) { 
            e.printStackTrace(); 
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

    public void ladeSpielstandVisuell() {
        starteKampfphase(); 
        int N = spiellogik.getGroesse();
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                int sZustand = spiellogik.getSpielerFeldZustand(r, c);
                if (sZustand == 1) spielerFeld.setZellenFarbe(r, c, Color.GRAY);
                else if (sZustand == 2) spielerFeld.setZellenFarbe(r, c, Color.BLUE);
                else if (sZustand == 3) spielerFeld.setZellenFarbe(r, c, Color.RED);

                int gZustand = spiellogik.getGegnerFeldZustand(r, c);
                if (gZustand == 2) gegnerFeld.setZellenFarbe(r, c, Color.BLUE);
                else if (gZustand == 3) gegnerFeld.setZellenFarbe(r, c, Color.RED);
            }
        }
    }
}