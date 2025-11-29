package net.fawnoculus.warclaims.utils;

public class MathUtil {
    public static int lerp(float delta, int start, int end) {
        return start + floor(delta * (end - start));
    }

    public static int floor(float value) {
        int i = (int)value;
        return value < i ? i - 1 : i;
    }

    public static int floor(double value) {
        int i = (int)value;
        return value < i ? i - 1 : i;
    }

    public static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    public static long clamp(long value, long min, long max) {
        return Math.min(Math.max(value, min), max);
    }

    public static float clamp(float value, float min, float max) {
        return Math.min(Math.max(value, min), max);
    }

    public static double clamp(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }
}
