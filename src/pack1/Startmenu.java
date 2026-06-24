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

        // Panel für die vier Spielmodus-Buttons im Zentrum (GridLayout auf 4 Zeilen erhöht)
        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 10, 15));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 50, 20, 50));

        JButton btnSpielerVsBot = new JButton("Spieler vs. Bot");
        JButton btnBotVsBot = new JButton("Bot vs. Bot");
        JButton btnSpielerVsSpieler = new JButton("Spieler vs. Spieler (Netzwerk)");
        JButton btnSpielLaden = new JButton("Letztes Spiel laden"); // NEU: Laden-Button

        // Schriftgröße der Buttons anpassen
        Font buttonFont = new Font("Arial", Font.PLAIN, 16);
        btnSpielerVsBot.setFont(buttonFont);
        btnBotVsBot.setFont(buttonFont);
        btnSpielerVsSpieler.setFont(buttonFont);
        btnSpielLaden.setFont(buttonFont);

        // --- Logik für die Buttons ---

        // 1. Spieler vs. Bot
        btnSpielerVsBot.addActionListener(e -> {
            KI ki = new KI();
            new GUI(ki);       
            this.dispose();    
        });

        // 2. Bot vs. Bot
        btnBotVsBot.addActionListener(e -> {
            KI bot1 = new KI();
            KI bot2 = new KI();
            GUI gui = new GUI(); 
            this.dispose();
            
            gui.getSpiellogik().setGegnerFeld(bot2.getMyBoard()); 
            
            char[][] board1 = bot1.getMyBoard();
            for (int r = 0; r < 10; r++) {
                for (int c = 0; c < 10; c++) {
                    if (board1[r][c] == 'S') {
                        gui.setSpielerSchiffManuell(r, c);
                        gui.getSpielerFeld().setZellenFarbe(r, c, Color.GRAY);
                    }
                }
            }

            char[][] board2 = bot2.getMyBoard();
            for (int r = 0; r < 10; r++) {
                for (int c = 0; c < 10; c++) {
                    if (board2[r][c] == 'S') {
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

            if (wahl == 0) { 
                new Thread(() -> {
                    try {
                        System.out.println("Server gestartet, warte auf Client auf Port 5000...");
                        ServerSocket serverSocket = new ServerSocket(5000);
                        Socket socket = serverSocket.accept();
                        
                        GUI gui = new GUI();
                        spiel netzwerkSpiel = new spiel(gui, null);
                        netzwerkSpiel.starteNetzwerkVerbindung(socket, true); 
                        this.dispose();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(this, "Fehler beim Erstellen des Servers!");
                    }
                }).start();
            } else if (wahl == 1) { 
                String ip = JOptionPane.showInputDialog(this, "Gib die IP-Adresse des Servers ein:", "127.0.0.1");
                if (ip != null && !ip.isEmpty()) {
                    new Thread(() -> {
                        try {
                            System.out.println("Verbinde mit Server " + ip + " auf Port 5000...");
                            Socket socket = new Socket(ip, 5000);
                            
                            GUI gui = new GUI();
                            spiel netzwerkSpiel = new spiel(gui, null);
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

        // 4. Letztes Spiel laden (NEU: Liest die gespeicherte Datei wieder ein)
 btnSpielLaden.addActionListener(e -> {
            try {
                BufferedReader reader = new BufferedReader(new FileReader("autosave_singleplayer.txt"));
                int[][] geladenesSpielerFeld = new int[10][10];
                int[][] geladenesGegnerFeld = new int[10][10];
                int sTreffer = 0;
                int kTreffer = 0;

                // Liest zuerst den gespeicherten Modus aus
                String modusZeile = reader.readLine(); 
                boolean warBotVsBot = modusZeile.contains("BOTVSBOT");

                reader.readLine(); // "Spielerfeld:"
                for (int r = 0; r < 10; r++) {
                    String zeile = reader.readLine();
                    String[] werte = zeile.trim().split(" ");
                    for (int c = 0; c < 10; c++) {
                        geladenesSpielerFeld[r][c] = Integer.parseInt(werte[c]);
                        if (geladenesSpielerFeld[r][c] == 3) kTreffer++; // KI-Treffer zählen
                    }
                }

                reader.readLine(); // "Gegnerfeld:"
                for (int r = 0; r < 10; r++) {
                    String zeile = reader.readLine();
                    String[] werte = zeile.trim().split(" ");
                    for (int c = 0; c < 10; c++) {
                        geladenesGegnerFeld[r][c] = Integer.parseInt(werte[c]);
                        if (geladenesGegnerFeld[r][c] == 3) sTreffer++; // Spieler-Treffer zählen
                    }
                }
                reader.close();

                // Startet das Spiel passend zum geladenen Modus neu
                if (warBotVsBot) {
                    KI bot1 = new KI();
                    KI bot2 = new KI();
                    GUI gui = new GUI(); // Leere GUI ohne Spieler-KI
                    
                    // Daten in die Logik füttern und zeichnen
                    gui.getSpiellogik().ladeSpielfeldManuell(geladenesSpielerFeld, geladenesGegnerFeld, sTreffer, kTreffer);
                    gui.ladeSpielstandVisuell();
                    
                    // BEHOBEN: Klicks für den Menschen komplett abschalten, damit du nicht steuern kannst!
                    gui.getSpielerFeld().setAktiv(false);
                    gui.getGegnerFeld().setAktiv(false);
                    
                    // Bot-Schleife mit den geladenen Daten sofort wieder anwerfen
                    gui.starteBotSchleife(bot1, bot2);
                } else {
                    // Normaler Singleplayer-Modus
                    KI ki = new KI(); 
                    GUI gui = new GUI(ki);
                    gui.getSpiellogik().ladeSpielfeldManuell(geladenesSpielerFeld, geladenesGegnerFeld, sTreffer, kTreffer);
                    gui.ladeSpielstandVisuell();
                }
                
                this.dispose(); // Menü schließen
                System.out.println("Spielstand erfolgreich geladen!");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Kein gespeicherter Spielstand gefunden!", "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttonPanel.add(btnSpielerVsBot);
        buttonPanel.add(btnBotVsBot);
        buttonPanel.add(btnSpielerVsSpieler);
        buttonPanel.add(btnSpielLaden); // Button dem Panel hinzufügen
        add(buttonPanel, BorderLayout.CENTER);

        // Fenstergröße angepasst für 4 Buttons
        setSize(400, 420);
        setLocationRelativeTo(null);
        setVisible(true);
    }
}