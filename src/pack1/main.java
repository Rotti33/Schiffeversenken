package pack1;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        //startet die GUI "reibungsloser"
        SwingUtilities.invokeLater(() -> {
            //Startet das Hauptmenü beim Programmstart
            new Startmenu(); 
        });
    }
}