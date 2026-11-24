package data;

import java.util.ArrayList;

public class Garaz {
    private ArrayList<Auto> auta;
    private int kapacita;

    public Garaz(int kapacita) {
        this.kapacita = kapacita;
        this.auta = new ArrayList<>();
    }

    public boolean pridejAuto(Auto auto) {
        if (auta.size() >= kapacita) {
            return false;
        }
        auta.add(auto);
        return true;
    }

    public void odeberAuto(int index) {
        auta.remove(index);
    }

    public Auto getAuto(int index) {
        return auta.get(index);
    }

    public int getPocetAut() {
        return auta.size();
    }
}