package br.com.duvier.camera;
import org.json.*;
import java.util.*;
/** Collector, conservative action and auditor. Deterministic metrics, not a trained scene model. */
public final class SceneIntelligence {
    private SceneIntelligence(){}
    public static JSONObject collect(int[] argb,int width,int height,int hour)throws JSONException {
        int[] histogram=new int[256];int dark=0,bright=0;double sum=0,edge=0,rs=0,gs=0,bs=0;
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            int c=argb[y*width+x],r=(c>>16)&255,g=(c>>8)&255,b=c&255,l=(54*r+183*g+19*b)>>8;histogram[l]++;sum+=l;rs+=r;gs+=g;bs+=b;if(l<26)dark++;if(l>249)bright++;
            if(x>0){int p=argb[y*width+x-1];int prev=(54*((p>>16)&255)+183*((p>>8)&255)+19*(p&255))>>8;edge+=Math.abs(l-prev);}
        }
        int n=Math.max(1,width*height),median=percentile(histogram,n,.5),p10=percentile(histogram,n,.1),p90=percentile(histogram,n,.9);
        String environment=median<45?"low_light":bright>n*.035&&dark>n*.15?"backlight_or_high_contrast":p90-p10>155?"high_contrast":"moderate_light";
        return new JSONObject().put("source","downsampled_preview_not_RAW_sensor_statistics").put("trained_model",false).put("local_hour_context_only",hour)
            .put("environment_heuristic",environment).put("mean_luma",sum/n/255).put("median_luma",median/255.).put("p10",p10/255.).put("p90",p90/255.)
            .put("shadow_fraction",dark/(double)n).put("highlight_clipped_fraction",bright/(double)n).put("edge_energy",edge/n)
            .put("rgb_means",new JSONArray(new double[]{rs/n,gs/n,bs/n})).put("clock_does_not_force_white_balance",true);
    }
    public static int highlightProtectionSteps(JSONObject collected,double evStep,boolean userHasPriority){
        if(collected==null||userHasPriority||evStep<=0)return 0;
        double clipped=collected.optDouble("highlight_clipped_fraction",0),median=collected.optDouble("median_luma",.5);
        if(clipped>.04&&median>.38)return -(int)Math.max(0,Math.floor(Math.min(.67,(clipped-.03)*3)/evStep+1e-6));
        return 0;
    }
    public static JSONObject audit(String requestedId,String actualId,double requestedZoom,double opticalRatio,boolean rawSaved,boolean timestampsMatch,int jpegW,int jpegH,RawDngCrop.Crop crop)throws JSONException {
        boolean lensConfirmed=actualId!=null&&actualId.equals(requestedId);
        return new JSONObject().put("role","auditor").put("lens_requested",requestedId).put("lens_observed",actualId==null?JSONObject.NULL:actualId).put("lens_identity_confirmed",lensConfirmed)
            .put("requested_global_zoom",requestedZoom).put("optical_ratio_calculated",opticalRatio).put("digital_crop_ratio",FrameGeometry.localZoom(requestedZoom,opticalRatio))
            .put("raw_saved",rawSaved).put("raw_jpeg_timestamp_match",timestampsMatch).put("jpeg_width",jpegW).put("jpeg_height",jpegH)
            .put("raw_default_crop_pixels",crop==null?JSONObject.NULL:(long)crop.width*crop.height).put("native_200mp_verified",false)
            .put("resolution_origin","sensor_output_then_explicit_crop; no_upscale").put("limits","Field-of-view calibration and image quality require the physical OPPO; API metadata is not a comparative quality test.");
    }
    private static int percentile(int[] h,int n,double q){int target=(int)(n*q),count=0;for(int i=0;i<h.length;i++){count+=h[i];if(count>=target)return i;}return 255;}
}
