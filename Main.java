import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

public final class Main {
    private Main() { }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8000;
        final Path publicDirectory = Paths.get("public").toAbsolutePath().normalize();
        if (!Files.isDirectory(publicDirectory)) {
            throw new IOException("Run this command from the project root; public/ was not found.");
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", exchange -> serveFile(exchange, publicDirectory));
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Tidepool is ready at http://localhost:" + port);
    }

    private static void serveFile(HttpExchange exchange, Path publicDirectory) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "GET");
            sendText(exchange, 405, "Method not allowed");
            return;
        }

        String requestPath = exchange.getRequestURI().getPath();
        if (requestPath == null || "/".equals(requestPath)) {
            requestPath = "/index.html";
        }
        Path file = publicDirectory.resolve(requestPath.substring(1)).normalize();
        if (!file.startsWith(publicDirectory) || !Files.isRegularFile(file)) {
            sendText(exchange, 404, "Not found");
            return;
        }

        byte[] content = Files.readAllBytes(file);
        String contentType = URLConnection.guessContentTypeFromName(file.getFileName().toString());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        if (file.toString().endsWith(".js")) {
            contentType = "application/javascript; charset=utf-8";
        } else if (file.toString().endsWith(".css")) {
            contentType = "text/css; charset=utf-8";
        } else if (file.toString().endsWith(".html")) {
            contentType = "text/html; charset=utf-8";
        }
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        headers.set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(content);
        }
    }

    private static void sendText(HttpExchange exchange, int status, String message) throws IOException {
        byte[] content = message.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, content.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(content);
        }
    }
}