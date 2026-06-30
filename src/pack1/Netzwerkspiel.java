package pack1;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.SwingUtilities;

public class Netzwerkspiel {
    
    // BEHOBEN: GUI darf beim Client nicht mehr final im Konstruktor erzeugt werden,
    // sondern wird erst erschaffen, wenn der Server die echte Größe schickt!
    private GUI gui; 
    private final KI ki;   
    private protokoll protokollHandler;
    
    private boolean ichBinAmZug = false; 
    private boolean modusKiGegenGegner = false; // true = KI spielt vollautomatisch, false = Mensch klickt auf GUI
    private boolean ichBinBereit = false; // Hat dieser PC seine Schiffe fertig gesetzt?
    private boolean gegnerIstBereit = false; // Hat der Gegner seine Schiffe fertig gesetzt?

    // Konstruktor für den Server (Host) - GUI existiert bereits
    public Netzwerkspiel(GUI gui, KI ki) {
        this.gui = gui;
        this.ki = ki;
        if (this.gui != null) {
            this.gui.setNetzwerkSpiel(this);
        }
    }

    public void starteNetzwerkVerbindung(Socket socket, boolean alsServer) {
        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                this.protokollHandler = new protokoll(reader, writer) {
                    
                    @Override
                    protected void verarbeiteSpielfeldGroesse(int zeilen, int spalten) {
                        // BEHOBEN FÜR CLIENT: Wenn wir der Client sind, erschaffen wir die GUI
                        // JETZT ERST in der exakten Wunschgröße des Servers!
                        if (!alsServer) {
                            SwingUtilities.invokeLater(() -> {
                                Netzwerkspiel.this.gui = new GUI(zeilen); // Fenster passend bauen!
                                Netzwerkspiel.this.gui.setNetzwerkSpiel(Netzwerkspiel.this);
                                Netzwerkspiel.this.gui.initialisiereSpielfeld(zeilen, spalten);
                            });
                        } else {
                            SwingUtilities.invokeLater(() -> gui.initialisiereSpielfeld(zeilen, spalten));
                        }
                        protokollHandler.sendeNachricht("done");
                    }

                    @Override
                    protected void verarbeiteSchiffsFlotte(int[] laengen) {
                        SwingUtilities.invokeLater(() -> gui.generiereSchiffsFlotte(laengen));
                        protokollHandler.sendeNachricht("done");
                    }

                    @Override
                    protected void verarbeiteDone() {
                        System.out.println("Gegner hat Daten bestätigt (done).");
                    }

                    @Override
                    protected void verarbeiteReady() {
                        System.out.println("Gegner ist spielbereit (ready).");
                        Netzwerkspiel.this.gegnerIstBereit = true; // Gegner signalisiert Bereitschaft
                        
                        // Erst wenn beide bereit sind, wird das Spielfeld freigegeben
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
                            KI.ShotResult res = Netzwerkspiel.this.ki.empfangeSchuss(r, c);
                            
                            if (res == KI.ShotResult.WASSER) {
                                ergebnis = 0;
                            } else if (res == KI.ShotResult.TREFFER) {
                                ergebnis = 1;
                            } else {
                                ergebnis = 2; // VERSENKT
                            }
                        } else {
                            // Wenn der Mensch spielt, wertet die GUI über die Logik aus
                            ergebnis = gui.pruefeGegnerSchuss(r, c);
                        }

                        protokollHandler.sendeNachricht("answer", ergebnis);

                        // Wenn der Gegner UNSER Wasser trifft (ergebnis == 0),
                        // schicken wir ein pass an ihn und aktivieren UNSER eigenes Feld, weil WIR jetzt dran sind!
                        if (ergebnis == 0) {
                            protokollHandler.sendeNachricht("pass");
                            Netzwerkspiel.this.ichBinAmZug = true; // Der Gegner hat verfehlt, also sind WIR jetzt dran!
                            SwingUtilities.invokeLater(() -> gui.schalteGegnerFeldAktiv(true));
                        } else {
                            // Gegner hat uns getroffen, er bleibt am Zug. Unser Feld bleibt eisern gesperrt!
                            Netzwerkspiel.this.ichBinAmZug = false;
                            SwingUtilities.invokeLater(() -> gui.schalteGegnerFeldAktiv(false));
                        }
                    }

                    @Override
                    protected void verarbeiteAntwort(int ergebnis) {
                        // Feedback an GUI senden
                        SwingUtilities.invokeLater(() -> gui.visuelleSchussRueckmeldung(ergebnis));
                        
                        // Wenn KI im Netzwerkmodus spielt, füttern wir ihr Update-System
                        if (Netzwerkspiel.this.modusKiGegenGegner) {
                            if (ergebnis == 0) Netzwerkspiel.this.ki.update(KI.ShotResult.WASSER);
                            else if (ergebnis == 1) Netzwerkspiel.this.ki.update(KI.ShotResult.TREFFER);
                            else Netzwerkspiel.this.ki.update(KI.ShotResult.VERSENKT);
                        }

                        // Wenn wir getroffen haben (1 oder 2), bleiben wir am Zug!
                        if (ergebnis > 0) {
                            Netzwerkspiel.this.ichBinAmZug = true;
                            SwingUtilities.invokeLater(() -> gui.schalteGegnerFeldAktiv(true));
                            if (Netzwerkspiel.this.modusKiGegenGegner) {
                                Netzwerkspiel.this.triggerKiZug();
                            }
                        } else {
                            // Bei Wasser ist unser Zug im Moment der Antwort vorbei. 
                            // Wir blockieren unser Feld und warten auf das 'pass' des Gegners.
                            Netzwerkspiel.this.ichBinAmZug = false;
                            SwingUtilities.invokeLater(() -> gui.schalteGegnerFeldAktiv(false));
                        }
                    }

                    @Override
                    protected void verarbeitePass() {
                        // Wenn wir das 'pass' empfangen, bedeutet das, dass der Gegner 
                        // nun offiziell seinen Zug an uns abtritt, weil wir vorhin ins Wasser geschossen haben.
                        Netzwerkspiel.this.ichBinAmZug = false; // Unser Fehlschuss-Zug wird jetzt hier final beendet!
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
                }; // Hier wird der anonyme protokollHandler sauber geschlossen!

                if (alsServer) {
                    Netzwerkspiel.this.ichBinAmZug = true; // Der Server darf anfangen zu schießen
                    Netzwerkspiel.this.protokollHandler.sendeNachricht("size", gui.getSpiellogik().getGroesse(), gui.getSpiellogik().getGroesse());
                    Netzwerkspiel.this.protokollHandler.sendeNachricht("ships", 5, 4, 3, 3, 2);
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

    public void spielerKlicktSpielfeld(int r, int c) {
        if (ichBinAmZug && !modusKiGegenGegner && protokollHandler != null) {
            protokollHandler.sendeNachricht("shot", r + 1, c + 1); 
            ichBinAmZug = false;
            if (gui != null) {
                SwingUtilities.invokeLater(() -> gui.schalteGegnerFeldAktiv(false));
            }
        }
    }

    private void triggerKiZug() {
        if (ki != null && protokollHandler != null) {
            KI.Koordinaten koordinaten = ki.getNextShot();
            protokollHandler.sendeNachricht("shot", koordinaten.x + 1, koordinaten.y + 1);
            ichBinAmZug = false;
        }
    }

    public void initiiereSpeichern(String gewaehlterDateipfad) {
        long id = System.currentTimeMillis();
        if (protokollHandler != null) {
            protokollHandler.sendeNachricht("save", id);
        }
    }

    public void sendeReadySignal() {
        this.ichBinBereit = true; // WIR sind fertig!
        if (protokollHandler != null) {
            protokollHandler.sendeNachricht("ready");
        }
    }

    public boolean istMeinZug() {
        return this.ichBinAmZug;
    }

    public boolean sindBeideBereit() {
        return this.ichBinBereit && this.gegnerIstBereit;
    }
}