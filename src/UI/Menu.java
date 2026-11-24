package UI;

import data.*;
import data.Dily.*;

public class Menu {
    private boolean bezi = true;
    private Garaz garaz;
    private Auto autoNaVylete;

    public Menu() {
        this.garaz = new Garaz(5);
    }

    public void vypisMenu() {

        while (bezi) {
            IO.println("\n*** MENU ***");
            IO.print("1. Garáž\n2. Auto\n0. Konec");
            int volba = Integer.parseInt(IO.readln("\nZadej možnost menu: "));

            switch (volba) {
                case 1:
                    int volba2;
                    do {
                
                        IO.print("\n--- GARÁŽ ---\n1. Přidat auto\n2. Výpis aut z garáže\n3. Odstranit auto\n4. Natankovat auto\n0. Zpět");
                        volba2 = Integer.parseInt(IO.readln("\nZadej možnost menu: "));
                        switch (volba2) {
                            case 1:
                                pridejAuto();
                                break;
                            case 2:
                                vypisAuta();
                                break;
                            case 3:
                                odstranAuto();
                                break;
                            case 4:
                                natankujAuto();
                                break;
                            case 0:
                                break;
                            default:
                                IO.print("Neplatná volba.\n");
                                break;
                        }
                    } while (volba2 != 0);
                    break;
                case 2:
                    int volba3;
                    do {
                        IO.print("\n--- AUTO ---\n1. Vyjet na výlet\n2. Zkontrolovat nádrž\n0. Zpět");
                        volba3 = Integer.parseInt(IO.readln("\nZadej možnost menu: "));
                        switch (volba3) {
                            case 1:
                                vyjedNaVylet();
                                break;
                            case 2:
                                zkontrolovatNadrz();
                                break;
                            case 0:
                                break;
                            default:
                                IO.print("Neplatná volba.\n");
                                break;
                        }
                    } while (volba3 != 0);
                    break;

                case 0:
                    IO.print("=== KONEC ===\n");
                    bezi = false;
                    break;
                default:
                    IO.print("Neplatná volba.\n");
                    break;
            }

        }

    }

    private void pridejAuto() {
        // značka
        String znacka = IO.readln("Značka: ");
        // karoserie
        String modelKaroserie = IO.readln("Model karoserie: ");
        String barvaKaroserie = IO.readln("Barva karoserie: ");
        // motor
        String nazevMotoru = IO.readln("Název motoru: ");
        double spotreba = Double.parseDouble(IO.readln("Spotřeba (l/100 km, např. 6.5): "));
        double vykon = Double.parseDouble(IO.readln("Výkon (kW): "));
        // nadrz
        double objem = Double.parseDouble(IO.readln("Objem nádrže (l): "));
        // kola
        double velikostKol = Double.parseDouble(IO.readln("Velikost kol (palce): "));
        String typKol = IO.readln("Typ kol: ");
        String barvaKol = IO.readln("Barva kol: ");

        if (spotreba <= 0 || vykon <= 0 || objem <= 0 || velikostKol <= 0) {
            IO.print("Hodnoty musí být větší než 0. Auto nebylo přidáno.\n");
            return;
        }

        Karoserie karoserie = new Karoserie(modelKaroserie, barvaKaroserie);
        Motor motor = new Motor(nazevMotoru, spotreba, vykon);
        Nadrz nadrz = new Nadrz(objem);
        Kola kola = new Kola(velikostKol, typKol, barvaKol);

        Auto auto = new Auto(znacka, karoserie, motor, nadrz, kola);

        if (garaz.pridejAuto(auto)) {
            IO.print("Auto přidáno.\n");
        } else {
            IO.print("Garáž je plná.\n");
        }
    }

    private void vypisAuta() {
        if (garaz.getPocetAut() == 0) {
            IO.print("Garáž je prázdná.\n");
            return;
        }
        for (int i = 0; i < garaz.getPocetAut(); i++) {
            Auto auto = garaz.getAuto(i);
            IO.print((i + 1) + ". " + auto.getZnacka() + " " + auto.getKaroserie().getZnacka()
                    + ", motor " + auto.getMotor().getNazev()
                    + ", palivo " + String.format("%.1f", auto.getNadrz().getAktualniStav()) + " l\n");
        }
    }

    private int vyberAuto() { // Metoda na vybírání auta. Ověří se, že garáž není prázdná a poté jsou vypsány
                              // auta. Následně si uživatel vybere auto podle čísla, číslo je poté
                              // zkontrolováno zda je vůbec platné. Po kontrole metoda vrací cislo -1 kvůli
                              // indexování od 0.
        if (garaz.getPocetAut() == 0) {
            IO.print("Garáž je prázdná.\n");
            return -1;
        }
        vypisAuta();
        int cislo = Integer.parseInt(IO.readln("Zadej číslo auta: "));
        if (cislo < 1 || cislo > garaz.getPocetAut()) {
            IO.print("Neplatné číslo auta.\n");
            return -1;
        }
        return cislo - 1;
    }

    private void vyjedNaVylet() { // Metoda pro vyjetí s autem. Ověření, že index je platný. Ověření dojezdu auta
        int index = vyberAuto();
        if (index == -1) {
            return;
        }
        Auto auto = garaz.getAuto(index);
        double km = Double.parseDouble(IO.readln("Vzdálenost výletu (km): "));
        if (km <= 0) {
            IO.print("Vzdálenost musí být větší než 0.\n");
            return;
        }

        if (!auto.muzeUjet(km)) {
            IO.print("Auto nedojede, dojezd je jen " + String.format("%.1f", auto.getDojezd()) + " km.\n");
            if (IO.readln("Chceš natankovat? (a/n): ").equalsIgnoreCase("a")) {
                double litry = Double.parseDouble(IO.readln("Kolik litrů: "));
                if (litry <= 0) {
                    IO.print("Neplatné množství.\n");
                } else {
                    auto.getNadrz().natankuj(litry);
                }
            }
            if (!auto.muzeUjet(km)) {
                IO.print("Výlet nelze uskutečnit.\n");
                return;
            }
        }

        auto.vyjed(km);
        autoNaVylete = auto;
        IO.print("Výlet proběhl. V nádrži zbývá "
                + String.format("%.1f", auto.getNadrz().getAktualniStav()) + " l.\n");
    }

    private void natankujAuto() {
        int index = vyberAuto();
        if (index == -1) {
            return;
        }
        Nadrz nadrz = garaz.getAuto(index).getNadrz();
        double volne = nadrz.getObjem() - nadrz.getAktualniStav();
        if (volne <= 0) {
            IO.print("Nádrž je plná.\n");
            return;
        }
        IO.print("Do nádrže se vejde ještě " + String.format("%.1f", volne) + " l.\n");
        double litry = Double.parseDouble(IO.readln("Kolik litrů natankovat: "));
        // Kontrola, že litry nejsou 0 ani záporné
        if (litry <= 0) {
            IO.print("Neplatné množství.\n");
            return;
        }
        nadrz.natankuj(litry);
        IO.print("Natankováno. V nádrži je nyní " + String.format("%.1f", nadrz.getAktualniStav()) + " l.\n");
    }

    private void odstranAuto() {
        int index = vyberAuto();
        if (index == -1) {
            return;
        }
        if (garaz.getAuto(index) == autoNaVylete) {
            autoNaVylete = null;
        }
        garaz.odeberAuto(index);
        IO.print("Auto odstraněno.\n");
    }

    private void zkontrolovatNadrz() {
        if (autoNaVylete == null) {
            IO.print("Nejsi na výletě.\n");
            return;
        }
        Nadrz nadrz = autoNaVylete.getNadrz();
        IO.print("Auto " + autoNaVylete.getZnacka() + ": v nádrži je aktuálně "
                + String.format("%.1f", nadrz.getAktualniStav()) + " l z "
                + String.format("%.1f", nadrz.getObjem()) + " l.\n");
    }
}
