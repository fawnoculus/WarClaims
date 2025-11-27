package net.fawnoculus.warclaims.utils;

// In more modern versions these where ordered as Alpha Red Green Blue,
// here they are in the inverse order for some reason (Blue Green Red Alpha)
public class ColorUtil {
    public static int getAlpha(int argb) {
        return argb & 0xFF;
    }

    public static int getRed(int argb) {
        return argb >> 8 & 0xFF;
    }

    public static int getGreen(int argb) {
        return argb >> 16 & 0xFF;
    }

    public static int getBlue(int argb) {
        return argb >> 24 & 0xFF;
    }

    public static int fromARGB(int alpha, int red, int green, int blue) {
        return blue << 24 | green << 16 | red << 8 | alpha;
    }

    public static int fromRGB(int red, int green, int blue) {
        return fromARGB(255, red, green, blue);
    }

    public static int withAlpha(int alpha, int rgb) {
        return alpha | rgb & 0xFFFFFF00;
    }

    public static int lerp(float delta, int start, int end) {
        int i = MathUtil.lerp(delta, getAlpha(start), getAlpha(end));
        int j = MathUtil.lerp(delta, getRed(start), getRed(end));
        int k = MathUtil.lerp(delta, getGreen(start), getGreen(end));
        int l = MathUtil.lerp(delta, getBlue(start), getBlue(end));
        return fromARGB(i, j, k, l);
    }
}
