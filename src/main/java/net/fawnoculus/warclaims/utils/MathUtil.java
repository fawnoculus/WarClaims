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
}
