package br.com.duvier.camera;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import org.json.*;
import java.io.*;
import java.util.*;

/** Manufacturer facts are advisory. Only runtime camera capabilities govern capture settings. */
final class ManufacturerReference {
    static JSONObject diagnostic(Context context)throws Exception {
        JSONObject out=new JSONObject().put("reference_changes_camera_controls",false).put("sensor_chip_identity_verified",false)
            .put("cameraunit_integrated",false).put("colour_temperature_sensor_used_for_WB",false);
        if(Build.MODEL.equalsIgnoreCase("CPH2863")||Build.MODEL.equalsIgnoreCase("PMK110"))try(InputStream in=context.getAssets().open("reno16_pro_manufacturer_reference.json")){
            ByteArrayOutputStream data=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)data.write(b,0,n);out.put("reno16_reference",new JSONObject(data.toString("UTF-8")));
        }
        JSONArray sensors=new JSONArray();SensorManager manager=(SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        if(manager!=null)for(Sensor s:manager.getSensorList(Sensor.TYPE_ALL)){
            String name=s.getName().toLowerCase(Locale.ROOT),type=s.getStringType().toLowerCase(Locale.ROOT);
            if(s.getType()==Sensor.TYPE_LIGHT||name.contains("color")||name.contains("colour")||name.contains("cct")||type.contains("color")||type.contains("colour"))
                sensors.put(new JSONObject().put("name",s.getName()).put("vendor",s.getVendor()).put("type",s.getStringType()).put("units",s.getType()==Sensor.TYPE_LIGHT?"lux":"vendor_defined_not_interpreted").put("used_for_white_balance",false));
        }
        return out.put("light_colour_sensors_reported",sensors);
    }
}
