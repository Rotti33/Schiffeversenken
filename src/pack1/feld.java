package pack1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class Feld extends JPanel {
    //Ein Feld mit 10x10 Zellen (100 Zellen) mit Buttons
    private JButton[][] Zellen = new JButton[10][10];

    //Konstruktor: Baut das 10x10 Feld auf
    public Feld(String titel, boolean istKlickbar, ActionListener klickAktion) {
        //Sorgt für den abstand zwischen Titel und Feld
        setLayout(new BorderLayout(5, 5));

        //Überschrift/Titel
        JLabel titleLabel = new JLabel(titel, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        add(titleLabel, BorderLayout.NORTH);

        //Das 10x10 Feld
        JPanel grid = new JPanel(new GridLayout(10, 10));

        //Gibt allen 100 Buttons eine Logik und Aussehen        
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(40, 40));
                btn.setBackground(new Color(220, 240, 255)); //Blau für Wasser
                btn.setBorder(BorderFactory.createLineBorder(Color.GRAY));

                //Speichert den Button im Array ab, um ihn später färben zu können                
                Zellen[i][j] = btn;
 
                //Nur wenn das Feld klickbar sein soll (Gegnerfeld)
                if (istKlickbar) {
                    final int row = i;
                    final int col = j;
                    btn.addActionListener(e -> {
                        //Schickt die Koordinaten als Point-Objekt mit
                        e.setSource(new Point(row, col));
                        klickAktion.actionPerformed(e);
                    });
                }
                //Fügt den fertigen Button dem visuell hinzu
                grid.add(btn);
            }
        }
        //Feld wird positioniert
        add(grid, BorderLayout.CENTER);
    }

    //Methode, um ein Feld von außen umzufärben (z.B. rot bei Treffer)
    public void setZellenFarbe(int reihe, int spalte, Color farbe) {
        Zellen[reihe][spalte].setBackground(farbe);
    }

    // NEU hinzugefügt für Netzwerk & Bot-Modus:
    // Erlaubt es, die Interaktion mit dem Feld dynamisch ein- und auszuschalten
    public void setAktiv(boolean aktiv) {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Zellen[i][j].setEnabled(aktiv);
            }
        }
    }
}