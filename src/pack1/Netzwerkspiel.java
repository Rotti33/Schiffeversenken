package pack1;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import javax.swing.SwingUtilities;

/**
 * Verwaltet ein Netzwerk-basiertes Schiffe-versenken-Spiel.
 * 
 * Diese Klasse koordiniert die Kommunikation zwischen zwei Spielern über ein Netzwerk.
 * Sie kann als Server oder Client fungieren und unterstützt sowohl Spieler-gegen-Spieler
 * als auch KI-gegen-Spieler-Modi über Netzwerk.
 * 
 * Die Klasse verwaltet den Spielzustand, die Zugteilung, die Bereitschaftsstatus und
 * delegiert Netzwerknachrichten an die GUI und die Spiellogik.
 * 
 * @author Lisa Renner, Rodrigo Malisi Sousa
 * @version 1.0
 * @see Netzwerkprotokoll
 * @see GUI
 * @see KI
 */
public class Netzwerkspiel {
    
    /**
     * Referenz zur Benutzeroberfläche des Spiels.
     */
    private GUI gui; 
    
    /**
     * Die KI-Instanz für KI-gesteuerte Züge.
     */
    private final KI ki;   
    
    /**
     * Handler für Netzwerk-Protokoll-Kommunikation.
     */
    private Netzwerkprotokoll protokollHandler; 
    
    /**
     * Flag: true wenn dieser Spieler gerade am Zug ist.
     */
    private boolean ichBinAmZug = false; 
    
    /**
     * Flag: true wenn der KI-Modus gegen einen Netzwerk-Gegner aktiv ist.
     */
    private boolean modusKiGegenGegner = false; 
    
    /**
     * Flag: true wenn dieser Spieler bereit ist, das Spiel zu starten.
     */
    private boolean ichBinBereit = false; 
    
    /**
     * Flag: true wenn der Gegner bereit ist, das Spiel zu starten.
     */
    private boolean gegnerIstBereit = false; 

    /**
     * Konstruktor für ein Netzwerkspiel.
     * 
     * Initialisiert das Netzwerkspiel mit einer GUI und einer KI-Instanz.
     * Die GUI wird registriert, um Netzwerk-Spielvorgänge zu koordinieren.
     * 
     * @param gui Die Benutzeroberfläche des Spiels (kann null sein)
     * @param ki Die KI-Instanz für KI-Züge
     */
    public Netzwerkspiel(GUI gui, KI ki) {
        this.gui = gui;
        this.ki = ki;
        if (this.gui != null) {
            this.gui.setNetzwerkSpiel(this);
        }
    }

    /**
     * Startet die Netzwerk-Verbindung und initialisiert die Protokoll-Kommunikation.
     * 
     * Diese Methode wird in einem separaten Thread ausgeführt und verarbeitet
     * alle eingehenden Nachrichten vom Gegner. Sie fungiert als Server oder Client,
     * abhängig vom Parameter alsServer.
     * 
     * @param socket Die Netzwerk-Socket-Verbindung zum Gegner
     * @param alsServer true wenn dieser Spieler als Server fungiert; false für Client
     */
    public void starteNetzwerkVerbindung(Socket socket, boolean alsServer) {
        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                this.protokollHandler = new Netzwerkprotokoll(reader, writer) {
                    
                    @Override
                    protected void verarbeiteSpielfeldGroesse(int zeilen, int spalten) {
                        if (!alsServer) {
                            SwingUtilities.invokeLater(() -> {
                                Netzwerkspiel.this.gui = new GUI(zeilen); 
                                Netzwerkspiel.this.gui.setNetzwerkSpiel(Netzwerkspiel.this);
                                Netzwerkspiel.this.gui.initialisiereSpielfeld(zeilen, spalten);
                            });
                        } else {
                            SwingUtilities.invokeLater(() -> gui.initialisiereSpielfeld(zeilen, spalten));
                        }
                        protokollHandler.sendeNachricht("done");
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    protected void verarbeiteSchiffsFlotte(int[] laengen) {
                        if (!alsServer && Netzwerkspiel.this.gui != null) {
                            Logik logik = Netzwerkspiel.this.gui.getSpiellogik();
                            try {
                                java.lang.reflect.Field fFlotte = Logik.class.getDeclaredField("verbleibendeFlotte");
                                fFlotte.setAccessible(true); 
                                java.util.List<Integer> clientFlotte = (java.util.List<Integer>) fFlotte.get(logik);
                                
                                clientFlotte.clear(); 
                                int gesamtSegmente = 0;
                                for (int laenge : laengen) {
                                    clientFlotte.add(laenge);
                                    gesamtSegmente += laenge;
                                }
                                java.lang.reflect.Field fZiel = Logik.class.getDeclaredField("zielBelegteFelder");
                                fZiel.setAccessible(true); 
                                fZiel.set(logik, gesamtSegmente);
                                
                                clientFlotte.sort((a, b) -> b - a);
                                
                                SwingUtilities.invokeLater(() -> Netzwerkspiel.this.gui.generiereSchiffsFlotte(laengen));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            SwingUtilities.invokeLater(() -> gui.generiereSchiffsFlotte(laengen));
                        }
                        protokollHandler.sendeNachricht("done");
                    }

                    @Override
                    protected void verarbeiteDone() {
                        System.out.println("Gegner hat Daten bestätigt (done).");
                    }

                    @Override
                    protected void verarbeiteReady() {
                        System.out.println("Gegner ist spielbereit (ready).");
                        Netzwerkspiel.this.gegnerIstBereit = true; 
                        
                        if (Netzwerkspiel.this.ichBinBereit && Netzwerkspiel.this.gegnerIstBereit) {
                            SwingUtilities.invokeLater(() -> gui.schalteGegnerFeldAktiv(Netzwerkspiel.this.ichBinAmZug));
                        }
                    }
                    @Override
                    protected void verarbeiteSchuss(int zeile, int spalte) {
                        int r = zeile - 1;
                        int c = spalte - 1;

                        int ergebnis;
                        if (Netzwerkspiel.this.modusKiGegenGegner) {
                            // BEHOBEN: Nutzt jetzt Spielzustand.ShotResult statt KI.ShotResult
                            Spielzustand.ShotResult res = Netzwerkspiel.this.ki.empfangeSchuss(r, c);
                            if (res == Spielzustand.ShotResult.WASSER) ergebnis = 0;
                            else if (res == Spielzustand.ShotResult.TREFFER) ergebnis = 1;
                            else ergebnis = 2; 
                        } else {
                            ergebnis = gui.pruefeGegnerSchuss(r, c);
                        }

                        protokollHandler.sendeNachricht("answer", ergebnis);

                        if (ergebnis == 0) {
                            protokollHandler.sendeNachricht("pass");
                            Netzwerkspiel.this.ichBinAmZug = true; 
                            SwingUtilities.invokeLater(() -> gui.schalteGegnerFeldAktiv(true));
                        } else {
                            Netzwerkspiel.this.ichBinAmZug = false;
                            SwingUtilities.invokeLater(() -> gui.schalteGegnerFeldAktiv(false));
                        }
                    }

                    @Override
                    protected void verarbeiteAntwort(int ergebnis) {
                        SwingUtilities.invokeLater(() -> gui.visuelleSchussRueckmeldung(ergebnis));
                        
                        // BEHOBEN: Aktualisiert die KI mit den Enums aus Spielzustand
                        if (Netzwerkspiel.this.modusKiGegenGegner) {
                            if (ergebnis == 0) Netzwerkspiel.this.ki.update(Spielzustand.ShotResult.WASSER);
                            else if (ergebnis == 1) Netzwerkspiel.this.ki.update(Spielzustand.ShotResult.TREFFER);
                            else Netzwerkspiel.this.ki.update(Spielzustand.ShotResult.VERSENKT);
                        }

                        if (ergebnis > 0) {
                            Netzwerkspiel.this.ichBinAmZug = true;
                            SwingUtilities.invokeLater(() -> gui.schalteGegnerFeldAktiv(true));
                            if (Netzwerkspiel.this.modusKiGegenGegner) {
                                Netzwerkspiel.this.triggerKiZug();
                            }
                        } else {
                            Netzwerkspiel.this.ichBinAmZug = false;
                            SwingUtilities.invokeLater(() -> gui.schalteGegnerFeldAktiv(false));
                        }
                    }

                    @Override
                    protected void verarbeitePass() {
                        Netzwerkspiel.this.ichBinAmZug = false; 
                        SwingUtilities.invokeLater(() -> gui.schalteGegnerFeldAktiv(false));
                    }

                    @Override
                    protected void verarbeiteSpeichern(long id) {
                        System.out.println("Speicherbefehl vom Gegner mit ID: " + id);
                        protokollHandler.sendeNachricht("ok");
                    }

                    @Override
                    protected void verarbeiteLaden(long id) {
                        System.out.println("Ladebefehl vom Gegner mit ID: " + id);
                        protokollHandler.sendeNachricht("ok");
                    }

                    @Override
                    protected void verarbeiteOk() {
                        System.out.println("Gegner hat OK gesendet.");
                    }

                    @Override
                    protected void verarbeiteVerbindungGeschlossen() {
                        System.out.println("Verbindung verloren oder Spiel abgebrochen.");
                        SwingUtilities.invokeLater(() -> {
                            if (gui != null) gui.zeigeVerbindungVerlorenMeldung();
                        });
                    }
                }; 

                if (alsServer) {
                    Netzwerkspiel.this.ichBinAmZug = true; 
                    int sGroesse = gui.getSpiellogik().getGroesse();
                    Netzwerkspiel.this.protokollHandler.sendeNachricht("size", sGroesse, sGroesse);
                    
                    try {
                        java.lang.reflect.Field fFlotte = Logik.class.getDeclaredField("verbleibendeFlotte");
                        fFlotte.setAccessible(true); 
                        
                        @SuppressWarnings("unchecked")
                        java.util.List<Integer> serverFlotte = (java.util.List<Integer>) fFlotte.get(gui.getSpiellogik());
                        
                        Object[] parameter = new Object[serverFlotte.size()];
                        for (int i = 0; i < serverFlotte.size(); i++) {
                            parameter[i] = serverFlotte.get(i);
                        }
                        Netzwerkspiel.this.protokollHandler.sendeNachricht("ships", parameter);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Netzwerkspiel.this.protokollHandler.sendeNachricht("ships", 5, 4, 3, 3, 2);
                    }
                }

                boolean laeuft = true;
                while (laeuft) {
                    laeuft = Netzwerkspiel.this.protokollHandler.verarbeiteNaechsteNachricht();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Verarbeitet einen Klick des Spielers auf das gegnerische Spielfeld.
     * 
     * Sendet den Schuss an den Gegner über das Netzwerk, falls der Spieler am Zug ist
     * und nicht im KI-Modus spielt.
     * 
     * @param r Zeilenindex des geklickten Feldes (0-basiert)
     * @param c Spaltenindex des geklickten Feldes (0-basiert)
     */
    public void spielerKlicktSpielfeld(int r, int c) {
        if (ichBinAmZug && !modusKiGegenGegner && protokollHandler != null) {
            protokollHandler.sendeNachricht("shot", r + 1, c + 1); 
            ichBinAmZug = false;
            if (gui != null) {
                SwingUtilities.invokeLater(() -> gui.schalteGegnerFeldAktiv(false));
            }
        }
    }

    /**
     * Triggert einen KI-Zug und sendet ihn über das Netzwerk.
     * 
     * Diese Methode wird aufgerufen, wenn die KI an der Reihe ist, einen Schuss abzufeuern.
     * Sie berechnet den nächsten Schuss basierend auf der KI-Logik und sendet ihn an den Gegner.
     */
    private void triggerKiZug() {
        if (ki != null && protokollHandler != null) {
            KI.Koordinaten koordinaten = ki.getNextShot();
            protokollHandler.sendeNachricht("shot", koordinaten.x + 1, koordinaten.y + 1);
            ichBinAmZug = false;
        }
    }

    /**
     * Initiiert das Speichern des aktuellen Spielstands.
     * 
     * Sendet einen Speicherbefehl an den Gegner mit einer eindeutigen ID basierend
     * auf dem aktuellen Zeitstempel.
     * 
     * @param gewaehlterDateipfad Der Dateipfad für den Speicherort (wird intern verarbeitet)
     */
    public void initiiereSpeichern(String gewaehlterDateipfad) {
        long id = System.currentTimeMillis();
        if (protokollHandler != null) {
            protokollHandler.sendeNachricht("save", id);
        }
    }

    /**
     * Sendet ein Bereitschaftssignal an den Gegner.
     * 
     * Signalisiert, dass dieser Spieler bereit ist, das Spiel zu beginnen.
     * Das Spiel startet, wenn beide Spieler bereit sind.
     */
    public void sendeReadySignal() {
        this.ichBinBereit = true; 
        if (protokollHandler != null) {
            protokollHandler.sendeNachricht("ready");
        }
    }

    /**
     * Prüft, ob dieser Spieler gerade am Zug ist.
     * 
     * @return true wenn dieser Spieler am Zug ist; false sonst
     */
    public boolean istMeinZug() {
        return this.ichBinAmZug;
    }

    /**
     * Prüft, ob beide Spieler bereit sind, das Spiel zu beginnen.
     * 
     * @return true wenn beide Spieler bereit sind; false sonst
     */
    public boolean sindBeideBereit() {
        return this.ichBinBereit && this.gegnerIstBereit;
    }
}