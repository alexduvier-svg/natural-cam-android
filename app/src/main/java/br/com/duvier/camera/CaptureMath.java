package br.com.duvier.camera;

/** Deterministic geometry shared by capture, diagnostics and tests. */
public final class CaptureMath {
    private CaptureMath() {}
    public static int jpegOrientation(int sensor, int device, boolean front) {
        return (sensor + (front ? -device : device) + 720) % 360;
    }
    public static int exifOrientation(int rotation) {
        switch ((rotation + 360) % 360) { case 90:return 6; case 180:return 3; case 270:return 8; default:return 1; }
    }
    public static double pixelsAfterCrop(double pixels, double linearZoom) {
        if (!(linearZoom >= 1.0)) throw new IllegalArgumentException("Zoom must be >=1");
        return pixels / (linearZoom * linearZoom);
    }
    /** UI mapping only. Extension strength is not a physical aperture calibration. */
    public static int experimentalBokehStrength(double simulatedFNumber) {
        if (!(simulatedFNumber >= 1.4 && simulatedFNumber <= 16)) throw new IllegalArgumentException("f-number");
        return (int)Math.round(100 * (1 / simulatedFNumber - 1 / 16.0) / (1 / 1.4 - 1 / 16.0));
    }
    public static double clamp(double x, double min, double max) { return Math.max(min, Math.min(max, x)); }
}
