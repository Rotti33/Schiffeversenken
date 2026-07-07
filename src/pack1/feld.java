package pack1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Visuelle Darstellung eines Schiffe-versenken-Spielfeldes.
 * 
 * Diese Klasse erstellt ein interaktives Gitter mit Buttons, das als
 * Spielfeld dient. Sie verwaltet:
 * - Ein konfigurierbares NxN-Gitter von Buttons
 * - Farbcodierung der Felder (Wasser, Schiff, Treffer, etc.)
 * - Klickbarkeit und Aktivierungsstatus
 * - Automatische Größenanpassung basierend auf Feldgröße
 * 
 * Das Feld kann sowohl für das Spielerfeld als auch das Gegnerfeld
 * verwendet werden, mit unterschiedlichen Interaktivitätseinstellungen.
 * 
 * @author Lisa Renner, Rodrigo Malisi Sousa
 * @version 1.0
 * @see GUI
 */
public class Feld extends JPanel {
    /**
     * 2D-Array von Buttons, die die einzelnen Spielfeld-Zellen repräsentieren.
     * Jeder Button kann gefärbt werden, um den Zustand der Zelle anzuzeigen.
     */
    private JButton[][] Zellen;
    
    /**
     * Größe des quadratischen Spielfeldes (z.B. 10 für 10x10).
     */
    private int groesse;

    /**
     * Konstruktor für ein Spielfeld.
     * 
     * Erstellt ein NxN-Gitter von Buttons mit automatischer Größenanpassung.
     * Der Konstruktor verwaltet:
     * - Layout mit Titel oben und Gitter in der Mitte
     * - Automatische Button-Größenberechnung (15-40 Pixel)
     * - Optionale Klickbarkeit mit ActionListener
     * - Farbcodierung (hellblau als Standard für Wasser)
     * 
     * Bei klickbaren Feldern wird die Mausposition als Point-Objekt mit dem
     * ActionEvent übertragen, damit der Listener die Koordinaten auslesen kann.
     * 
     * @param titel Der Name/Titel des Feldes (z.B. "Spielerfeld", "Gegnerfeld")
     * @param groesse Die Größe des quadratischen Gitters (z.B. 10 für 10x10)
     * @param istKlickbar true wenn das Feld interaktiv sein soll; false sonst
     * @param klickAktion Ein ActionListener, der aufgerufen wird, wenn ein Button geklickt wird
     */
    public Feld(String titel, int groesse, boolean istKlickbar, ActionListener klickAktion) {
        this.groesse = groesse;
        this.Zellen = new JButton[groesse][groesse];

        //Sorgt für den Abstand zwischen Titel und Feld
        setLayout(new BorderLayout(5, 5));

        //Überschrift/Titel
        JLabel titleLabel = new JLabel(titel, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        add(titleLabel, BorderLayout.NORTH);

        //Feld-Raster
        JPanel grid = new JPanel(new GridLayout(groesse, groesse));

        //Berechnet die Button-Größe, damit das Fenster richtig skaliert
        int buttonGroesse = Math.max(15, Math.min(40, 400 / groesse));

        //Gibt allen Buttons eine Logik und Aussehen        
        for (int i = 0; i < groesse; i++) {
            for (int j = 0; j < groesse; j++) {
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(buttonGroesse, buttonGroesse));
                btn.setBackground(new Color(220, 240, 255)); //Blau für Wasser
                btn.setBorder(BorderFactory.createLineBorder(Color.GRAY));

                //Speichert den Button im Array ab, um ihn später färben zu können                
                Zellen[i][j] = btn;
 
                //Nur wenn das Feld klickbar sein soll
                if (istKlickbar) {
                    final int row = i;
                    final int col = j;
                    btn.addActionListener(e -> {
                        //Schickt die Koordinaten als Point-Objekt mit
                        e.setSource(new Point(row, col));
                        klickAktion.actionPerformed(e);
                    });
                }
                //Fügt den fertigen Button visuell hinzu
                grid.add(btn);
            }
        }
        //Feld wird positioniert
        add(grid, BorderLayout.CENTER);
    }

    /**
     * Ändert die Farbe einer einzelnen Zelle im Spielfeld.
     * 
     * Wird verwendet, um Zellenzustände visuell darzustellen:
     * - Blau: Wasser (unbeschossen)
     * - Rot: Treffer
     * - Dunkelblau: Versenkt
     * - Grau: Spieler-Schiff (versteckt im Gegnerfeld)
     * 
     * @param reihe Zeilenindex der Zelle (0-basiert)
     * @param spalte Spaltenindex der Zelle (0-basiert)
     * @param farbe Die neue Hintergrundfarbe für die Zelle
     */
    public void setZellenFarbe(int reihe, int spalte, Color farbe) {
        if (reihe >= 0 && reihe < groesse && spalte >= 0 && spalte < groesse) {
            Zellen[reihe][spalte].setBackground(farbe);
        }
    }

    /**
     * Aktiviert oder deaktiviert die Interaktivität des gesamten Spielfeldes.
     * 
     * Wenn deaktiviert, können die Buttons nicht geklickt werden. Dies wird verwendet,
     * um zu verhindern, dass der Spieler während der KÜ-Phase schießt oder
     * während die KI am Zug ist.
     * 
     * @param aktiv true um alle Buttons klickbar zu machen; false zum Deaktivieren
     */
    public void setAktiv(boolean aktiv) {
        for (int i = 0; i < groesse; i++) {
            for (int j = 0; j < groesse; j++) {
                Zellen[i][j].setEnabled(aktiv);
            }
        }
    }
}