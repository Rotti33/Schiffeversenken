package pack1;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Startmenu extends JFrame {

    public Startmenu() {
        setTitle("Schiffe versenken - Hauptmenü");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Große Überschrift im Norden
        JLabel titelLabel = new JLabel("SCHIFFE VERSENKEN", SwingConstants.CENTER);
        titelLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titelLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        add(titelLabel, BorderLayout.NORTH);

        // Panel für die vier Spielmodus-Buttons im Zentrum
        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 10, 15));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 50, 20, 50));

        JButton btnSpielerVsBot = new JButton("Spieler vs. Bot");
        JButton btnBotVsBot = new JButton("Bot vs. Bot");
        JButton btnSpielerVsSpieler = new JButton("Spieler vs. Spieler (Netzwerk)");
        JButton btnSpielLaden = new JButton("Letztes Spiel laden"); 

        Font buttonFont = new Font("Arial", Font.PLAIN, 16);
        btnSpielerVsBot.setFont(buttonFont);
        btnBotVsBot.setFont(buttonFont);
        btnSpielerVsSpieler.setFont(buttonFont);
        btnSpielLaden.setFont(buttonFont);

        // 1. Spieler vs. Bot 
        btnSpielerVsBot.addActionListener(e -> {
            int groesse = frageSpielfeldGroesse();
            if (groesse == -1) return; 

            KI ki = new KI(groesse); 
            new GUI(ki, groesse);       
            this.dispose();    
        });

        // 2. Bot vs. Bot
        btnBotVsBot.addActionListener(e -> {
            int groesse = frageSpielfeldGroesse();
            if (groesse == -1) return; 

            KI bot1 = new KI(groesse);
            KI bot2 = new KI(groesse);
            GUI gui = new GUI(groesse); 
            this.dispose();
            
            gui.getSpiellogik().setGegnerFeld(bot2.getMyBoard()); 
            
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

        // 3. Spieler vs. Spieler (Netzwerk)
        btnSpielerVsSpieler.addActionListener(e -> {
            String[] optionen = {"Server erstellen (Host)", "Mit Server verbinden (Client)"};
            int wahl = JOptionPane.showOptionDialog(this, "Möchtest du ein Spiel hosten oder beitreten?", "Netzwerkmodus",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, optionen, optionen);

            if (wahl == 0) { // Server / Host
                int groesse = frageSpielfeldGroesse();
                if (groesse == -1) return;

                new Thread(() -> {
                    try {
                        System.out.println("Server gestartet, warte auf Client auf Port 5000...");
                        ServerSocket serverSocket = new ServerSocket(5000);
                        Socket socket = serverSocket.accept();
                        
                        GUI gui = new GUI(groesse);
                        Netzwerkspiel netzwerkSpiel = new Netzwerkspiel(gui, null);
                        netzwerkSpiel.starteNetzwerkVerbindung(socket, true); 
                        this.dispose();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(this, "Fehler beim Erstellen des Servers!");
                    }
                }).start();
            } else if (wahl == 1) { // Client / Beitreten
                String ip = JOptionPane.showInputDialog(this, "Gib die IP-Adresse des Servers ein:", "127.0.0.1");
                if (ip != null && !ip.isEmpty()) {
                    new Thread(() -> {
                        try {
                            System.out.println("Verbinde mit Server " + ip + " auf Port 5000...");
                            Socket socket = new Socket(ip, 5000);
                            
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

        // 4. Letztes Spiel laden
        btnSpielLaden.addActionListener(e -> {
            try {
                BufferedReader reader = new BufferedReader(new FileReader("autosave_singleplayer.txt"));
                String modusZeile = reader.readLine(); 
                boolean warBotVsBot = modusZeile.contains("BOTVSBOT");

                reader.readLine(); 
                
                int ermittelteGroesse = 0;
                reader.mark(10000);
                String testZeile = reader.readLine();
                if (testZeile != null && !testZeile.startsWith("Gegnerfeld:")) {
                    ermittelteGroesse = testZeile.trim().split(" ").length;
                }
                reader.reset();

                int[][] geladenesSpielerFeld = new int[ermittelteGroesse][ermittelteGroesse];
                int[][] geladenesGegnerFeld = new int[ermittelteGroesse][ermittelteGroesse];
                int sTreffer = 0;
                int kTreffer = 0;

                for (int r = 0; r < ermittelteGroesse; r++) {
                    String zeile = reader.readLine();
                    String[] werte = zeile.trim().split(" ");
                    for (int c = 0; c < ermittelteGroesse; c++) {
                        geladenesSpielerFeld[r][c] = Integer.parseInt(werte[c]);
                        if (geladenesSpielerFeld[r][c] == 3) kTreffer++; 
                    }
                }

                reader.readLine(); 
                for (int r = 0; r < ermittelteGroesse; r++) {
                    String zeile = reader.readLine();
                    String[] werte = zeile.trim().split(" ");
                    for (int c = 0; c < ermittelteGroesse; c++) {
                        geladenesGegnerFeld[r][c] = Integer.parseInt(werte[c]);
                        if (geladenesGegnerFeld[r][c] == 3) sTreffer++; 
                    }
                }
                reader.close();

                if (warBotVsBot) {
                    KI bot1 = new KI(ermittelteGroesse);
                    KI bot2 = new KI(ermittelteGroesse);
                    GUI gui = new GUI(ermittelteGroesse); 
                    gui.getSpiellogik().ladeSpielfeldManuell(geladenesSpielerFeld, geladenesGegnerFeld, sTreffer, kTreffer);
                    gui.ladeSpielstandVisuell();
                    gui.getSpielerFeld().setAktiv(false);
                    gui.getGegnerFeld().setAktiv(false);
                    gui.starteBotSchleife(bot1, bot2);
                } else {
                    KI ki = new KI(ermittelteGroesse); 
                    GUI gui = new GUI(ki, ermittelteGroesse);
                    gui.getSpiellogik().ladeSpielfeldManuell(geladenesSpielerFeld, geladenesGegnerFeld, sTreffer, kTreffer);
                    gui.ladeSpielstandVisuell();
                }
                
                this.dispose(); 
                System.out.println("Spielstand erfolgreich geladen!");
            } catch (Exception ex) {
                ex.printStackTrace();
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

    private int frageSpielfeldGroesse() {
        String eingabe = JOptionPane.showInputDialog(this, "Wähle die quadratische Spielfeldgröße (5 bis 30):", "Spielfeldgröße", JOptionPane.QUESTION_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Ungültige Eingabe! Bitte gib eine Zahl ein.", "Fehler", JOptionPane.ERROR_MESSAGE);
            return frageSpielfeldGroesse(); 
        }
    }
}