import web.Server;

// Spustí webovou verzi aplikace (backend + frontend).
// Konzolová verze se pořád spouští přes App.java.
public class WebApp {
    public static void main(String[] args) throws Exception {
        // Hostingy jako Render nastavují port přes proměnnou PORT, lokálně se použije 8080.
        String portEnv = System.getenv("PORT");
        int port = (portEnv != null) ? Integer.parseInt(portEnv) : 8080;

        // Složka s frontendem (HTML, CSS, JS), jde změnit prvním argumentem.
        String slozka = (args.length > 0) ? args[0] : "public";
        // Garáž se ukládá do souboru garaz.txt vedle složky public.
        String soubor = java.nio.file.Path.of(slozka).toAbsolutePath().getParent().resolve("garaz.txt").toString();
        Server server = new Server(port, slozka, soubor);
        server.start();
        IO.println("Server běží na http://localhost:" + port);
    }
}
