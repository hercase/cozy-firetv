package com.cosyhome.tv;

import android.graphics.Bitmap;

/**
 * Frosted-glass source for Android 11, which has no live blur (RenderEffect is Android 12).
 * The scene does not move behind the panels, so blurring it once per phase looks the same:
 * shrink it, box-blur the small copy a few times, and let the panels stretch it back up.
 */
final class Blur {

    private Blur() {
    }

    /** A copy of {@code src} shrunk by {@code factor} and box-blurred with {@code radius} (three passes). */
    static Bitmap of(Bitmap src, int factor, int radius) {
        int w = Math.max(1, src.getWidth() / factor), h = Math.max(1, src.getHeight() / factor);
        Bitmap small = Bitmap.createScaledBitmap(src, w, h, true).copy(Bitmap.Config.ARGB_8888, true);
        int[] px = new int[w * h];
        int[] tmp = new int[w * h];
        small.getPixels(px, 0, w, 0, 0, w, h);
        for (int pass = 0; pass < 3; pass++) {
            boxHorizontal(px, tmp, w, h, radius);
            boxVertical(tmp, px, w, h, radius);
        }
        small.setPixels(px, 0, w, 0, 0, w, h);
        return small;
    }

    private static void boxHorizontal(int[] in, int[] out, int w, int h, int r) {
        for (int y = 0; y < h; y++) {
            int row = y * w;
            for (int x = 0; x < w; x++) {
                int rs = 0, gs = 0, bs = 0, n = 0;
                for (int k = Math.max(0, x - r); k <= Math.min(w - 1, x + r); k++) {
                    int p = in[row + k];
                    rs += (p >> 16) & 0xFF;
                    gs += (p >> 8) & 0xFF;
                    bs += p & 0xFF;
                    n++;
                }
                out[row + x] = 0xFF000000 | ((rs / n) << 16) | ((gs / n) << 8) | (bs / n);
            }
        }
    }

    private static void boxVertical(int[] in, int[] out, int w, int h, int r) {
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int rs = 0, gs = 0, bs = 0, n = 0;
                for (int k = Math.max(0, y - r); k <= Math.min(h - 1, y + r); k++) {
                    int p = in[k * w + x];
                    rs += (p >> 16) & 0xFF;
                    gs += (p >> 8) & 0xFF;
                    bs += p & 0xFF;
                    n++;
                }
                out[y * w + x] = 0xFF000000 | ((rs / n) << 16) | ((gs / n) << 8) | (bs / n);
            }
        }
    }
}
