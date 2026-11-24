package data.Dily;

public class Karoserie {
    private String znacka;
    private String barva;

    public Karoserie(String znacka, String barva) {
        this.znacka = znacka;
        this.barva = barva;
    }

    public String getZnacka() {
        return znacka;
    }

    public String getBarva() {
        return barva;
    }
}