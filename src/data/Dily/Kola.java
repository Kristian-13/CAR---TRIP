package data.Dily;

public class Kola {
    private double velikost;
    private String typ;
    private String barva;

    public Kola(double velikost, String typ, String barva) {
        this.velikost = velikost;
        this.typ = typ;
        this.barva = barva;
    }

    public double getVelikost() {
        return velikost;
    }

    public String getTyp() {
        return typ;
    }

    public String getBarva() {
        return barva;
    }
}