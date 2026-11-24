# Cesta autem

Konzolová aplikace v Javě, která simuluje garáž a jízdu autem. Školní projekt (PG, 2.D).

## Co umí

**Garáž** (kapacita 5 aut)
- přidat auto (značka, karoserie, motor, nádrž, kola)
- vypsat auta v garáži
- odstranit auto
- natankovat auto

**Auto**
- vyjet na výlet – spočítá spotřebu a dojezd; když auto nedojede, nabídne natankování
- zkontrolovat nádrž auta, které je na výletě

## Struktura

```
src/
├── App.java              – vstupní bod programu
├── UI/Menu.java          – textové menu a ovládání
└── data/
    ├── Auto.java         – auto složené z dílů, výpočet dojezdu
    ├── Garaz.java        – seznam aut s omezenou kapacitou
    └── Dily/
        ├── Karoserie.java
        ├── Kola.java
        ├── Motor.java    – název, spotřeba (l/100 km), výkon
        └── Nadrz.java    – objem a aktuální stav paliva
```

## Spuštění

Potřebuješ **JDK 25** nebo novější (program používá třídu `java.lang.IO`).

```
javac -encoding UTF-8 -d bin src/App.java src/UI/*.java src/data/*.java src/data/Dily/*.java
java -cp bin App
```

Nebo otevři složku ve VS Code s rozšířením *Extension Pack for Java* a klikni na **Run** u `App.java`.
