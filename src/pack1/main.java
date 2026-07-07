package pack1;

import javax.swing.SwingUtilities;

/**
 * Haupteinstiegspunkt der Anwendung "Schiffe versenken".
 * 
 * Diese Klasse ist verantwortlich für die Initialisierung der GUI
 * und den Start des Programms. Die GUI wird auf dem Event Dispatch Thread
 * gestartet, um Thread-Sicherheit zu gewährleisten.
 * 
 * @author Lisa Renner, Rodrigo Malisi Sousa
 * @version 1.0
 */
public class Main {
    /**
     * Hauptmethode, die das Programm startet.
     * 
     * Ruft das Startmenü auf und lädt es auf dem Event Dispatch Thread,
     * um sicherzustellen, dass Swing-Komponenten thread-sicher initialisiert werden.
     * 
     * @param args Kommandozeilenargumente (werden nicht verwendet)
     */
    public static void main(String[] args) {
        //startet die GUI "reibungsloser"
        SwingUtilities.invokeLater(() -> {
            //Startet das Hauptmenü beim Programmstart
            new Startmenu(); 
        });
    }
}