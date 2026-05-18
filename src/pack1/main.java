package pack1;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        //startet die GUI "reibungsloser"
        SwingUtilities.invokeLater(() -> {
            //KI-Logik
            KI meineKI = new KI();
            //erstellt die GUI mit der KI
            new GUI(meineKI);
        });
    }
}