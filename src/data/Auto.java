package data;

import data.Dily.*;

public class Auto {
    private String znacka;
    private Karoserie karoserie;
    private Motor motor;
    private Nadrz nadrz;
    private Kola kola;

    public Auto(String znacka, Karoserie model, Motor motor, Nadrz nadrz, Kola kola) {
        this.znacka = znacka;
        this.karoserie = model;
        this.motor = motor;
        this.nadrz = nadrz;
        this.kola = kola;
    }

    public String getZnacka() {
        return znacka;
    }

    public Karoserie getKaroserie() {
        return karoserie;
    }

    public Motor getMotor() {
        return motor;
    }

    public Nadrz getNadrz() {
        return nadrz;
    }

    public Kola getKola() {
        return kola;
    }

    public double getDojezd() {
        return nadrz.getAktualniStav() / motor.getSpotreba() * 100;
    }

    public boolean muzeUjet(double km) {
        return getDojezd() >= km;
    }

    public void vyjed(double km) {
        double spotrebovano = motor.getSpotreba() * km / 100;
        nadrz.odeberPalivo(spotrebovano);
    }
}