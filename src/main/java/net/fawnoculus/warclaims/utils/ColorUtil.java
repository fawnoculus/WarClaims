package net.fawnoculus.warclaims.utils;

// In more modern versions these where ordered as Alpha Red Green Blue,
// here they are in the inverse order for some reason (Blue Green Red Alpha)
public class ColorUtil {
    public static String getRGB(int argb) {
        return String.format("[R: %1$d G: %2$d B: %3$d]", getRed(argb), getGreen(argb), getBlue(argb));
    }

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

    public static int fromARGB(int alpha, int red, int green, int blue) {
        return blue << 24 | green << 16 | red << 8 | alpha;
    }

    public static int fromARGB(float alpha, float red, float green, float blue) {
        return fromARGB(
                MathUtil.clamp((int) (alpha * 255 + 0.5f), 0, 255),
                MathUtil.clamp((int) (red * 255 + 0.5f), 0, 255),
                MathUtil.clamp((int) (green * 255 + 0.5f), 0, 255),
                MathUtil.clamp((int) (blue * 255 + 0.5f), 0, 255)
        );
    }

    public static int fromRGB(int red, int green, int blue) {
        return fromARGB(255, red, green, blue);
    }

    public static int fromRGB(float red, float green, float blue) {
        return fromARGB(1f, red, green, blue);
    }

    public static int fromHSV(float hue, float saturation, float value) {
        if (saturation == 0) {
            return fromRGB((int) (value * 255.0f + 0.5f),
                    (int) (value * 255.0f + 0.5f),
                    (int) (value * 255.0f + 0.5f)
            );
        }

        float red = 0f;
        float green = 0f;
        float blue = 0f;

        float clampedHue = MathUtil.clamp(hue * 6.0f, 0f, 6f);
        float hueExtra = clampedHue - (float) MathUtil.floor(clampedHue);
        float p = value * (1.0f - saturation);
        float q = value * (1.0f - saturation * hueExtra);
        float t = value * (1.0f - (saturation * (1.0f - hueExtra)));

        switch ((int) clampedHue) {
            case 0:
                red = value;
                green = t;
                blue = p;
                break;
            case 1:
                red = q;
                green = value;
                blue = p;
                break;
            case 2:
                red = p;
                green = value;
                blue = t;
                break;
            case 3:
                red = p;
                green = q;
                blue = value;
                break;
            case 4:
                red = t;
                green = p;
                blue = value;
                break;
            case 5:
                red = value;
                green = p;
                blue = q;
                break;
        }

        return fromRGB(red, green, blue);
    }
}
