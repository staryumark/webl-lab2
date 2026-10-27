package lab2;

import java.math.BigDecimal;

public final class Area {
    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    private static final BigDecimal FOUR = BigDecimal.valueOf(4);

    private Area() {
    }

    // BigDecimal 
    public static boolean isHit(BigDecimal x, BigDecimal y, BigDecimal r) {
        int sx = x.signum();
        int sy = y.signum();

        // Треугольник: y <= x + R/2  ->  2y <= 2x + R
        boolean triangle = sx <= 0 && sy >= 0
                && TWO.multiply(y).compareTo(TWO.multiply(x).add(r)) <= 0;

        // Четверть круга: x² + y² <= (R/2)²  ->  4(x² + y²) <= R²
        boolean circle = sx >= 0 && sy >= 0
                && FOUR.multiply(x.multiply(x).add(y.multiply(y))).compareTo(r.multiply(r)) <= 0;

        // Прямоугольник: 0 <= x <= R, -R/2 <= y <= 0  ->  2y >= -R
        boolean rectangle = sx >= 0 && x.compareTo(r) <= 0 && sy <= 0
                && TWO.multiply(y).compareTo(r.negate()) >= 0;

        return triangle || circle || rectangle;
    }
}
