package data;

import data.Dily.*;

public class Auto {
    private String znacka;
    private Karoserie karoserie;
    private Motor motor;
    private Nadrz nadrz;
    private Kola kola;
    private double najeto; // tachometr – kolik km auto celkem ujelo

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

    public double getNajeto() {
        return najeto;
    }

    // Používá se při načítání uložené garáže ze souboru.
    public void setNajeto(double najeto) {
        if (najeto >= 0) {
            this.najeto = najeto;
        }
    }

    public double spotrebaNaTrase(double km) {
        return motor.getSpotreba() * km / 100;
    }

    public void vyjed(double km) {
        if (km <= 0) {
            return;
        }
        nadrz.odeberPalivo(spotrebaNaTrase(km));
        najeto += km;
    }
}