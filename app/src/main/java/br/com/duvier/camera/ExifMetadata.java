package br.com.duvier.camera;
import androidx.exifinterface.media.ExifInterface;
import java.io.*;
import org.json.*;
/** Photographic metadata is copied explicitly; dimensions/orientation describe the exported pixels. */
final class ExifMetadata {
    static final String[] TAGS={ExifInterface.TAG_MAKE,ExifInterface.TAG_MODEL,ExifInterface.TAG_LENS_MAKE,ExifInterface.TAG_LENS_MODEL,ExifInterface.TAG_FOCAL_LENGTH,ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,ExifInterface.TAG_DIGITAL_ZOOM_RATIO,ExifInterface.TAG_EXPOSURE_TIME,ExifInterface.TAG_F_NUMBER,ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,ExifInterface.TAG_DATETIME_ORIGINAL,ExifInterface.TAG_DATETIME,ExifInterface.TAG_DATETIME_DIGITIZED,ExifInterface.TAG_OFFSET_TIME_ORIGINAL,ExifInterface.TAG_WHITE_BALANCE,ExifInterface.TAG_EXPOSURE_BIAS_VALUE,ExifInterface.TAG_FLASH,ExifInterface.TAG_METERING_MODE};
    static void copy(File source,File destination,int w,int h,EditRecipe recipe)throws IOException {
        ExifInterface from=new ExifInterface(source),to=new ExifInterface(destination);
        for(String tag:TAGS){String v=from.getAttribute(tag);if(v!=null)to.setAttribute(tag,v);}
        to.setAttribute(ExifInterface.TAG_ORIENTATION,"1");to.setAttribute(ExifInterface.TAG_IMAGE_WIDTH,Integer.toString(w));to.setAttribute(ExifInterface.TAG_IMAGE_LENGTH,Integer.toString(h));to.setAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION,Integer.toString(w));to.setAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION,Integer.toString(h));
        double digital=from.getAttributeDouble(ExifInterface.TAG_DIGITAL_ZOOM_RATIO,1)*recipe.cropZoom;
        if(digital>0)to.setAttribute(ExifInterface.TAG_DIGITAL_ZOOM_RATIO,Double.toString(digital));
        int focal35=from.getAttributeInt(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,0);if(focal35>0)to.setAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,Integer.toString(Math.round(focal35*recipe.cropZoom)));
        to.setAttribute(ExifInterface.TAG_COLOR_SPACE,"1");to.setAttribute(ExifInterface.TAG_SOFTWARE,"Natural Cam 0.2.1-candidate.1");
        to.setAttribute(ExifInterface.TAG_USER_COMMENT,"Original preserved; "+StyleCatalog.get(recipe.style).family+" / "+StyleCatalog.get(recipe.style).name+"; virtual depth is not optical aperture.");
        to.saveAttributes();
    }
    private static String rational(double value){return Math.round(Math.max(0,value)*1000000)+"/1000000";}
    static void capture(File jpeg,JSONObject metadata)throws IOException {
        ExifInterface e=new ExifInterface(jpeg);JSONObject a=metadata.optJSONObject("actual_camera_metadata");if(a==null)a=new JSONObject();
        double focal=a.optDouble("focal_length_mm",0),equivalent=metadata.optDouble("equivalent_35mm_effective_calculated",0);
        if(focal>0)e.setAttribute(ExifInterface.TAG_FOCAL_LENGTH,rational(focal));
        if(equivalent>0)e.setAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,Integer.toString((int)Math.round(equivalent)));
        e.setAttribute(ExifInterface.TAG_LENS_MAKE,android.os.Build.MANUFACTURER);
        String id=metadata.isNull("actual_camera_id")?"":metadata.optString("actual_camera_id","");if(!id.isEmpty()&&focal>0)e.setAttribute(ExifInterface.TAG_LENS_MODEL,String.format(java.util.Locale.US,"%s camera %s %.4fmm",android.os.Build.MANUFACTURER,id,focal));
        e.setAttribute(ExifInterface.TAG_DIGITAL_ZOOM_RATIO,Double.toString(metadata.optDouble("digital_crop_ratio",1)));
        e.setAttribute(ExifInterface.TAG_SOFTWARE,"Natural Cam 0.2.1-candidate.1");e.saveAttributes();
    }
}
