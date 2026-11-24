package data.Dily;

public class Motor {

    private String nazev;
    private double spotreba;
    private double vykon;

    public Motor(String nazev, double spotreba, double vykon) {
        this.nazev = nazev;
        this.spotreba = spotreba;
        this.vykon = vykon;
    }

    public double getSpotreba() {
        return spotreba;
    }

    public String getNazev() {
        return nazev;
    }

    public double getVykon() {
        return vykon;
    }
}
