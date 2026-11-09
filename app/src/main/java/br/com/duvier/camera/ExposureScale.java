package br.com.duvier.camera;

/** Continuous gesture geometry, separate from the camera's discrete compensation steps. */
public final class ExposureScale {
    private ExposureScale(){}
    public static float clamp(float v){return Math.max(0,Math.min(1,v));}
    public static float fraction(int value,int low,int high){return high<=low?.5f:clamp((value-low)/(float)(high-low));}
    public static int steps(float fraction,int low,int high){return high<=low?low:Math.max(low,Math.min(high,Math.round(low+clamp(fraction)*(high-low))));}
    public static float thumb(float fraction,float top,float bottom,float radius){float lo=top+radius,hi=Math.max(lo,bottom-radius);return hi-clamp(fraction)*(hi-lo);}
    public static float fromY(float y,float top,float bottom,float radius){float lo=top+radius,hi=Math.max(lo,bottom-radius);return hi<=lo?.5f:clamp((hi-y)/(hi-lo));}
}
