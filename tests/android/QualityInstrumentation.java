package br.com.duvier.camera;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import androidx.exifinterface.media.ExifInterface;
import java.io.*;
import org.json.*;

/** Installed separately for QA; this instrumentation is not in the shipped app manifest. */
public final class QualityInstrumentation extends Instrumentation {
    private String scope="full";private boolean includeDepth=true;private int assertions=0;private JSONObject report=new JSONObject();private Context context;
    private void phase(String text){Bundle b=new Bundle();b.putString("phase",text);sendStatus(1,b);}
    private void check(boolean value,String name)throws Exception{assertions++;if(!value)throw new AssertionError(name);report.put(name,true);}
    public void onCreate(Bundle args){super.onCreate(args);scope=args==null?"full":args.getString("scope","full");includeDepth="full".equals(scope);start();}
    public void onStart(){
        context=getTargetContext();new Thread(()->{try{if("capture".equals(scope))runCaptureCheck();else runChecks();report.put("assertions",assertions);report.put("status","passed");}catch(Throwable e){try{report.put("status","failed").put("error",e.toString());}catch(Exception ignored){}}
            try{File f=new File(context.getCacheDir(),"native-qa.json");try(FileWriter w=new FileWriter(f)){w.write(report.toString(2));}}catch(Exception ignored){}
            Bundle result=new Bundle();result.putString("report",report.toString());finish(report.optString("status").equals("passed")?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
        },"Natural-QA").start();
    }
    private void runCaptureCheck()throws Exception {
        phase("opening Camera2 for actual JPEG capture");Activity cam=startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
        java.lang.reflect.Field busy=MainActivity.class.getDeclaredField("busy"),engine=MainActivity.class.getDeclaredField("engine"),last=MainActivity.class.getDeclaredField("lastPhoto");busy.setAccessible(true);engine.setAccessible(true);last.setAccessible(true);boolean[] ready={false};long deadline=SystemClock.elapsedRealtime()+90000;
        while(SystemClock.elapsedRealtime()<deadline){runOnMainSync(()->{try{ready[0]=!busy.getBoolean(cam);}catch(Exception ignored){}});if(ready[0])break;Thread.sleep(150);}
        check(ready[0],"capture_camera_became_ready");phase("capturing emulator JPEG");CameraEngine e=(CameraEngine)engine.get(cam);e.capture(0);Uri[] saved={null};deadline=SystemClock.elapsedRealtime()+90000;
        while(SystemClock.elapsedRealtime()<deadline){runOnMainSync(()->{try{saved[0]=(Uri)last.get(cam);}catch(Exception ignored){}});if(saved[0]!=null)break;Thread.sleep(150);}
        check(saved[0]!=null,"camera2_capture_saved_jpeg");try(InputStream in=context.getContentResolver().openInputStream(saved[0])){BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeStream(in,null,o);check(o.outWidth>0&&o.outHeight>0,"camera2_capture_reopens");report.put("captured_width",o.outWidth).put("captured_height",o.outHeight);}
        report.put("physical_oppo_test",false);phase("CAPTURE_READY");Thread.sleep(8000);
    }
    private void runChecks()throws Exception {
        phase("creating deterministic image fixture");File source=new File(context.getCacheDir(),"native-qa-source.jpg");int w=1024,h=768;Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);int[] pixels=new int[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++){int r=x*255/(w-1),g=y*255/(h-1),b=((x/16+y/16)%2)*140+50;pixels[y*w+x]=0xff000000|(r<<16)|(g<<8)|b;}bitmap.setPixels(pixels,0,w,0,0,w,h);try(OutputStream out=new FileOutputStream(source)){bitmap.compress(Bitmap.CompressFormat.JPEG,98,out);}bitmap.recycle();
        ExifInterface exif=new ExifInterface(source);exif.setAttribute(ExifInterface.TAG_ORIENTATION,"6");exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH,"123/10");exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,"72");exif.setAttribute(ExifInterface.TAG_LENS_MODEL,"test-optic");exif.setAttribute(ExifInterface.TAG_DIGITAL_ZOOM_RATIO,"1.0");exif.saveAttributes();
        try(PhotoSource p=new PhotoSource(source)){check(p.width==768&&p.height==1024,"oriented_dimensions");Bitmap preview=p.preview(512,1);check(preview.getWidth()==384&&preview.getHeight()==512,"oriented_preview_dimensions");Bitmap region=p.region(new Rect(0,0,p.width,p.height));check(region.getWidth()==p.width&&region.getHeight()==p.height,"region_orientation_dimensions");int c=region.getPixel(40,40);check(((c>>8)&255)>200&&((c>>16)&255)<40,"orientation_pixel_mapping");region.recycle();preview.recycle();}
        phase("native JPEG full-resolution stripe export");EditRecipe recipe=new EditRecipe();recipe.cropZoom=2;recipe.style=12;recipe.exposure=.15f;recipe.vignette=.2f;recipe.sharpness=.1f;File output=new File(context.getCacheDir(),"native-qa-output.jpg");ImageExporter.Result result=ImageExporter.export(context,source,output,recipe,null);check(result.width==384&&result.height==512,"crop_export_preserves_real_pixels");ExifInterface written=new ExifInterface(output);check(written.getAttributeInt(ExifInterface.TAG_ORIENTATION,0)==1,"export_orientation_normalized");check(Math.abs(written.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH,0)-12.3)<.001,"focal_length_preserved");check(written.getAttributeInt(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,0)==144,"effective_focal_includes_crop");check("test-optic".equals(written.getAttribute(ExifInterface.TAG_LENS_MODEL)),"lens_model_preserved");report.put("digital_zoom_exif",written.getAttributeDouble(ExifInterface.TAG_DIGITAL_ZOOM_RATIO,0));check(written.getAttributeDouble(ExifInterface.TAG_DIGITAL_ZOOM_RATIO,0)==2,"digital_zoom_is_explicit");
        Bitmap expected;try(PhotoSource p=new PhotoSource(source)){Rect crop=p.crop(2);Bitmap original=p.region(crop);expected=ImageExporter.preview(original,recipe,null);original.recycle();}Bitmap actual=BitmapFactory.decodeFile(output.getAbsolutePath());check(actual.getWidth()==expected.getWidth()&&actual.getHeight()==expected.getHeight(),"preview_export_same_dimensions");long error=0;int n=actual.getWidth()*actual.getHeight();int[] a=new int[n],b=new int[n];actual.getPixels(a,0,actual.getWidth(),0,0,actual.getWidth(),actual.getHeight());expected.getPixels(b,0,expected.getWidth(),0,0,expected.getWidth(),expected.getHeight());for(int i=0;i<n;i++)for(int shift=0;shift<=16;shift+=8)error+=Math.abs(((a[i]>>shift)&255)-((b[i]>>shift)&255));double mae=error/(n*3.);report.put("jpeg_vs_pixel_reference_mae",mae);check(mae<2.4,"stripe_processing_matches_full_frame");actual.recycle();expected.recycle();
        phase("publishing a real JPEG with MediaStore");MediaWriter writer=new MediaWriter(context);Uri published=writer.image("NATURAL_QA_"+System.currentTimeMillis()+".jpg","image/jpeg",out->{try(InputStream in=new FileInputStream(output)){PhotoSource.copy(in,out);}});check(published!=null,"mediastore_published");try(InputStream in=context.getContentResolver().openInputStream(published)){BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeStream(in,null,o);check(o.outWidth==384&&o.outHeight==512,"published_file_reopens");}
        phase("writing paired metadata");Uri json=writer.document("NATURAL_QA_"+System.currentTimeMillis()+".json","application/json",out->out.write(recipe.json().toString().getBytes("UTF-8")));check(json!=null,"recipe_saved_separately");
        phase("opening editor on fixture");Uri sourceUri=writer.image("NATURAL_QA_SOURCE_"+System.currentTimeMillis()+".jpg","image/jpeg",out->{try(InputStream in=new FileInputStream(source)){PhotoSource.copy(in,out);}});Intent intent=new Intent(context,EditorActivity.class).setData(sourceUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("recipe",recipe.json().toString());Activity editor=startActivitySync(intent);waitForIdleSync();
        long deadline=SystemClock.elapsedRealtime()+45000;java.lang.reflect.Field rendered=EditorActivity.class.getDeclaredField("rendered");rendered.setAccessible(true);boolean[] ready={false};
        while(SystemClock.elapsedRealtime()<deadline){runOnMainSync(()->{try{ready[0]=rendered.get(editor)!=null;}catch(Exception ignored){}});if(ready[0])break;Thread.sleep(150);}
        check(ready[0],"editor_preview_rendered");report.put("editor_opened",true);phase("EDITOR_READY");Thread.sleep(12000);
        runOnMainSync(editor::finish);Intent camera=new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);Activity cam=startActivitySync(camera);waitForIdleSync();
        java.lang.reflect.Field busy=MainActivity.class.getDeclaredField("busy"),stage=MainActivity.class.getDeclaredField("stage"),root=MainActivity.class.getDeclaredField("root"),status=MainActivity.class.getDeclaredField("status");busy.setAccessible(true);stage.setAccessible(true);root.setAccessible(true);status.setAccessible(true);boolean[] cameraReady={false};int[] widths={0,0};String[] text={""};deadline=SystemClock.elapsedRealtime()+45000;
        while(SystemClock.elapsedRealtime()<deadline){runOnMainSync(()->{try{cameraReady[0]=!busy.getBoolean(cam);android.view.View s=(android.view.View)stage.get(cam),r=(android.view.View)root.get(cam);widths[0]=s.getWidth();widths[1]=r.getWidth()-r.getPaddingLeft()-r.getPaddingRight();text[0]=((android.widget.TextView)status.get(cam)).getText().toString();}catch(Exception ignored){}});if(cameraReady[0])break;Thread.sleep(150);}
        report.put("camera_status",text[0]);check(widths[0]>0&&widths[0]==widths[1],"camera_preview_spans_available_width");phase("CAMERA_READY");Thread.sleep(12000);
        report.put("depth_android_requested",includeDepth);if(includeDepth){
        phase("local depth model inference");long start=SystemClock.elapsedRealtime();Bitmap guide;try(PhotoSource p=new PhotoSource(source)){guide=p.preview(400,2);}try(DepthEngine.Bokeh depth=DepthEngine.prepare(context,guide,.5f,.42f,5.6f)){int[] small=new int[guide.getWidth()*guide.getHeight()];guide.getPixels(small,0,guide.getWidth(),0,0,guide.getWidth(),guide.getHeight());depth.apply(small,guide.getWidth(),guide.getHeight(),0,guide.getHeight());check(small.length>0,"depth_model_and_renderer_execute");}guide.recycle();report.put("depth_ms_virtual_device",SystemClock.elapsedRealtime()-start);
        }


    }
}
