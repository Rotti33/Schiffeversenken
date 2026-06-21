package pack1;

import javax.swing.*;
import java.awt.*;
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

        // Panel für die drei Spielmodus-Buttons im Zentrum
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 10, 15));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 50, 20, 50));

        JButton btnSpielerVsBot = new JButton("Spieler vs. Bot");
        JButton btnBotVsBot = new JButton("Bot vs. Bot");
        JButton btnSpielerVsSpieler = new JButton("Spieler vs. Spieler (Netzwerk)");

        // Schriftgröße der Buttons anpassen
        Font buttonFont = new Font("Arial", Font.PLAIN, 16);
        btnSpielerVsBot.setFont(buttonFont);
        btnBotVsBot.setFont(buttonFont);
        btnSpielerVsSpieler.setFont(buttonFont);

        // --- Logik für die Buttons ---

        // 1. Spieler vs. Bot (Ihr bisheriger Modus)
        btnSpielerVsBot.addActionListener(e -> {
            KI ki = new KI();
            new GUI(ki);       // Startet Ihr bisheriges Spielfeld mit der KI
            this.dispose();    // Schließt das Menüfenster sauber im Hintergrund
        });

        // 2. Bot vs. Bot (BEHOBEN: Startet jetzt die automatische KI-Schleife)
        btnBotVsBot.addActionListener(e -> {
            KI bot1 = new KI();
            KI bot2 = new KI();
            GUI gui = new GUI(); // Leeres GUI-Fenster ohne Spieler-KI erstellen
            this.dispose();
            
            // Schiffe visuell auf das Feld bringen, damit man sie sieht
            char[][] board1 = bot1.getMyBoard();
            for (int r = 0; r < 10; r++) {
                for (int c = 0; c < 10; c++) {
                    if (board1[r][c] == 'S') gui.pruefeGegnerSchuss(r, c); // Trick zum Aufdecken
                }
            }
            // Felder zurücksetzen auf Startfarbe
            gui.starteBotSchleife(bot1, bot2);
        });

        // 3. Spieler vs. Spieler (BEHOBEN: Baut jetzt echte Server/Client Sockets auf)
        btnSpielerVsSpieler.addActionListener(e -> {
            String[] optionen = {"Server erstellen (Host)", "Mit Server verbinden (Client)"};
            int wahl = JOptionPane.showOptionDialog(this, "Möchtest du ein Spiel hosten oder beitreten?", "Netzwerkmodus",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, optionen, optionen);

            if (wahl == 0) { // Server / Host
                new Thread(() -> {
                    try {
                        System.out.println("Server gestartet, warte auf Client auf Port 5000...");
                        ServerSocket serverSocket = new ServerSocket(5000);
                        Socket socket = serverSocket.accept();
                        
                        GUI gui = new GUI();
                        spiel netzwerkSpiel = new spiel(gui, null);
                        netzwerkSpiel.starteNetzwerkVerbindung(socket, true); // true = Server darf anfangen
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
                            
                            GUI gui = new GUI();
                            spiel netzwerkSpiel = new spiel(gui, null);
                            netzwerkSpiel.starteNetzwerkVerbindung(socket, false); // false = Client wartet
                            this.dispose();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(this, "Verbindung fehlgeschlagen!");
                        }
                    }).start();
                }
            }
        });

        buttonPanel.add(btnSpielerVsBot);
        buttonPanel.add(btnBotVsBot);
        buttonPanel.add(btnSpielerVsSpieler);
        add(buttonPanel, BorderLayout.CENTER);

        // Fenstergröße festlegen und mittig anzeigen
        setSize(400, 350);
        setLocationRelativeTo(null);
        setVisible(true);
    }
}