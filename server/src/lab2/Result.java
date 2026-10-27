package lab2;

public final class Result {
    public final String x;
    public final String y;
    public final String r;
    public final boolean hit;
    public final long timestamp;
    public final long executionNanos;

    public Result(String x, String y, String r, boolean hit, long timestamp, long executionNanos) {
        this.x = x;
        this.y = y;
        this.r = r;
        this.hit = hit;
        this.timestamp = timestamp;
        this.executionNanos = executionNanos;
    }

    public String toJson() {
        return "{\"x\":" + Json.str(x)
                + ",\"y\":" + Json.str(y)
                + ",\"r\":" + Json.str(r)
                + ",\"hit\":" + hit
                + ",\"timestamp\":" + timestamp
                + ",\"executionNanos\":" + executionNanos + "}";
    }
}
