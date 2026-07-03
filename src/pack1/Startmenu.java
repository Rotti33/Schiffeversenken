package pack1;

import javax.swing.*;
import java.awt.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Startmenu extends JFrame {

    public Startmenu() {
        setTitle("Schiffe versenken - Hauptmen\u00fc");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        //Große Überschrift im Norden
        JLabel titelLabel = new JLabel("SCHIFFE VERSENKEN", SwingConstants.CENTER);
        titelLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titelLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        add(titelLabel, BorderLayout.NORTH);

        //Panel für die vier Spielmodus-Buttons im Zentrum
        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 10, 15));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 50, 20, 50));

        JButton btnSpielerVsBot = new JButton("P1 vs CPU");
        JButton btnBotVsBot = new JButton("CPU vs CPU");
        JButton btnSpielerVsSpieler = new JButton("P1 vs P2 (Netzwerk)");
        JButton btnSpielLaden = new JButton("Letztes Spiel laden"); 

        Font buttonFont = new Font("Arial", Font.PLAIN, 16);
        btnSpielerVsBot.setFont(buttonFont);
        btnBotVsBot.setFont(buttonFont);
        btnSpielerVsSpieler.setFont(buttonFont);
        btnSpielLaden.setFont(buttonFont);

        //1. Spieler vs. Bot 
        btnSpielerVsBot.addActionListener(e -> {
            //Öffnet ein Eingabefenster und fragt nach der Spielfeldgröße (5 bis 30)
            int groesse = frageSpielfeldGroesse();
            if (groesse == -1) return; //Abbrechen, wenn das Fenster geschlossen wird
            //KI wird erstellt mit der selben Spielfeldgröße
            KI ki = new KI(groesse); 
            new GUI(ki, groesse);       
            this.dispose();    
        });

        //2. Bot vs. Bot
        btnBotVsBot.addActionListener(e -> {
            int groesse = frageSpielfeldGroesse();
            if (groesse == -1) return; 

            KI bot1 = new KI(groesse);
            KI bot2 = new KI(groesse);
            GUI gui = new GUI(groesse); 
            this.dispose();
            //Verknüpft das Spielfeld von Bot 2 mit der Gegner-Logik der GUI           
            gui.getSpiellogik().setGegnerFeld(bot2.getMyBoard()); 
            //Überträgt die Flotte von Bot 1 grau gezeichnet auf das linke Feld          
            char[][] board1 = bot1.getMyBoard();
            int N = gui.getSpiellogik().getGroesse();
            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    if (r < board1.length && c < board1[r].length && board1[r][c] == 'S') {
                        gui.setSpielerSchiffManuell(r, c);
                        gui.getSpielerFeld().setZellenFarbe(r, c, Color.GRAY);
                    }
                }
            }
            // Überträgt die Flotte von Bot 2 grau gezeichnet auf das rechte Feld
            char[][] board2 = bot2.getMyBoard();
            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    if (r < board2.length && c < board2[r].length && board2[r][c] == 'S') {
                        gui.getGegnerFeld().setZellenFarbe(r, c, Color.GRAY);
                    }
                }
            }

            gui.starteBotSchleife(bot1, bot2);
        });

        //3. Spieler vs. Spieler (Netzwerkmodus)
        btnSpielerVsSpieler.addActionListener(e -> {
            String[] optionen = {"Server erstellen (Host)", "Mit Server verbinden (Client)"};
            int wahl = JOptionPane.showOptionDialog(this, "M\u00f6chtest du ein Spiel hosten oder beitreten?", "Netzwerkmodus",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, optionen, optionen);

            if (wahl == 0) { //Server / Host
                int groesse = frageSpielfeldGroesse();
                if (groesse == -1) return;

                //Startet den Server in einem eigenen Thread, damit die GUI beim Warten auf den Gegner nicht einfriert
                new Thread(() -> {
                    try {
                        System.out.println("Server gestartet, warte auf Client auf Port 5000...");
                        
                        // KORREKTUR: "java.net." davor geschrieben – so wird der Import oben überflüssig!
                        java.net.ServerSocket serverSocket = new java.net.ServerSocket(5000);
                        Socket socket = serverSocket.accept(); //Wartet hier, bis der zweite Spieler sich verbindet
                        
                        //Erstellt das Spielfeld in der gewählten Größe und startet die Netzwerk-Verbindung
                        GUI gui = new GUI(groesse);
                        Netzwerkspiel netzwerkSpiel = new Netzwerkspiel(gui, null);
                        netzwerkSpiel.starteNetzwerkVerbindung(socket, true); 
                        this.dispose(); //Schließt das Hauptmenü
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(this, "Fehler beim Erstellen des Servers!");
                    }
                }).start();
            } else if (wahl == 1) { //Client / Beitreten
                String ip = JOptionPane.showInputDialog(this, "Gib die IP-Adresse des Servers ein:", "127.0.0.1");
                if (ip != null && !ip.isEmpty()) {
                    //Auch der Client läuft im eigenen Thread, um Ruckler beim Verbindungsaufbau zu verhindern
                    new Thread(() -> {
                        try {
                            System.out.println("Verbinde mit Server " + ip + " auf Port 5000...");
                            Socket socket = new Socket(ip, 5000); //Baut die Verbindung zum Host auf
                            
                            //Startet das Netzwerkspiel für den Client
                            Netzwerkspiel netzwerkSpiel = new Netzwerkspiel(null, null);
                            netzwerkSpiel.starteNetzwerkVerbindung(socket, false); 
                            this.dispose();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(this, "Verbindung fehlgeschlagen!");
                        }
                    }).start();
                }
            }
        });

        // 4.Letztes Spiel laden
        btnSpielLaden.addActionListener(e -> {
            int[] datenMappe = new int[3];
            Logik geladenieLogik = Speicher.ladeSpiel("autosave_singleplayer.txt", datenMappe);

            if (geladenieLogik != null) {
                int groesse = geladenieLogik.getGroesse();
                boolean warBotVsBot = (datenMappe[2] == 1);

                if (warBotVsBot) {
                    KI bot1 = new KI(groesse);
                    KI bot2 = new KI(groesse);
                    GUI gui = new GUI(groesse); 
                    
                    try {
                        //Liest die versteckten Gitter-Daten aus der geladenen Logik aus
                        java.lang.reflect.Field fSpieler = Logik.class.getDeclaredField("spielerfeld");
                        java.lang.reflect.Field fGegner = Logik.class.getDeclaredField("gegnerfeld");
                        fSpieler.setAccessible(true); fGegner.setAccessible(true);
                        int[][] sFeld = (int[][]) fSpieler.get(geladenieLogik);
                        int[][] gFeld = (int[][]) fGegner.get(geladenieLogik);
                        //Überträgt die geladenen Daten inklusive Trefferstände in das neue Spiel
                        gui.getSpiellogik().ladeSpielfeldManuell(sFeld, gFeld, datenMappe[0], datenMappe[1]);
                    } catch(Exception ex) { ex.printStackTrace(); }
                    //Zeichnet die getroffenen und heilen Schiffe auf dem Bildschirm neu
                    gui.ladeSpielstandVisuell();
                    gui.getSpielerFeld().setAktiv(false);
                    gui.getGegnerFeld().setAktiv(false);
                    gui.starteBotSchleife(bot1, bot2);
                } else {
                    KI ki = new KI(groesse); 
                    GUI gui = new GUI(ki, groesse);
                    
                    try {
                        java.lang.reflect.Field fSpieler = Logik.class.getDeclaredField("spielerfeld");
                        java.lang.reflect.Field fGegner = Logik.class.getDeclaredField("gegnerfeld");
                        fSpieler.setAccessible(true); fGegner.setAccessible(true);
                        int[][] sFeld = (int[][]) fSpieler.get(geladenieLogik);
                        int[][] gFeld = (int[][]) fGegner.get(geladenieLogik);
                        gui.getSpiellogik().ladeSpielfeldManuell(sFeld, gFeld, datenMappe[0], datenMappe[1]);
                    } catch(Exception ex) { ex.printStackTrace(); }

                    gui.ladeSpielstandVisuell();
                }
                this.dispose(); 
            } else {
                JOptionPane.showMessageDialog(this, "Kein gespeicherter Spielstand gefunden!", "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttonPanel.add(btnSpielerVsBot);
        buttonPanel.add(btnBotVsBot);
        buttonPanel.add(btnSpielerVsSpieler);
        buttonPanel.add(btnSpielLaden); 
        add(buttonPanel, BorderLayout.CENTER);

        setSize(400, 420);
        setLocationRelativeTo(null);
        setVisible(true);
    }
    //Für die Abfrage der Spielfeldgröße, die der Spieler beim Start eines Spiels eingeben kann
    private int frageSpielfeldGroesse() {
        String eingabe = JOptionPane.showInputDialog(this, "W\u00e4hle die quadratische Spielfeldgr\u00f6ße (5 bis 30):", "Spielfeldgr\u00f6ße", JOptionPane.QUESTION_MESSAGE);
        if (eingabe == null || eingabe.isEmpty()) return -1;
        try {
            int g = Integer.parseInt(eingabe.trim());
            if (g >= 5 && g <= 30) {
                return g;
            } else {
                JOptionPane.showMessageDialog(this, "Erlaubt sind nur Werte zwischen 5 und 30!", "Fehler", JOptionPane.WARNING_MESSAGE);
                return frageSpielfeldGroesse(); 
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Ung\u00fcltige Eingabe! Bitte gib eine Zahl ein.", "Fehler", JOptionPane.ERROR_MESSAGE);
            return frageSpielfeldGroesse(); 
        }
    }
}