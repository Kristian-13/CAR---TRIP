package web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import data.*;
import data.Dily.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Backend webové aplikace.
 *
 * Používá HTTP server, který je přímo součástí Javy (žádné knihovny navíc).
 * Dělá dvě věci:
 *   1) /api/...  – REST API, přes které frontend pracuje s garáží (vrací JSON)
 *   2) ostatní   – posílá soubory frontendu ze složky "public" (HTML, CSS, JS)
 *
 * Logika (Garaz, Auto, Nadrz...) je stejná jako v konzolové verzi,
 * mění se jen to, že místo IO.readln() přichází data z prohlížeče.
 *
 * Garáž se po každé změně uloží do textového souboru, takže
 * po restartu serveru auta nezmizí.
 */
public class Server {
    private final HttpServer http;
    private final Path slozkaFrontendu;
    private final Path souborGaraze;
    private final Garaz garaz = new Garaz(5);
    private Auto autoNaVylete;

    /** Jeden záznam v deníku jízd. */
    private record Jizda(String auto, String cil, double km, double litry) {
    }

    private final List<Jizda> denik = new ArrayList<>(); // nejnovější jízda je první

    public Server(int port, String slozkaFrontendu, String souborGaraze) throws IOException {
        this.slozkaFrontendu = Path.of(slozkaFrontendu).toAbsolutePath().normalize();
        this.souborGaraze = Path.of(souborGaraze);
        nactiGaraz();
        this.http = HttpServer.create(new InetSocketAddress(port), 0);
        http.createContext("/api/", this::obsluzApi);
        http.createContext("/", this::obsluzSoubor);
    }

    public void start() {
        http.start();
    }

    // ===================== API =====================
    //
    //  GET    /api/garaz                  – stav garáže a seznam aut
    //  POST   /api/auta                   – přidat auto
    //  DELETE /api/auta/{cislo}           – odstranit auto
    //  POST   /api/auta/{cislo}/tankovani – natankovat (parametr litry)
    //  POST   /api/auta/{cislo}/vylet     – vyjet na výlet (parametry km, cil)
    //
    //  Každá odpověď vrací celý nový stav garáže, frontend ho jen vykreslí.

    private void obsluzApi(HttpExchange ex) throws IOException {
        try {
            String metoda = ex.getRequestMethod();
            String[] casti = ex.getRequestURI().getPath().split("/"); // ["", "api", ...]

            if (casti.length == 3 && casti[2].equals("garaz") && metoda.equals("GET")) {
                posliJson(ex, 200, stavGaraze());
            } else if (casti.length == 3 && casti[2].equals("auta") && metoda.equals("POST")) {
                pridejAuto(ex);
            } else if (casti.length == 4 && casti[2].equals("auta") && metoda.equals("DELETE")) {
                odstranAuto(ex, indexAuta(casti[3]));
            } else if (casti.length == 5 && casti[2].equals("auta") && metoda.equals("POST")) {
                int index = indexAuta(casti[3]);
                switch (casti[4]) {
                    case "tankovani" -> natankuj(ex, index);
                    case "vylet" -> vyjedNaVylet(ex, index);
                    default -> posliChybu(ex, 404, "Neznámá akce.");
                }
            } else {
                posliChybu(ex, 404, "Neznámá adresa API.");
            }
        } catch (ChybaVstupu e) {
            posliChybu(ex, 400, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            posliChybu(ex, 500, "Chyba na serveru.");
        }
    }

    private void pridejAuto(HttpExchange ex) throws IOException {
        Map<String, String> p = nactiParametry(ex);

        String znacka = text(p, "znacka", "Značka");
        String modelKaroserie = text(p, "model", "Model karoserie");
        String barvaKaroserie = text(p, "barva", "Barva karoserie");
        String nazevMotoru = text(p, "motor", "Název motoru");
        double spotreba = kladneCislo(p, "spotreba", "Spotřeba");
        double vykon = kladneCislo(p, "vykon", "Výkon");
        double objem = kladneCislo(p, "objem", "Objem nádrže");
        double velikostKol = kladneCislo(p, "velikostKol", "Velikost kol");
        String typKol = text(p, "typKol", "Typ kol");
        String barvaKol = text(p, "barvaKol", "Barva kol");

        Auto auto = new Auto(znacka,
                new Karoserie(modelKaroserie, barvaKaroserie),
                new Motor(nazevMotoru, spotreba, vykon),
                new Nadrz(objem),
                new Kola(velikostKol, typKol, barvaKol));

        if (!garaz.pridejAuto(auto)) {
            posliChybu(ex, 409, "Garáž je plná.");
            return;
        }
        ulozGaraz();
        posliJson(ex, 201, stavGaraze());
    }

    private void odstranAuto(HttpExchange ex, int index) throws IOException {
        if (garaz.getAuto(index) == autoNaVylete) {
            autoNaVylete = null;
        }
        garaz.odeberAuto(index);
        ulozGaraz();
        posliJson(ex, 200, stavGaraze());
    }

    private void natankuj(HttpExchange ex, int index) throws IOException {
        Nadrz nadrz = garaz.getAuto(index).getNadrz();
        double litry = kladneCislo(nactiParametry(ex), "litry", "Množství");
        if (nadrz.getAktualniStav() >= nadrz.getObjem()) {
            posliChybu(ex, 409, "Nádrž je plná.");
            return;
        }
        nadrz.natankuj(litry);
        ulozGaraz();
        posliJson(ex, 200, stavGaraze());
    }

    private void vyjedNaVylet(HttpExchange ex, int index) throws IOException {
        Auto auto = garaz.getAuto(index);
        Map<String, String> p = nactiParametry(ex);
        double km = kladneCislo(p, "km", "Vzdálenost");
        String cil = p.getOrDefault("cil", "").replaceAll("[\\t\\r\\n]", " ").trim();
        if (cil.isEmpty()) cil = "Výlet";
        if (cil.length() > 60) cil = cil.substring(0, 60);

        if (!auto.muzeUjet(km)) {
            posliChybu(ex, 409, "Auto nedojede, dojezd je jen "
                    + String.format("%.1f", auto.getDojezd()) + " km.");
            return;
        }
        double palivoPred = auto.getNadrz().getAktualniStav();
        auto.vyjed(km);
        autoNaVylete = auto;

        String nazevAuta = auto.getZnacka() + " " + auto.getKaroserie().getZnacka();
        denik.add(0, new Jizda(nazevAuta, cil, km, palivoPred - auto.getNadrz().getAktualniStav()));
        if (denik.size() > 20) denik.remove(denik.size() - 1);

        ulozGaraz();
        posliJson(ex, 200, stavGaraze());
    }

    // ===================== Převod na JSON =====================

    private String stavGaraze() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"kapacita\":").append(garaz.getKapacita()).append(",\"auta\":[");
        for (int i = 0; i < garaz.getPocetAut(); i++) {
            if (i > 0) sb.append(',');
            sb.append(autoJson(garaz.getAuto(i)));
        }
        sb.append("],\"denik\":[");
        for (int i = 0; i < denik.size(); i++) {
            Jizda j = denik.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"auto\":").append(str(j.auto()))
                    .append(",\"cil\":").append(str(j.cil()))
                    .append(",\"km\":").append(num(j.km()))
                    .append(",\"litry\":").append(num(j.litry()))
                    .append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    private String autoJson(Auto a) {
        return "{"
                + "\"znacka\":" + str(a.getZnacka())
                + ",\"model\":" + str(a.getKaroserie().getZnacka())
                + ",\"barva\":" + str(a.getKaroserie().getBarva())
                + ",\"motor\":" + str(a.getMotor().getNazev())
                + ",\"spotreba\":" + num(a.getMotor().getSpotreba())
                + ",\"vykon\":" + num(a.getMotor().getVykon())
                + ",\"objem\":" + num(a.getNadrz().getObjem())
                + ",\"palivo\":" + num(a.getNadrz().getAktualniStav())
                + ",\"dojezd\":" + num(a.getDojezd())
                + ",\"velikostKol\":" + num(a.getKola().getVelikost())
                + ",\"typKol\":" + str(a.getKola().getTyp())
                + ",\"barvaKol\":" + str(a.getKola().getBarva())
                + ",\"najeto\":" + num(a.getNajeto())
                + ",\"naVylete\":" + (a == autoNaVylete)
                + "}";
    }

    private static String num(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
    }

    private static String str(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }

    // ===================== Čtení vstupu =====================

    /** Chyba ve vstupu od uživatele – vrátí se jako 400 s hláškou. */
    private static class ChybaVstupu extends RuntimeException {
        ChybaVstupu(String zprava) {
            super(zprava);
        }
    }

    /** Frontend posílá data jako formulář: znacka=Škoda&objem=50... */
    private static Map<String, String> nactiParametry(HttpExchange ex) throws IOException {
        String telo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> mapa = new HashMap<>();
        for (String par : telo.split("&")) {
            if (par.isEmpty()) continue;
            int rovnase = par.indexOf('=');
            String klic = rovnase < 0 ? par : par.substring(0, rovnase);
            String hodnota = rovnase < 0 ? "" : par.substring(rovnase + 1);
            mapa.put(URLDecoder.decode(klic, StandardCharsets.UTF_8),
                    URLDecoder.decode(hodnota, StandardCharsets.UTF_8));
        }
        return mapa;
    }

    private static String text(Map<String, String> p, String klic, String nazev) {
        // tabulátory a konce řádků nahradíme mezerou – v souboru oddělují hodnoty
        String hodnota = p.getOrDefault(klic, "").replaceAll("[\\t\\r\\n]", " ").trim();
        if (hodnota.isEmpty()) throw new ChybaVstupu(nazev + " nesmí být prázdná.");
        if (hodnota.length() > 60) throw new ChybaVstupu(nazev + " je moc dlouhá.");
        return hodnota;
    }

    private static double kladneCislo(Map<String, String> p, String klic, String nazev) {
        double cislo;
        try {
            cislo = Double.parseDouble(p.getOrDefault(klic, "").trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ChybaVstupu(nazev + " musí být číslo.");
        }
        if (!(cislo > 0) || Double.isInfinite(cislo)) {
            throw new ChybaVstupu(nazev + " musí být větší než 0.");
        }
        return cislo;
    }

    private int indexAuta(String cast) {
        try {
            int index = Integer.parseInt(cast);
            if (index >= 0 && index < garaz.getPocetAut()) return index;
        } catch (NumberFormatException ignored) {
        }
        throw new ChybaVstupu("Neplatné číslo auta.");
    }

    // ===================== Ukládání do souboru =====================
    //
    // Každé auto je jeden řádek, hodnoty jsou oddělené tabulátorem:
    // znacka model barva motor spotreba vykon objem palivo velikostKol typKol barvaKol najeto naVylete

    private void ulozGaraz() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < garaz.getPocetAut(); i++) {
            Auto a = garaz.getAuto(i);
            sb.append(String.join("\t",
                    a.getZnacka(), a.getKaroserie().getZnacka(), a.getKaroserie().getBarva(),
                    a.getMotor().getNazev(), num(a.getMotor().getSpotreba()), num(a.getMotor().getVykon()),
                    num(a.getNadrz().getObjem()), num(a.getNadrz().getAktualniStav()),
                    num(a.getKola().getVelikost()), a.getKola().getTyp(), a.getKola().getBarva(),
                    num(a.getNajeto()), String.valueOf(a == autoNaVylete)))
                    .append('\n');
        }
        try {
            Files.writeString(souborGaraze, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            IO.println("Garáž se nepodařilo uložit: " + e.getMessage());
        }
    }

    private void nactiGaraz() {
        if (!Files.exists(souborGaraze)) return;
        try {
            for (String radek : Files.readAllLines(souborGaraze, StandardCharsets.UTF_8)) {
                String[] h = radek.split("\t");
                if (h.length < 13) continue;
                Auto auto = new Auto(h[0],
                        new Karoserie(h[1], h[2]),
                        new Motor(h[3], Double.parseDouble(h[4]), Double.parseDouble(h[5])),
                        new Nadrz(Double.parseDouble(h[6])),
                        new Kola(Double.parseDouble(h[8]), h[9], h[10]));
                auto.getNadrz().natankuj(Double.parseDouble(h[7]));
                auto.setNajeto(Double.parseDouble(h[11]));
                if (garaz.pridejAuto(auto) && Boolean.parseBoolean(h[12])) {
                    autoNaVylete = auto;
                }
            }
            IO.println("Načteno aut ze souboru: " + garaz.getPocetAut());
        } catch (Exception e) {
            IO.println("Soubor s garáží se nepodařilo načíst: " + e.getMessage());
        }
    }

    // ===================== Odesílání odpovědí =====================

    private static void posliJson(HttpExchange ex, int status, String json) throws IOException {
        posli(ex, status, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    private static void posliChybu(HttpExchange ex, int status, String zprava) throws IOException {
        posliJson(ex, status, "{\"chyba\":" + str(zprava) + "}");
    }

    private static void posli(HttpExchange ex, int status, String typ, byte[] data) throws IOException {
        ex.getResponseHeaders().set("Content-Type", typ);
        ex.sendResponseHeaders(status, data.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(data);
        }
    }

    // ===================== Soubory frontendu =====================

    private void obsluzSoubor(HttpExchange ex) throws IOException {
        String cesta = ex.getRequestURI().getPath();
        if (cesta.equals("/")) cesta = "/index.html";

        Path soubor = slozkaFrontendu.resolve(cesta.substring(1)).normalize();
        // Ochrana, aby si nikdo nemohl stáhnout soubory mimo složku public (např. /../src/...)
        if (!soubor.startsWith(slozkaFrontendu) || !Files.isRegularFile(soubor)) {
            posli(ex, 404, "text/plain; charset=utf-8", "Nenalezeno".getBytes(StandardCharsets.UTF_8));
            return;
        }
        posli(ex, 200, typSouboru(soubor.toString()), Files.readAllBytes(soubor));
    }

    private static String typSouboru(String nazev) {
        if (nazev.endsWith(".html")) return "text/html; charset=utf-8";
        if (nazev.endsWith(".css")) return "text/css; charset=utf-8";
        if (nazev.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (nazev.endsWith(".svg")) return "image/svg+xml";
        if (nazev.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }
}
