package br.com.duvier.camera;

/** Optical and digital field of view are separate quantities. */
public final class FrameGeometry {
    private FrameGeometry() {}
    public static double equivalent35(double focalMm, double sensorW, double sensorH) {
        return focalMm > 0 && sensorW > 0 && sensorH > 0
                ? focalMm * Math.hypot(36, 24) / Math.hypot(sensorW, sensorH) : 0;
    }
    public static int[] centerCrop(int width, int height, double digitalZoom) {
        double zoom = Math.max(1, digitalZoom);
        int w = Math.max(2, ((int) Math.floor(width / zoom)) & ~1);
        int h = Math.max(2, ((int) Math.floor(height / zoom)) & ~1);
        return new int[]{((width-w)/2)&~1, ((height-h)/2)&~1, w, h};
    }
    public static double localZoom(double requested, double opticalRatio) {
        return Math.max(1, requested / Math.max(.01, opticalRatio));
    }
    public static boolean opticalCandidate(double requested, double opticalRatio) {
        return opticalRatio > 0 && opticalRatio <= requested * 1.015;
    }
}
