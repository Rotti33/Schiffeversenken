package pack1;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import javax.swing.SwingUtilities;

public class Netzwerkspiel {
    
    private GUI gui; 
    private final KI ki;   
    private Netzwerkprotokoll protokollHandler; 
    
    private boolean ichBinAmZug = false; 
    private boolean modusKiGegenGegner = false; 
    private boolean ichBinBereit = false; 
    private boolean gegnerIstBereit = false; 

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
        this.ichBinBereit = true; 
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