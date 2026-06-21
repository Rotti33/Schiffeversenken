package pack1;

import javax.swing.*;
import java.awt.*;

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

        // 2. Bot vs. Bot
        btnBotVsBot.addActionListener(e -> {
            // Hier starten Sie später die Logik, bei der zwei KIs gegeneinander spielen
            JOptionPane.showMessageDialog(this, "Modus Bot vs. Bot folgt noch!");
        });

        // 3. Spieler vs. Spieler (Ihr neues Netzwerksystem)
        btnSpielerVsSpieler.addActionListener(e -> {
            // Hier rufen Sie die Logik/GUI auf, die Sie für das Netzwerk gecodet haben
            JOptionPane.showMessageDialog(this, "Verbindung zum Netzwerk wird aufgebaut...");
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