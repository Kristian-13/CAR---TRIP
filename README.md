# Cesta autem

Aplikace v Javě, která simuluje garáž a jízdu autem. Školní projekt (PG, 2.D).
Běží buď v konzoli, nebo jako webová aplikace (Java backend + HTML/JS frontend).

## Co umí

**Garáž** (kapacita 5 aut)
- přidat auto (značka, karoserie, motor, nádrž, kola)
- vypsat auta v garáži
- odstranit auto
- natankovat auto

**Auto**
- vyjet na výlet – spočítá spotřebu a dojezd; když auto nedojede, nabídne natankování
- zkontrolovat nádrž auta, které je na výletě
- tachometr – kolik km auto celkem ujelo

**Navíc ve webové verzi**
- plánovač výletu s oblíbenými cíli a předpovědí spotřeby (když palivo nestačí, dotankuje s rezervou)
- animovaná simulace jízdy – krajina, rychlost, ubývající palivo a ujeté km živě, na konci souhrn
- deník posledních jízd
- tankování „do plna“ a ukázková auta pro rychlé vyzkoušení
- garáž se ukládá do souboru `garaz.txt`, takže po restartu serveru auta nezmizí

## Struktura

```
src/
├── App.java              – spuštění konzolové verze
├── WebApp.java           – spuštění webové verze
├── UI/Menu.java          – textové menu a ovládání
├── web/Server.java       – backend: HTTP server a REST API
└── data/
    ├── Auto.java         – auto složené z dílů, výpočet dojezdu
    ├── Garaz.java        – seznam aut s omezenou kapacitou
    └── Dily/
        ├── Karoserie.java
        ├── Kola.java
        ├── Motor.java    – název, spotřeba (l/100 km), výkon
        └── Nadrz.java    – objem a aktuální stav paliva
public/
├── index.html            – frontend: stránka
├── style.css             – vzhled
└── app.js                – volá API backendu a vykresluje garáž
```

## Jak je propojený backend s frontendem

```
Prohlížeč (public/app.js)  ──fetch──▶  Java server (web/Server.java)  ──▶  Garaz, Auto, Nadrz…
                           ◀──JSON──
```

Backend je napsaný jen ve standardní Javě (`com.sun.net.httpserver.HttpServer`), bez knihoven.
Server posílá prohlížeči soubory ze složky `public` a na adresách `/api/...` nabízí API:

| Metoda | Adresa                        | Co dělá                        |
|--------|-------------------------------|--------------------------------|
| GET    | `/api/garaz`                  | vrátí kapacitu a seznam aut    |
| POST   | `/api/auta`                   | přidá auto                     |
| DELETE | `/api/auta/{cislo}`           | odstraní auto                  |
| POST   | `/api/auta/{cislo}/tankovani` | natankuje (parametr `litry`)   |
| POST   | `/api/auta/{cislo}/vylet`     | vyjede na výlet (parametry `km`, `cil`)|

Chyby (např. „Auto nedojede“) vrací server jako `{"chyba": "..."}` a frontend je ukáže.
Auta se ukládají do souboru `garaz.txt` (deník jízd je jen v paměti).

## Spuštění

Potřebuješ **JDK 25** nebo novější (program používá třídu `java.lang.IO`).
Příkazy spouštěj ve složce projektu (tam, kde je `src` a `public`).

**Webová verze:**

```
javac -encoding UTF-8 -d bin src/*.java src/web/*.java src/UI/*.java src/data/*.java src/data/Dily/*.java
java -cp bin WebApp
```

Pak otevři v prohlížeči http://localhost:8080

**Konzolová verze:**

```
java -cp bin App
```

Ve VS Code s rozšířením *Extension Pack for Java* stačí kliknout na **Run** u `WebApp.java` nebo `App.java`.
