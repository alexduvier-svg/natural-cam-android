package br.com.duvier.camera;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.util.Range;
import android.util.Size;
import android.util.SizeF;
import org.json.*;
import java.util.*;

public final class CameraCaps {
    public final String id,openId,physicalId;
    public final CameraCharacteristics characteristics,hostCharacteristics;
    public final boolean front,raw,manual,manualColor,awbLock,aeLock;
    public final int sensorOrientation;
    public final List<Integer> extensions=new ArrayList<>();
    public final List<Size> jpegSizes,rawSizes,maxJpegSizes,maxRawSizes;
    public final float minZoom,maxZoom,minFocus,focalMm;
    public final double equivalent35;
    public final Range<Integer> iso;
    public final Range<Long> exposure;
    public final String label;
    public final boolean hdr,bokeh;
    public final Set<CaptureRequest.Key> bokehKeys=new HashSet<>(),hdrKeys=new HashSet<>();
    public final Set<CaptureRequest.Key<?>> physicalKeys=new HashSet<>();

    CameraCaps(CameraManager manager,String id)throws CameraAccessException{this(manager,id,id,null);}
    CameraCaps(CameraManager manager,String id,String host,String physical)throws CameraAccessException {
        this.id=id;openId=host;physicalId=physical;characteristics=manager.getCameraCharacteristics(id);hostCharacteristics=physical==null?characteristics:manager.getCameraCharacteristics(host);
        front=Integer.valueOf(CameraCharacteristics.LENS_FACING_FRONT).equals(characteristics.get(CameraCharacteristics.LENS_FACING));
        raw=hasCapability(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW);
        manual=hasCapability(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR);
        manualColor=hasCapability(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING);
        awbLock=Boolean.TRUE.equals(characteristics.get(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE));
        aeLock=Boolean.TRUE.equals(characteristics.get(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE));
        Integer ori=characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);sensorOrientation=ori==null?90:ori;
        StreamConfigurationMap normal=characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        StreamConfigurationMap max=characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION);
        jpegSizes=sizes(normal,ImageFormat.JPEG,true);rawSizes=sizes(normal,ImageFormat.RAW_SENSOR,true);
        maxJpegSizes=sizes(max,ImageFormat.JPEG,true);maxRawSizes=sizes(max,ImageFormat.RAW_SENSOR,true);
        Range<Float> zr=characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE);
        Float digital=characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        minZoom=zr==null?1:zr.getLower();maxZoom=zr==null?(digital==null?1:digital):zr.getUpper();
        iso=characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
        exposure=characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
        Float mf=characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);minFocus=mf==null?0:mf;
        float[] fl=characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);focalMm=fl==null||fl.length==0?0:fl[0];
        SizeF sensor=characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
        equivalent35=sensor==null?0:FrameGeometry.equivalent35(focalMm,sensor.getWidth(),sensor.getHeight());
        label=(front?"Frontal":"Traseira")+" "+id+String.format(Locale.US," · %.2f mm",focalMm)+(physical!=null?" · física":"");
        if(physical!=null){List<CaptureRequest.Key<?>> keys=manager.getCameraCharacteristics(host).getAvailablePhysicalCameraRequestKeys();if(keys!=null)physicalKeys.addAll(keys);}
        if(physical==null)try{
            CameraExtensionCharacteristics ec=manager.getCameraExtensionCharacteristics(id);extensions.addAll(ec.getSupportedExtensions());
            if(Build.VERSION.SDK_INT>=33&&extensions.contains(CameraExtensionCharacteristics.EXTENSION_BOKEH))bokehKeys.addAll(ec.getAvailableCaptureRequestKeys(CameraExtensionCharacteristics.EXTENSION_BOKEH));
            if(Build.VERSION.SDK_INT>=33&&extensions.contains(CameraExtensionCharacteristics.EXTENSION_HDR))hdrKeys.addAll(ec.getAvailableCaptureRequestKeys(CameraExtensionCharacteristics.EXTENSION_HDR));
        }catch(Exception ignored){}
        hdr=extensions.contains(CameraExtensionCharacteristics.EXTENSION_HDR);bokeh=extensions.contains(CameraExtensionCharacteristics.EXTENSION_BOKEH);
    }
    boolean hasCapability(int cap){int[] v=characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);if(v!=null)for(int c:v)if(c==cap)return true;return false;}
    public static List<Size> sizes(StreamConfigurationMap map,int format,boolean high){
        LinkedHashSet<Size> out=new LinkedHashSet<>();if(map!=null)try{Size[] a=map.getOutputSizes(format);if(a!=null)Collections.addAll(out,a);if(high){a=map.getHighResolutionOutputSizes(format);if(a!=null)Collections.addAll(out,a);}}catch(Exception ignored){}
        ArrayList<Size> list=new ArrayList<>(out);list.sort((a,b)->Long.compare(area(b),area(a)));return list;
    }
    public static long area(Size s){return (long)s.getWidth()*s.getHeight();}
    public static Size pick(List<Size> sizes,long budget){for(Size s:sizes)if(area(s)<=budget)return s;return sizes.isEmpty()?null:sizes.get(sizes.size()-1);}
    public static Size preview(Size[] options,Size photo){
        if(options==null||options.length==0)return new Size(1280,960);Size best=null;double score=Double.MAX_VALUE,wanted=photo==null?4.0/3:photo.getWidth()/(double)photo.getHeight();
        for(Size s:options){if(s.getWidth()>1920||s.getHeight()>1440)continue;double v=Math.abs(s.getWidth()/(double)s.getHeight()-wanted)*100+Math.abs(s.getWidth()-1280)/1280.;if(v<score){score=v;best=s;}}
        return best==null?options[0]:best;
    }
    public JSONObject json()throws JSONException {
        JSONObject j=new JSONObject().put("id",id).put("open_id",openId).put("pinned_physical_id",physicalId==null?JSONObject.NULL:physicalId).put("label",label)
            .put("physical_ids",new JSONArray(characteristics.getPhysicalCameraIds())).put("capability_status","reported_by_api_not_capture_verified")
            .put("raw_reported",raw).put("manual_sensor",manual).put("manual_color",manualColor).put("awb_lock",awbLock).put("ae_lock",aeLock)
            .put("sensor_orientation",sensorOrientation).put("zoom_min",minZoom).put("zoom_max",maxZoom).put("focal_length_mm_reported",focalMm).put("equivalent_35mm_calculated",equivalent35)
            .put("jpeg_default",jsonSizes(jpegSizes)).put("raw_default",jsonSizes(rawSizes)).put("jpeg_maximum_sensor_mode",jsonSizes(maxJpegSizes)).put("raw_maximum_sensor_mode",jsonSizes(maxRawSizes))
            .put("extensions_reported",new JSONArray(extensions)).put("iso_range",iso==null?JSONObject.NULL:iso.toString()).put("exposure_ns_range",exposure==null?JSONObject.NULL:exposure.toString());
        SizeF size=characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);j.put("sensor_mm",size==null?JSONObject.NULL:size.toString());
        j.put("ultra_high_resolution_reported",hasCapability(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR)).put("remosaic_reprocessing_reported",hasCapability(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_REMOSAIC_REPROCESSING));
        Size binning=characteristics.get(CameraCharacteristics.SENSOR_INFO_BINNING_FACTOR),pixels=characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE),maximumPixels=characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION);j.put("binning_factor_reported",binning==null?JSONObject.NULL:binning.toString()).put("pixel_array_default",pixels==null?JSONObject.NULL:pixels.toString()).put("pixel_array_maximum",maximumPixels==null?JSONObject.NULL:maximumPixels.toString());
        Object black=characteristics.get(CameraCharacteristics.SENSOR_BLACK_LEVEL_PATTERN);j.put("black_level_reported",black==null?JSONObject.NULL:black.toString());
        Object white=characteristics.get(CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL);j.put("white_level_reported",white==null?JSONObject.NULL:white);
        return j;
    }
    static JSONArray jsonSizes(List<Size> list)throws JSONException{JSONArray a=new JSONArray();for(Size s:list)a.put(new JSONObject().put("width",s.getWidth()).put("height",s.getHeight()).put("pixels",area(s)));return a;}
}
