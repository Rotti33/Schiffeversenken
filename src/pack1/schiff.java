package pack1;

public class Schiff {
    private String name;
    private int laenge;
    private int treffer;

    //Erstellt Schiffe mit Namen und länge 
    public Schiff(String name, int laenge) {
        this.name = name;
        this.laenge = laenge;
        //Schiff startet unbeschädigt
        this.treffer = 0;
    }

    //Zähler für die Schiffs treffer
    public void registriereTreffer() {
        if (treffer < laenge) {
            treffer++;
        }
    }

    //Prüft, ob das Schiff komplett zerstörte wurde
    public boolean istVersenkt() {
        return treffer >= laenge;
    }

    public String getName() {
        return name;
    }

    public int getLaenge() {
        return laenge;
    }

    public int getTreffer() {
        return treffer;
    }
}