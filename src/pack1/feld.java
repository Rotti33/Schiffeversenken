package pack1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class feld extends JPanel {
    // Jedes Feld verwaltet seine eigenen 100 Buttons
    private JButton[][] cells = new JButton[10][10];

    // Konstruktor: Baut das 10x10 Gitter auf
    public feld(String titel, boolean istKlickbar, ActionListener klickAktion) {
        setLayout(new BorderLayout(5, 5));

        // Überschrift (z.B. "Dein Spielfeld")
        JLabel titleLabel = new JLabel(titel, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        add(titleLabel, BorderLayout.NORTH);

        // Das 10x10 Raster
        JPanel grid = new JPanel(new GridLayout(10, 10));
        
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(40, 40));
                btn.setBackground(new Color(220, 240, 255)); // Wasser-Blau
                btn.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                
                cells[r][c] = btn;

                // Nur wenn das Feld klickbar sein soll (Gegnerfeld)
                if (istKlickbar) {
                    final int row = r;
                    final int col = c;
                    btn.addActionListener(e -> {
                        // Wir tricksen: Wir schicken die Koordinaten als Point-Objekt mit
                        e.setSource(new Point(row, col));
                        klickAktion.actionPerformed(e);
                    });
                }
                grid.add(btn);
            }
        }
        add(grid, BorderLayout.CENTER);
    }

    // Methode, um ein Feld von außen umzufärben (z.B. rot bei Treffer)
    public void setZellenFarbe(int reihe, int spalte, Color farbe) {
        cells[reihe][spalte].setBackground(farbe);
    }
}