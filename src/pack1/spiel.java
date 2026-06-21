package pack1;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.SwingUtilities;

public class spiel {
    
    private final GUI gui; 
    private final KI ki;   
    private protokoll protokollHandler;
    
    private boolean ichBinAmZug = false; 
    private boolean modusKiGegenGegner = false; // true = KI spielt vollautomatisch, false = Mensch klickt auf GUI

    public spiel(GUI gui, KI ki) {
        this.gui = gui;
        this.ki = ki;
        // Wichtig: Der GUI sagen, dass ein Netzwerkspiel läuft!
        this.gui.setNetzwerkSpiel(this);
    }

    public void starteNetzwerkVerbindung(Socket socket, boolean alsServer) {
        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                this.protokollHandler = new protokoll(reader, writer) {
                    
                    @Override
                    protected void verarbeiteSpielfeldGroesse(int zeilen, int spalten) {
                        SwingUtilities.invokeLater(() -> gui.initialisiereSpielfeld(zeilen, spalten));
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
                        // Schaltet das gegnerische Spielfeld aktiv, falls wir am Zug sind
                        SwingUtilities.invokeLater(() -> gui.schalteGegnerFeldAktiv(spiel.this.ichBinAmZug));
                    }

                    @Override
                    protected void verarbeiteSchuss(int zeile, int spalte) {
                        int r = zeile - 1;
                        int c = spalte - 1;

                        int ergebnis;
                        if (spiel.this.modusKiGegenGegner) {
                            KI.ShotResult res = spiel.this.ki.empfangeSchuss(r, c);
                            
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

                        if (ergebnis == 0) {
                            protokollHandler.sendeNachricht("pass");
                            spiel.this.ichBinAmZug = true;
                            // Feld für uns wieder aktivieren, da der Gegner vorbei geschossen hat
                            SwingUtilities.invokeLater(() -> gui.schalteGegnerFeldAktiv(true));
                            if (spiel.this.modusKiGegenGegner) {
                                spiel.this.triggerKiZug();
                            }
                        }
                    }

                    @Override
                    protected void verarbeiteAntwort(int ergebnis) {
                        // Feedback an GUI senden
                        SwingUtilities.invokeLater(() -> gui.visuelleSchussRueckmeldung(ergebnis));
                        
                        // Wenn KI im Netzwerkmodus spielt, füttern wir ihr Update-System
                        if (spiel.this.modusKiGegenGegner) {
                            if (ergebnis == 0) spiel.this.ki.update(KI.ShotResult.WASSER);
                            else if (ergebnis == 1) spiel.this.ki.update(KI.ShotResult.TREFFER);
                            else spiel.this.ki.update(KI.ShotResult.VERSENKT);
                        }

                        if (ergebnis > 0) {
                            spiel.this.ichBinAmZug = true;
                            // Bei einem Treffer bleiben wir am Zug, Feld aktiv lassen
                            SwingUtilities.invokeLater(() -> gui.schalteGegnerFeldAktiv(true));
                            if (spiel.this.modusKiGegenGegner) {
                                spiel.this.triggerKiZug();
                            }
                        }
                    }

                    @Override
                    protected void verarbeitePass() {
                        spiel.this.ichBinAmZug = true; 
                        // Wir erhalten den Zug per 'pass' zurück, Feld wieder freigeben
                        SwingUtilities.invokeLater(() -> gui.schalteGegnerFeldAktiv(true));
                        if (spiel.this.modusKiGegenGegner) {
                            spiel.this.triggerKiZug();
                        }
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
                        SwingUtilities.invokeLater(() -> gui.zeigeVerbindungVerlorenMeldung());
                    }
                };

                if (alsServer) {
                    spiel.this.ichBinAmZug = true; // Der Server darf anfangen zu schießen
                    spiel.this.protokollHandler.sendeNachricht("size", 10, 10);
                    spiel.this.protokollHandler.sendeNachricht("ships", 5, 4, 4, 3, 3, 3, 2, 2, 2, 2);
                }

                boolean laeuft = true;
                while (laeuft) {
                    laeuft = spiel.this.protokollHandler.verarbeiteNaechsteNachricht();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void spielerKlicktSpielfeld(int r, int c) {
        if (ichBinAmZug && !modusKiGegenGegner) {
            protokollHandler.sendeNachricht("shot", r + 1, c + 1); 
            ichBinAmZug = false;
            // Nach unserem Schuss das Feld direkt sperren, bis die Antwort oder der 'pass' kommt
            SwingUtilities.invokeLater(() -> gui.schalteGegnerFeldAktiv(false));
        }
    }

    private void triggerKiZug() {
        KI.Koordinaten koordinaten = ki.getNextShot();
        protokollHandler.sendeNachricht("shot", koordinaten.x + 1, koordinaten.y + 1);
        ichBinAmZug = false;
    }

    public void initiiereSpeichern(String gewaehlterDateipfad) {
        long id = System.currentTimeMillis();
        if (protokollHandler != null) {
            protokollHandler.sendeNachricht("save", id);
        }
    }

    public void sendeReadySignal() {
        if (protokollHandler != null) {
            protokollHandler.sendeNachricht("ready");
        }
    }
}