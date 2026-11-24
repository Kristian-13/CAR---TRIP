package data.Dily;

public class Nadrz {
    private double objem;
    private double aktualniStav;

    public Nadrz(double objem) {
        this.objem = objem;
        this.aktualniStav = 0;
    }

    public void natankuj(double litry) {
        // OPRAVA: Pojistka - záporné nebo nulové litry se ignorují, aby nádrž
        // nikdy nemohla klesnout pod 0 (dřív šlo "natankovat" -20 l).
        if (litry <= 0) {
            return;
        }
        aktualniStav += litry;
        if (aktualniStav > objem) {
            aktualniStav = objem;
        }
    }

    public void odeberPalivo(double litry) {
        // OPRAVA: Pojistka - záporné litry se ignorují, aby odebrání paliva
        // nemohlo palivo naopak přidat (dřív se to stalo u výletu na záporné km).
        if (litry <= 0) {
            return;
        }
        aktualniStav -= litry;
        if (aktualniStav < 0) {
            aktualniStav = 0;
        }
    }

    public double getObjem() {
        return objem;
    }

    public double getAktualniStav() {
        return aktualniStav;
    }
}