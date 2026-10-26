package com.cosyhome.tv;

/**
 * Sunrise and sunset from latitude and longitude, offline. The standard sunrise equation
 * (https://en.wikipedia.org/wiki/Sunrise_equation), good to a minute or two, which is plenty for
 * deciding whether the valley shows dawn or day.
 */
final class Sun {

    private Sun() {
    }

    /** {sunrise, sunset} in epoch milliseconds for the UTC day containing {@code atMillis}. */
    static long[] riseSet(long atMillis, double latitude, double longitude) {
        double julianNoon = Math.floor(atMillis / 86_400_000.0) + 2440587.5 + 0.5;
        double n = Math.round(julianNoon - 2451545.0 + 0.0008);
        double meanNoon = n - longitude / 360.0;
        double m = norm(357.5291 + 0.98560028 * meanNoon);
        double mr = Math.toRadians(m);
        double c = 1.9148 * Math.sin(mr) + 0.0200 * Math.sin(2 * mr) + 0.0003 * Math.sin(3 * mr);
        double lambda = Math.toRadians(norm(m + c + 180 + 102.9372));
        double transit = 2451545.0 + meanNoon + 0.0053 * Math.sin(mr) - 0.0069 * Math.sin(2 * lambda);
        double sinDecl = Math.sin(lambda) * Math.sin(Math.toRadians(23.4397));
        double cosDecl = Math.cos(Math.asin(sinDecl));
        double lat = Math.toRadians(latitude);
        double cosHour = (Math.sin(Math.toRadians(-0.833)) - Math.sin(lat) * sinDecl) / (Math.cos(lat) * cosDecl);
        // Polar day or night never happens in Wales; clamp so the maths stays defined anywhere.
        double hourAngle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cosHour))));
        return new long[]{toMillis(transit - hourAngle / 360.0), toMillis(transit + hourAngle / 360.0)};
    }

    private static double norm(double degrees) {
        double d = degrees % 360.0;
        return d < 0 ? d + 360.0 : d;
    }

    private static long toMillis(double julian) {
        return Math.round((julian - 2440587.5) * 86_400_000.0);
    }
}
