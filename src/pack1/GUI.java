package pack1;

import javax.swing.*;
import java.awt.*;

/**
 * Hauptbenutzeroberfläche des Schiffe-versenken-Spiels.
 * 
 * Diese Klasse verwaltet die gesamte GUI mit zwei Spielfeldern (Spieler und Gegner),
 * Steuerungselementen und visuellen Rückmeldungen. Sie unterstützt:
 * - Singleplayer-Modus (Mensch vs. KI)
 * - CPU vs. CPU-Modus (zwei Bots spielen gegeneinander)
 * - Netzwerk-Multiplayer-Modus (Mensch vs. Mensch)
 * - Schiffsplatzierung und Kampfphase
 * - Versenkungserkennung mit Pop-ups
 * - Automatisches Speichern und Laden
 * 
 * Die GUI wurde flexibel entworfen, um verschiedene Spielfeldgrößen zu unterstützen
 * und responsive auf alle Modi zu reagieren.
 * 
 * @author Lisa Renner, Rodrigo Malisi Sousa
 * @version 1.0
 * @see Feld
 * @see Logik
 * @see KI
 * @see Netzwerkspiel
 */
public class GUI extends JFrame {
    /**
     * Visuelle Darstellung des Spielerfeldes (linke Seite).
     * Der Spieler platziert hier seine Schiffe und sieht gegnerische Schüsse.
     */
    private Feld spielerFeld;
    
    /**
     * Visuelle Darstellung des Gegnerfeldes (rechte Seite).
     * Der Spieler schießt hier und sieht die Ergebnisse seiner Schüsse.
     */
    private Feld gegnerFeld;
    
    /**
     * Button zum Speichern und Zurückkehr zum Hauptmenü.
     */
    private JButton menueButton;
    
    /**
     * Statuszeile am oberen Rand, die Spielinformationen anzeigt.
     */
    private JLabel statusLabel;
    
    /**
     * Button zum Wechseln der Schiffs-Ausrichtung (Horizontal/Vertikal).
     */
    private JButton drehButton;
    
    /**
     * Referenz zur KI-Instanz (nur im Singleplayer-Modus).
     */
    private KI ki;
    
    /**
     * Zentrale Spiellogik mit Feldverwaltung und Regelproblematik.
     */
    private Logik spiellogik;
    
    /**
     * Referenz zur Netzwerk-Verwaltung (nur im Netzwerk-Modus).
     */
    private Netzwerkspiel netzwerkSpiel;
    
    /**
     * Zeilenindex des letzten Spieler-Klicks (für Schussverarbeitung).
     */
    private int letzterKlickReihe = -1;
    
    /**
     * Spaltenindex des letzten Spieler-Klicks (für Schussverarbeitung).
     */
    private int letzterKlickSpalte = -1;
    
    /**
     * Flag zur Kontrolle der Bot-vs-Bot-Schleife.
     */
    private boolean botSchleifeAktiv = true;
    
    /**
     * Spielmodus als String ("SINGLEPLAYER", "CPUVSCPU", "NETZWERK").
     */
    private String spielmodus = "SINGLEPLAYER";
    
    /**
     * Flag zur Verhinderung von Überlappungen bei Pop-up-Anzeigen.
     */
    private boolean popupAktiv = false;

    /**
     * Standardkonstruktor mit Spielfeldgröße 10x10.
     */
    public GUI() {
        this(10);
    }

    /**
     * Hauptkonstruktor mit flexibler Spielfeldgröße.
     * 
     * Erstellt die GUI mit:
     * - Zwei NxN-Spielfeldern (nebeneinander)
     * - Statuszeile mit Spielinformationen
     * - Steuerungs-Panel mit Ausrichtungs- und Menü-Buttons
     * - Vorinitialisierter Logik mit harmonischer Flottenverteilung
     * 
     * Das gegnerische Feld startet gesperrt (nicht klickbar), wird aber
     * nach Abschluss der Schiffsplatzierung freigegeben.
     * 
     * @param gewaehlteGroesse Die Größe des quadratischen Spielfeldes
     */
    public GUI(int gewaehlteGroesse) {
        spiellogik = new Logik(gewaehlteGroesse); //Logik mit Wunschgröße starten

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

        //Das eigene Spielfeld wird mit der dynamischen Größe erstellt
        spielerFeld = new Feld("Dein Spielfeld", gewaehlteGroesse, true, e -> {
            Point koordinaten = (Point) e.getSource();
            schiffsPlatzierung(koordinaten.x, koordinaten.y);
        });
        
        //Das gegnerische Spielfeld wird mit der dynamischen Größe erstellt
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

        //Der Autosave- und Hauptmenü-Button
        menueButton = new JButton("Speichern und schliessen");
        menueButton.setFont(new Font("Arial", Font.PLAIN, 12));
        menueButton.addActionListener(e -> {
            this.botSchleifeAktiv = false; //Stoppt den Thread der Bots
            
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

    /**
     * Konstruktor für Singleplayer-Modus (Spieler vs. KI).
     * 
     * Initialisiert die GUI mit einer KI-Instanz und setzt das gegnerische Feld
     * mit den KI-Schiffen. Der Spielmodus wird auf "SINGLEPLAYER" gesetzt.
     * 
     * @param ki Die KI-Instanz als Gegner
     * @param gewaehlteGroesse Die Größe des Spielfeldes
     */
    public GUI(KI ki, int gewaehlteGroesse) {
        this(gewaehlteGroesse); 
        this.ki = ki;
        this.spielmodus = "SINGLEPLAYER";
        
        char[][] kiBoard = ki.getMyBoard();
        spiellogik.setGegnerFeld(kiBoard);
        aktualisiereStatusText();
    }

    /**
     * Setzt die Netzwerk-Verwaltung für Multiplayer-Spiele.
     * 
     * @param netzwerkSpiel Die Netzwerkspiel-Instanz für Multiplayer-Kommunikation
     */
    public void setNetzwerkSpiel(Netzwerkspiel netzwerkSpiel) {           
        this.netzwerkSpiel = netzwerkSpiel;
    }

    /**
     * Aktualisiert die Statuszeile mit aktuellen Spielinformationen.
     * 
     * Zeigt entweder die noch zu platzierenden Schiffe oder die "Kampfphase gestartet"-Meldung.
     */
    private void aktualisiereStatusText() {
        if (!spiellogik.alleSchiffePlatziert()) {
            statusLabel.setText("N\u00e4chstes Schiff: " + spiellogik.getAktuelleSchiffsLaenge() + " Felder.  |  " + spiellogik.getFlottenText());
        } else {
            statusLabel.setText("Alle Schiffe platziert! Kampfphase läuft.");
        }
    }

    /**
     * Verarbeitet die Platzierung eines Schiffes auf dem Spielerfeld.
     * 
     * Validiert die Position, visualisiert das Schiff mit grauer Färbung und
     * schaltet zur Kampfphase um, wenn alle Schiffe platziert wurden.
     * 
     * @param r Zeilenindex der Platzierung
     * @param c Spaltenindex der Platzierung
     */
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

    /**
     * Wechselt von der Platzierungsphase zur Kampfphase.
     * 
     * Deaktiviert den Ausrichtungs-Button und aktiviert das gegnerische Feld.
     * Im Netzwerk-Modus wird ein Ready-Signal gesendet und auf beide Spieler gewartet.
     */
    private void starteKampfphase() {
        aktualisiereStatusText();
        drehButton.setEnabled(false); //Deaktiviert den Ausrichtungs-Button, da er nicht mehr benötigt wird
        
        if (this.netzwerkSpiel != null) {
            //1. Dem Gegner über das Internet signalisieren, dass wir fertig sind
            this.netzwerkSpiel.sendeReadySignal();
            
            //2. Wir schalten das Feld NUR DANN frei, wenn BEIDE Seiten absolut fertig aufgestellt haben!
            if (this.netzwerkSpiel.sindBeideBereit()) {
                schalteGegnerFeldAktiv(this.netzwerkSpiel.istMeinZug());
            } else {
                schalteGegnerFeldAktiv(false);
                statusLabel.setText("Warte, bis der Spielpartner alle Schiffe platziert hat...");
            }
        } else {
            //Im normalen Singleplayer-Modus darf der Spieler nach der Setzphase sofort das gegnerfeld attackieren!
            gegnerFeld.setAktiv(true);
        }
    }

    /**
     * Zentrale Methode zur Verarbeitung aller Schüsse im Spiel.
     * 
     * Verarbeitet Schüsse, visualisiert Ergebnisse (Wasser/Treffer/Versenkt),
     * zeigt automatische Pop-ups bei versenkten Schiffen und erkennt Spielende.
     * 
     * @param r Zeilenindex des Schusses
     * @param c Spaltenindex des Schusses
     * @param schussAufGegnerFeld true für Schuss auf Gegner; false auf Spieler
     * @return true wenn Treffer (Extra-Zug); false wenn Wasser
     */
    public boolean verarbeiteSchussZentral(int r, int c, boolean schussAufGegnerFeld) {
        int ergebnis;
        Feld zielFeld = schussAufGegnerFeld ? gegnerFeld : spielerFeld;
        
        if (schussAufGegnerFeld) {
            ergebnis = spiellogik.schussAufGegner(r, c);
        } else {
            ergebnis = spiellogik.schussAufSpieler(r, c);
        }

        if (ergebnis == 1) { //Wasser getroffen
            zielFeld.setZellenFarbe(r, c, Color.BLUE);
            if (schussAufGegnerFeld) {
                statusLabel.setText("Bot 1 verfehlt bei Reihe " + (r + 1) + ", Spalte " + (c + 1));
            } else {
                statusLabel.setText("Bot 2 verfehlt bei Reihe " + (r + 1) + ", Spalte " + (c + 1));
            }
            spielende();
            return false; //Keine Extra-Runde bei Wasser
            
        } else if (ergebnis == 2) { //Schiff getroffen
            zielFeld.setZellenFarbe(r, c, Color.RED);
            if (schussAufGegnerFeld) {
                statusLabel.setText("CPU 1 landete einen TREFFER bei Reihe " + (r + 1) + ", Spalte " + (c + 1) + "!");
            } else {
                statusLabel.setText("CPU 2 landete einen TREFFER bei Reihe " + (r + 1) + ", Spalte " + (c + 1) + "!");
            }

            //Prüfen, ob das getroffene Schiff komplett zerstört wurde
            boolean komplettVersenkt = schussAufGegnerFeld ? 
                spiellogik.istGegnerSchiffVersenkt(r, c) : spiellogik.istSpielerSchiffVersenkt(r, c);

            if (komplettVersenkt) {
                boolean spielerFeldVorherAktiv = spielerFeld.isEnabled();
                boolean gegnerFeldVorherAktiv = gegnerFeld.isEnabled();
                
                spielerFeld.setAktiv(false);
                gegnerFeld.setAktiv(false);

                String meldungText = schussAufGegnerFeld ? 
                    "Ein gegnerisches Schiff wurde versenkt!" : "Die KI hat eines deiner Schiffe versenkt!";
                
                //Modusabhängige Weiche für modale Blockade
                boolean istModal = "CPUVSCPU".equals(this.spielmodus);
                JDialog autoPopup = new JDialog(this, "Treffer versenkt", istModal); 
                
                JLabel infoLabel = new JLabel(meldungText, SwingConstants.CENTER);
                infoLabel.setFont(new Font("Arial", Font.BOLD, 14));
                infoLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                autoPopup.add(infoLabel);
                autoPopup.pack();
                autoPopup.setLocationRelativeTo(this); 

                //Unabhängiger Schließ-Thread
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
                            if (!"CPUVSCPU".equals(this.spielmodus) && !schussAufGegnerFeld) {
                                gegnerFeld.setAktiv(true);
                            } else {
                                gegnerFeld.setAktiv(gegnerFeldVorherAktiv);
                            }
                        }
                    });
                }).start();

                autoPopup.setVisible(true); 
            }

            spielende();
            return true; //Extra-Runde bei Treffer!
        }
        
        return false; 
    }

    /**
     * Verarbeitet Angriffe des menschlichen Spielers auf das gegnerische Feld.
     * 
     * Überprüft Spielzustände, Zugteilung (besonders im Netzwerk-Modus),
     * validiert bereits beschossene Felder und delegiert an zentrale Schussverarbeitung.
     * 
     * @param r Zeilenindex des Angriffes
     * @param c Spaltenindex des Angriffes
     */
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
            statusLabel.setText("Feld schon gew\u00e4hlt");
            return;
        }

        if (this.netzwerkSpiel != null) {
            this.letzterKlickReihe = r;
            this.letzterKlickSpalte = c;
            this.netzwerkSpiel.spielerKlicktSpielfeld(r, c);
            gegnerFeld.setAktiv(false); 
            return;
        }

        //Normaler Singleplayer-Modus nutzt die zentrale Methode
        boolean treffer = verarbeiteSchussZentral(r, c, true);
        
        if (!treffer) {
            gegnerFeld.setAktiv(false); 
            kiZugAus();
        }
    }

    /**
     * Führt einen KI-Zug aus (Singleplayer-Modus).
     * 
     * Mit verzögertem Timer, berechnet die KI den nächsten Schuss, visualisiert ihn
     * und aktualisiert die KI-Logik mit dem Ergebnis. Nach einem Fehlschuss wird
     * das Gegnerfeld freigegeben, nach einem Treffer folgt ein automatischer weiterer Zug.
     */
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
                
                //Nutzt die KI-Logik, um den Zustand des Schusses zu aktualisieren
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

    /**
     * Startet eine automatische Bot-vs-Bot-Kampfschleife.
     * 
     * Zwei KI-Instanzen spielen abwechselnd gegeneinander mit 1 Sekunde Pause
     * zwischen den Zügen. Die Schleife läuft in einem separaten Thread und
     * kann durch das botSchleifeAktiv-Flag gestoppt werden.
     * 
     * @param bot1 Erste KI-Instanz
     * @param bot2 Zweite KI-Instanz
     */
    public void starteBotSchleife(KI bot1, KI bot2) {
        this.spielmodus = "CPUVSCPU";
        new Thread(() -> {
            boolean spielLaeuft = true;
            boolean bot1IstDran = true;
            this.botSchleifeAktiv = true; //Schleife aktivieren beim Start

            spielerFeld.setAktiv(false);
            gegnerFeld.setAktiv(false);

            statusLabel.setText("CPU vs CPU Kampfphase gestartet!");

            while (spielLaeuft && this.botSchleifeAktiv) {
                try {
                    Thread.sleep(1000); //1 Sekunde Pause zwischen den regulären Schüssen
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

    /**
     * Überprüft und zeigt das Spielende an.
     * 
     * Prüft auf Sieg oder Niederlage und zeigt entsprechende Pop-ups
     * mit Nachrichten und Status-Updates.
     * 
     * @return true wenn Spiel vorbei; false sonst
     */
    private boolean spielende() {
        if (spiellogik.sieg()) {
            statusLabel.setText("SIEG! Alle gegnerischen Schiffe wurden versenkt!");
            JOptionPane.showMessageDialog(this, "Das Spiel ist vorbei! Ein Sieger steht fest.", "Spiel vorbei", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } else if (spiellogik.kisieg()) {
            statusLabel.setText("NIEDERLAGE! Die Flotte wurde versenkt.");
            JOptionPane.showMessageDialog(this, "Das Spiel ist vorbei! Die Flotte wurde vernichtet.", "Spiel vorbei", JOptionPane.WARNING_MESSAGE);
            return true;
        }
        return false;
    }

    //NETZWERK-METHODEN & BRÜCKEN-METHODEN

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

    /**
     * Gibt die Spiellogik-Instanz zurück.
     * 
     * @return Die zentrale Spiellogik
     */
    public Logik getSpiellogik() {
        return spiellogik;
    }

    /**
     * Gibt das Spielerfeld zurück.
     * 
     * @return Das Spielerfeld-Panel
     */
    public Feld getSpielerFeld() {
        return spielerFeld;
    }

    /**
     * Gibt das Gegnerfeld zurück.
     * 
     * @return Das Gegnerfeld-Panel
     */
    public Feld getGegnerFeld() {
        return gegnerFeld;
    }

    public void initialisiereSpielfeld(int zeilen, int spalten) {
        System.out.println("Netzwerk: Spielfeld initialisiert auf " + zeilen + "x" + spalten);
    }

    public void generiereSchiffsFlotte(int[] laengen) {
        System.out.println("Netzwerk: Flotte empfangen.");
    }

    /**
     * Prüft einen gegnerischen Schuss (Netzwerk-Modus).
     * 
     * Verarbeitet den Schuss auf das Spielerfeld, visualisiert das Ergebnis
     * und gibt ein Ergebnis-Flag zurückkehr (0=Wasser, 1=Treffer, 2=Versenkt).
     * 
     * @param r Zeilenindex des gegnerischen Schusses
     * @param c Spaltenindex des gegnerischen Schusses
     * @return Ergebnis-Code: 0=Wasser, 1=Treffer, 2=Versenkt
     */
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
                    JOptionPane.showMessageDialog(this, "Die KI/Gegner hat eines deiner Schiffe versenkt!", "Treffer versenkt", JOptionPane.WARNING_MESSAGE);
                });
                popupTimer.setRepeats(false);
                popupTimer.start();
                
                return 2; 
            }
            return 1; 
        }
        return 0; 
    }

    /**
     * Visualisiert das Ergebnis eines eigenen Schusses (Netzwerk-Modus).
     * 
     * Empfängt die Antwort vom Gegner, färbt das Gegnerfeld entsprechend
     * und zeigt Pop-ups bei versenkten Schiffen. Steuert die Zugteilung
     * basierend auf dem Ergebnis.
     * 
     * @param ergebnis Das Schuss-Ergebnis vom Gegner (0=Wasser, 1=Treffer, 2=Versenkt)
     */
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

                    statusLabel.setText("VERSENKT! Du hast ein gegnerisches Schiff versenkt!");
                    
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

    /**
     * Zeigt eine Fehlermeldung an, wenn die Netzwerkverbindung verloren geht.
     */
    public void zeigeVerbindungVerlorenMeldung() {
        JOptionPane.showMessageDialog(this, "Verbindung zum Netzwerk-Spielpartner verloren!", "Fehler", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Aktiviert oder deaktiviert das gegnerische Feld basierend auf Zugteilung.
     * 
     * Aktualisiert auch die Statuszeile mit entsprechenden Meldungen.
     * 
     * @param istMeinZug true wenn dieser Spieler am Zug ist; false sonst
     */
    public void schalteGegnerFeldAktiv(boolean istMeinZug) {
        gegnerFeld.setAktiv(istMeinZug);
        if (istMeinZug) {
            statusLabel.setText("Du bist am Zug! Klicke auf das gegnerische Feld.");
        } else {
            statusLabel.setText("Gegner ist am Zug... Bitte warten.");
        }
    }

    /**
     * Lädt und visualisiert einen gespeicherten Spielstand.
     * 
     * Zeigt alle bisherigen Schüsse und Treffermarkierungen auf beiden Feldern
     * und schaltet in die Kampfphase um.
     */
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