package br.com.duvier.camera;
import org.json.*;
import java.util.*;
/** All edits are a reversible recipe. Source pixels and RAW files are never overwritten. */
public final class EditRecipe {
    public float cropZoom=1;
    public int style=0;public float intensity=1,exposure=0,contrast=0,saturation=0,hue=0,temperature=0,tint=0,highlights=0,shadows=0,sharpness=0,grain=0,vignette=0;
    public float localExposure=0,localWarmth=0,localSaturation=0,localTexture=0,localTint=0;
    public float[] curve={0,.25f,.5f,.75f,1};
    public float[][] rgbCurves={{0,.25f,.5f,.75f,1},{0,.25f,.5f,.75f,1},{0,.25f,.5f,.75f,1}};
    public float[] hslHue=new float[8],hslSaturation=new float[8],hslLuminance=new float[8];
    public final List<float[]> strokes=new ArrayList<>();
    public boolean portrait=false;public float aperture=5.6f,focusX=.5f,focusY=.42f;
    public EditRecipe copy(){try{return from(json());}catch(Exception e){throw new IllegalStateException(e);}}
    public JSONObject json()throws JSONException{
        JSONObject j=new JSONObject().put("format","duvier_edit_recipe_v2").put("source_preserved",true).put("crop_zoom",cropZoom).put("style",style).put("style_family",StyleCatalog.get(style).family).put("style_name",StyleCatalog.get(style).name).put("styles_are_original_interpretations",true);
        String[] names={"intensity","exposure","contrast","saturation","hue","temperature","tint","highlights","shadows","sharpness","grain","vignette","localExposure","localWarmth","localSaturation","localTexture","localTint","aperture","focusX","focusY"};
        float[] values={intensity,exposure,contrast,saturation,hue,temperature,tint,highlights,shadows,sharpness,grain,vignette,localExposure,localWarmth,localSaturation,localTexture,localTint,aperture,focusX,focusY};
        for(int i=0;i<names.length;i++)j.put(names[i],values[i]);j.put("curve",new JSONArray(curve));
        JSONArray channels=new JSONArray();for(float[] c:rgbCurves)channels.put(new JSONArray(c));j.put("rgb_curves",channels);
        j.put("hsl_hue",new JSONArray(hslHue)).put("hsl_saturation",new JSONArray(hslSaturation)).put("hsl_luminance",new JSONArray(hslLuminance));
        JSONArray ss=new JSONArray();for(float[] s:strokes)ss.put(new JSONArray(s));j.put("manual_mask_strokes",ss).put("portrait",portrait).put("virtual_f_number_not_optical",true);return j;
    }
    public static EditRecipe from(JSONObject j)throws JSONException {
        EditRecipe r=new EditRecipe();r.cropZoom=(float)j.optDouble("crop_zoom",1);r.style=j.optInt("style",0);r.intensity=(float)j.optDouble("intensity",1);r.exposure=(float)j.optDouble("exposure",0);r.contrast=(float)j.optDouble("contrast",0);r.saturation=(float)j.optDouble("saturation",0);r.hue=(float)j.optDouble("hue",0);r.temperature=(float)j.optDouble("temperature",0);r.tint=(float)j.optDouble("tint",0);r.highlights=(float)j.optDouble("highlights",0);r.shadows=(float)j.optDouble("shadows",0);r.sharpness=(float)j.optDouble("sharpness",0);r.grain=(float)j.optDouble("grain",0);r.vignette=(float)j.optDouble("vignette",0);
        r.localExposure=(float)j.optDouble("localExposure",0);r.localWarmth=(float)j.optDouble("localWarmth",0);r.localSaturation=(float)j.optDouble("localSaturation",0);r.localTexture=(float)j.optDouble("localTexture",0);r.localTint=(float)j.optDouble("localTint",0);
        r.portrait=j.optBoolean("portrait",false);r.aperture=(float)j.optDouble("aperture",5.6);r.focusX=(float)j.optDouble("focusX",.5);r.focusY=(float)j.optDouble("focusY",.42);
        read(j.optJSONArray("curve"),r.curve);JSONArray curves=j.optJSONArray("rgb_curves");if(curves!=null)for(int i=0;i<Math.min(3,curves.length());i++)read(curves.optJSONArray(i),r.rgbCurves[i]);
        read(j.optJSONArray("hsl_hue"),r.hslHue);read(j.optJSONArray("hsl_saturation"),r.hslSaturation);read(j.optJSONArray("hsl_luminance"),r.hslLuminance);
        JSONArray s=j.optJSONArray("manual_mask_strokes");if(s!=null)for(int i=0;i<Math.min(1024,s.length());i++){float[] st=new float[4];read(s.optJSONArray(i),st);r.strokes.add(st);}return r;
    }
    private static void read(JSONArray j,float[] out){if(j!=null)for(int i=0;i<Math.min(j.length(),out.length);i++)out[i]=(float)j.optDouble(i,out[i]);}
}
