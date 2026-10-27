package lab2;

import com.fastcgi.FCGIInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        FCGIInterface fcgi = new FCGIInterface();
        RequestHandler handler = new RequestHandler();
        // Сервер живёт между запросами, поэтому история в handler сохраняется
        while (fcgi.FCGIaccept() >= 0) {
            long start = System.nanoTime();
            RequestHandler.Response response;
            try {
                Properties params = FCGIInterface.request.params;
                String body = readBody(System.in, params.getProperty("CONTENT_LENGTH"));
                response = handler.handle(
                        params.getProperty("REQUEST_METHOD", ""),
                        params.getProperty("CONTENT_TYPE"),
                        body,
                        start);
            } catch (Exception e) {
                response = new RequestHandler.Response(500, "Internal Server Error",
                        "{\"ok\":false,\"error\":\"Внутренняя ошибка сервера.\"}");
            }
            send(response);
        }
    }

    // null, если тело больше лимита
    private static String readBody(InputStream in, String contentLength) throws IOException {
        int length;
        try {
            length = contentLength == null || contentLength.isEmpty() ? 0 : Integer.parseInt(contentLength);
        } catch (NumberFormatException e) {
            return null;
        }
        if (length < 0 || length > RequestHandler.MAX_BODY) return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream(length);
        byte[] buffer = new byte[length];
        int total = 0;
        while (total < length) {
            int n = in.read(buffer, total, length - total);
            if (n < 0) break;
            total += n;
        }
        out.write(buffer, 0, total);
        return out.toString(StandardCharsets.UTF_8);
    }

    private static void send(RequestHandler.Response response) {
        byte[] body = response.body.getBytes(StandardCharsets.UTF_8);
        // -nph в конфиге Apache: сервер сам пишет строку статуса
        String headers = "HTTP/1.1 " + response.status + " " + response.reason + "\r\n"
                + "Content-Type: application/json; charset=UTF-8\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Cache-Control: no-store\r\n"
                + "\r\n";
        System.out.write(headers.getBytes(StandardCharsets.US_ASCII), 0, headers.length());
        System.out.write(body, 0, body.length);
        System.out.flush();
    }
}
