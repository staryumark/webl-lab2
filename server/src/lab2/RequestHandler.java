package lab2;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class RequestHandler {
    public static final int MAX_BODY = 1024;
    private static final int MAX_LENGTH = 40;
    private static final Set<String> X_VALUES = Set.of("-2", "-1.5", "-1", "-0.5", "0", "0.5", "1", "1.5", "2");
    private static final Pattern DECIMAL = Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)");
    private static final BigDecimal Y_MIN = new BigDecimal("-5");
    private static final BigDecimal Y_MAX = new BigDecimal("3");
    private static final BigDecimal R_MIN = new BigDecimal("2");
    private static final BigDecimal R_MAX = new BigDecimal("5");

    private final List<Result> history = new ArrayList<>();

    public static final class Response {
        public final int status;
        public final String reason;
        public final String body;

        Response(int status, String reason, String body) {
            this.status = status;
            this.reason = reason;
            this.body = body;
        }
    }

    public Response handle(String method, String contentType, String body, long startNanos) {
        if ("GET".equals(method)) {
            return ok(startNanos);
        }
        if (!"POST".equals(method)) {
            return error(405, "Method Not Allowed", "Поддерживаются только GET и POST.", startNanos);
        }
        if (contentType == null || !contentType.toLowerCase().startsWith("application/x-www-form-urlencoded")) {
            return error(415, "Unsupported Media Type", "Ожидается application/x-www-form-urlencoded.", startNanos);
        }
        if (body == null) {
            return error(413, "Payload Too Large", "Слишком большой запрос.", startNanos);
        }

        Map<String, String> params = parseForm(body);
        if (params == null) {
            return error(400, "Bad Request", "Некорректное тело запроса.", startNanos);
        }
        String x = params.get("x");
        if (x == null || !X_VALUES.contains(x)) {
            return error(400, "Bad Request", "X должен быть одним из: -2, -1.5, -1, -0.5, 0, 0.5, 1, 1.5, 2.", startNanos);
        }
        BigDecimal y;
        BigDecimal r;
        try {
            y = parseNumber(params.get("y"), "Y", Y_MIN, Y_MAX);
            r = parseNumber(params.get("r"), "R", R_MIN, R_MAX);
        } catch (IllegalArgumentException e) {
            return error(400, "Bad Request", e.getMessage(), startNanos);
        }

        boolean hit = Area.isHit(new BigDecimal(x), y, r);
        long now = System.currentTimeMillis();
        long execution = System.nanoTime() - startNanos;
        history.add(new Result(x, format(y), format(r), hit, now, execution));
        return ok(startNanos);
    }

    private Response ok(long startNanos) {
        StringBuilder results = new StringBuilder("[");
        for (int i = 0; i < history.size(); i++) {
            if (i > 0) results.append(',');
            results.append(history.get(i).toJson());
        }
        results.append(']');
        return new Response(200, "OK", "{\"ok\":true" + timing(startNanos) + ",\"results\":" + results + "}");
    }

    private Response error(int status, String reason, String message, long startNanos) {
        return new Response(status, reason, "{\"ok\":false,\"error\":" + Json.str(message) + timing(startNanos) + "}");
    }

    // Текущее время и время работы скрипта
    private static String timing(long startNanos) {
        return ",\"currentTime\":" + System.currentTimeMillis()
                + ",\"executionNanos\":" + (System.nanoTime() - startNanos);
    }

    // Строка -> BigDecimal
    private static BigDecimal parseNumber(String text, String name, BigDecimal min, BigDecimal max) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Введите " + name + ".");
        }
        if (text.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(name + " слишком длинное.");
        }
        String normalized = text.trim().replace(',', '.');
        if (!DECIMAL.matcher(normalized).matches()) {
            throw new IllegalArgumentException(name + " должно быть десятичным числом.");
        }
        BigDecimal value = new BigDecimal(normalized);
        if (value.compareTo(min) <= 0 || value.compareTo(max) >= 0) {
            throw new IllegalArgumentException(name + " должно быть больше " + min + " и меньше " + max + ".");
        }
        return value;
    }

    // "+003.50" -> "3.5"
    private static String format(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.signum() == 0) return "0";
        return stripped.toPlainString();
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> result = new HashMap<>();
        if (body.isEmpty()) return result;
        for (String pair : body.split("&", -1)) {
            int eq = pair.indexOf('=');
            if (eq < 0) return null;
            try {
                String key = URLDecoder.decode(pair.substring(0, eq), "UTF-8");
                String value = URLDecoder.decode(pair.substring(eq + 1), "UTF-8");
                if (result.put(key, value) != null) return null;
            } catch (IllegalArgumentException | UnsupportedEncodingException e) {
                return null;
            }
        }
        return result;
    }
}
