package pack1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class Feld extends JPanel {
    // BEHOBEN: Das Array wird jetzt flexibel deklariert und erst im Konstruktor dimensioniert
    private JButton[][] Zellen;
    private int groesse;

    // Konstruktor: Baut das Feld in der gewünschten dynamischen Größe auf
    public Feld(String titel, int groesse, boolean istKlickbar, ActionListener klickAktion) {
        this.groesse = groesse;
        this.Zellen = new JButton[groesse][groesse];

        // Sorgt für den Abstand zwischen Titel und Feld
        setLayout(new BorderLayout(5, 5));

        // Überschrift/Titel
        JLabel titleLabel = new JLabel(titel, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        add(titleLabel, BorderLayout.NORTH);

        // Das dynamische Feld-Raster
        JPanel grid = new JPanel(new GridLayout(groesse, groesse));

        // Berechnet die optimale Button-Größe, damit das Fenster sauber skaliert
        // Bei 10x10 sind es 40px, bei 30x30 wird es automatisch kleiner (ca. 20px)
        int buttonGroesse = Math.max(15, Math.min(40, 400 / groesse));

        // Gibt allen Buttons eine Logik und Aussehen        
        for (int i = 0; i < groesse; i++) {
            for (int j = 0; j < groesse; j++) {
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(buttonGroesse, buttonGroesse));
                btn.setBackground(new Color(220, 240, 255)); // Blau für Wasser
                btn.setBorder(BorderFactory.createLineBorder(Color.GRAY));

                // Speichert den Button im Array ab, um ihn später färben zu können                
                Zellen[i][j] = btn;
 
                // Nur wenn das Feld klickbar sein soll
                if (istKlickbar) {
                    final int row = i;
                    final int col = j;
                    btn.addActionListener(e -> {
                        // Schickt die Koordinaten als Point-Objekt mit
                        e.setSource(new Point(row, col));
                        klickAktion.actionPerformed(e);
                    });
                }
                // Fügt den fertigen Button visuell hinzu
                grid.add(btn);
            }
        }
        // Feld wird positioniert
        add(grid, BorderLayout.CENTER);
    }

    // Methode, um ein Feld von außen umzufärben (z.B. rot bei Treffer)
    public void setZellenFarbe(int reihe, int spalte, Color farbe) {
        if (reihe >= 0 && reihe < groesse && spalte >= 0 && spalte < groesse) {
            Zellen[reihe][spalte].setBackground(farbe);
        }
    }

    // Erlaubt es, die Interaktion mit dem Feld dynamisch ein- und auszuschalten
    public void setAktiv(boolean aktiv) {
        for (int i = 0; i < groesse; i++) {
            for (int j = 0; j < groesse; j++) {
                Zellen[i][j].setEnabled(aktiv);
            }
        }
    }
}