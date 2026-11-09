package br.com.duvier.camera;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.hardware.camera2.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Regression for updates DURING a gesture. No claim about physical OPPO colour or frame rate. */
public final class InteractionInstrumentation extends Instrumentation {
    private final JSONObject report=new JSONObject();private int assertions;
    private static Field field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f;}
    private static Object get(Object o,String name)throws Exception{return field(o,name).get(o);}
    private static void set(Object o,String name,Object value)throws Exception{field(o,name).set(o,value);}
    private static Method method(Object o,String name,Class<?>...types)throws Exception{Method m=o.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m;}
    private void phase(String text){Bundle b=new Bundle();b.putString("phase",text);sendStatus(1,b);}
    private void check(boolean pass,String name)throws Exception{assertions++;report.put(name,pass);if(!pass)throw new AssertionError(name);}
    public void onCreate(Bundle args){super.onCreate(args);start();}
    public void onStart(){new Thread(()->{try{checks();report.put("assertions",assertions).put("status","passed");}catch(Throwable e){try{report.put("status","failed").put("error",e.toString());}catch(Exception ignored){}}Bundle b=new Bundle();b.putString("report",report.toString());finish(report.optString("status").equals("passed")?Activity.RESULT_OK:Activity.RESULT_CANCELED,b);},"Interaction-QA").start();}
    private void checks()throws Exception {
        Context c=getTargetContext();phase("camera update and template policy checks");Activity cam=startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        long until=SystemClock.elapsedRealtime()+90000;boolean[] ready={false};
        while(SystemClock.elapsedRealtime()<until){runOnMainSync(()->{try{ready[0]=!field(cam,"busy").getBoolean(cam);}catch(Exception ignored){}});if(ready[0])break;Thread.sleep(80);}
        check(ready[0],"camera_ready");CameraEngine engine=(CameraEngine)get(cam,"engine");Handler handler=(Handler)get(engine,"handler");
        FutureTask<Void> policy=new FutureTask<>(()->{
            CameraDevice device=(CameraDevice)get(engine,"device");CameraCaps caps=(CameraCaps)get(engine,"caps");Method settings=method(engine,"settings",CaptureRequest.Builder.class,boolean.class,int.class);
            CaptureRequest.Builder b=device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);Object noise=b.get(CaptureRequest.NOISE_REDUCTION_MODE),edge=b.get(CaptureRequest.EDGE_MODE),colour=b.get(CaptureRequest.COLOR_CORRECTION_MODE);
            settings.invoke(engine,b,true,0);check(Objects.equals(noise,b.get(CaptureRequest.NOISE_REDUCTION_MODE)),"default_preserves_camera_noise_policy");check(Objects.equals(edge,b.get(CaptureRequest.EDGE_MODE)),"default_preserves_camera_detail_policy");
            if(caps.manual){int oldIso=(Integer)get(engine,"manualIso");long oldTime=(Long)get(engine,"manualTime");set(engine,"manualIso",caps.iso==null?100:Math.max(caps.iso.getLower(),100));set(engine,"manualTime",10000000L);CaptureRequest.Builder manual=device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);settings.invoke(engine,manual,true,0);check(Integer.valueOf(CameraMetadata.CONTROL_AWB_MODE_AUTO).equals(manual.get(CaptureRequest.CONTROL_AWB_MODE)),"manual_exposure_keeps_AWB_auto");check(Objects.equals(colour,manual.get(CaptureRequest.COLOR_CORRECTION_MODE)),"manual_exposure_preserves_template_colour_transform");set(engine,"manualIso",oldIso);set(engine,"manualTime",oldTime);}else report.put("manual_policy_test","skipped_no_manual_sensor");return null;
        });handler.post(policy);policy.get(30,TimeUnit.SECONDS);
        CameraCaps caps=(CameraCaps)get(engine,"caps");android.util.Range<Integer> range=caps.characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);long before=(Long)get(engine,"previewUpdateCount"),mid=0;
        for(int i=0;i<100;i++){if(range!=null)engine.exposureCompensation(i%2==0?range.getLower():range.getUpper());Thread.sleep(8);if(i==50)mid=(Long)get(engine,"previewUpdateCount")-before;}
        if(range!=null&&range.getUpper()>range.getLower())check(mid>0,"camera_EV_updates_before_gesture_ends");report.put("EV_requests_applied_mid_gesture",mid);
        int generation=(Integer)get(engine,"generation");before=(Long)get(engine,"zoomApplyCount");mid=0;
        for(int i=0;i<100;i++){engine.zoom(1+(i%12)*.006f);Thread.sleep(8);if(i==50)mid=(Long)get(engine,"zoomApplyCount")-before;}
        check(mid>0,"zoom_intents_apply_before_gesture_ends");check(generation==(Integer)get(engine,"generation"),"same_optic_zoom_does_not_reopen_camera");report.put("zoom_intents_applied_mid_gesture",mid);engine.zoom(1);engine.reset();
        runOnMainSync(()->{try{View stage=(View)get(cam,"stage"),zooms=(View)get(cam,"zooms");int[] a=new int[2],b=new int[2];stage.getLocationOnScreen(a);zooms.getLocationOnScreen(b);check(b[1]>=a[1]+stage.getHeight(),"zoom_pills_are_outside_photo");LinearLayout row=(LinearLayout)zooms;boolean pills=row.getChildCount()>0;for(int i=0;i<row.getChildCount();i++)pills&=row.getChildAt(i).getBackground()!=null;check(pills,"zoom_presets_have_pill_backgrounds");Object overlay=get(cam,"overlay");set(overlay,"x",stage.getWidth()*.6f);set(overlay,"y",stage.getHeight()*.96f);float top=(Float)method(overlay,"trackTop").invoke(overlay),bottom=(Float)method(overlay,"trackBottom").invoke(overlay);check(top>=0&&bottom<=stage.getHeight(),"EV_track_stays_inside_view_near_edge");}catch(Exception e){throw new RuntimeException(e);}});
        phase("CAMERA_INTERACTION_READY");Thread.sleep(12000);
        phase("editor continuous gesture checks");File fixture=new File(c.getCacheDir(),"interaction-fixture.jpg");Bitmap pixels=Bitmap.createBitmap(1024,768,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(pixels);canvas.drawColor(0xff929292);Paint p=new Paint();p.setColor(0xffaa6655);canvas.drawRect(0,0,512,768,p);try(OutputStream out=new FileOutputStream(fixture)){pixels.compress(Bitmap.CompressFormat.JPEG,98,out);}pixels.recycle();
        MediaWriter writer=new MediaWriter(c);Uri uri=writer.image("INTERACTION_QA_"+System.currentTimeMillis()+".jpg","image/jpeg",out->{try(InputStream in=new FileInputStream(fixture)){PhotoSource.copy(in,out);}});Activity editor=startActivitySync(new Intent(c,EditorActivity.class).setData(uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));until=SystemClock.elapsedRealtime()+60000;boolean[] loaded={false};
        while(SystemClock.elapsedRealtime()<until){runOnMainSync(()->{try{loaded[0]=get(editor,"rendered")!=null;}catch(Exception ignored){}});if(loaded[0])break;Thread.sleep(80);}check(loaded[0],"editor_initial_preview_ready");
        SeekBar[] slider={null};runOnMainSync(()->{try{set(editor,"category","Luz");set(editor,"parameter","Exposição");method(editor,"showControls").invoke(editor);slider[0]=(SeekBar)((LinearLayout)get(editor,"controls")).getChildAt(2);}catch(Exception e){throw new RuntimeException(e);}});waitForIdleSync();
        until=SystemClock.elapsedRealtime()+20000;int[] size={0,0};while(SystemClock.elapsedRealtime()<until){runOnMainSync(()->{size[0]=slider[0].getWidth();size[1]=slider[0].getHeight();});if(size[0]>100&&size[1]>10)break;Thread.sleep(30);}check(size[0]>100&&size[1]>10,"editor_slider_laid_out_before_gesture");report.put("slider_width",size[0]).put("slider_height",size[1]);
        long commits=(Long)get(editor,"previewCommitCount"),down=SystemClock.uptimeMillis();SeekBar bar=slider[0];int width=bar.getWidth(),height=bar.getHeight();touch(bar,down,MotionEvent.ACTION_DOWN,width*.5f,height*.5f);long middle=0;boolean[] small={false};
        for(int i=0;i<180;i++){float x=width*(.2f+.6f*(i%20)/19f);touch(bar,down,MotionEvent.ACTION_MOVE,x,height*.5f);Thread.sleep(8);if(i==140){middle=(Long)get(editor,"previewCommitCount")-commits;runOnMainSync(()->{try{Bitmap b=(Bitmap)get(editor,"rendered");small[0]=Math.max(b.getWidth(),b.getHeight())<=640;}catch(Exception ignored){}});}}
        runOnMainSync(()->{try{report.put("editor_progress_before_UP",bar.getProgress()).put("editor_dragging_before_UP",get(editor,"dragging")).put("editor_rendering_before_UP",get(editor,"rendering")).put("editor_pending_before_UP",get(editor,"renderPending")).put("editor_queue_before_UP",((java.util.concurrent.BlockingQueue)get(editor,"workQueue")).size()).put("editor_render_version_before_UP",get(editor,"renderVersion")).put("editor_commits_before_UP",get(editor,"previewCommitCount"));}catch(Exception e){throw new RuntimeException(e);}});
        check(middle>0,"editor_publishes_changes_before_finger_lifts");check(small[0],"interactive_preview_uses_bounded_resolution");report.put("editor_commits_mid_gesture",middle);touch(bar,down,MotionEvent.ACTION_UP,width*.8f,height*.5f);
        until=SystemClock.elapsedRealtime()+60000;boolean[] full={false};while(SystemClock.elapsedRealtime()<until){runOnMainSync(()->{try{Bitmap b=(Bitmap)get(editor,"base"),r=(Bitmap)get(editor,"rendered");full[0]=!(Boolean)get(editor,"rendering")&&r!=null&&r.getWidth()==b.getWidth()&&r.getHeight()==b.getHeight();}catch(Exception ignored){}});if(full[0])break;Thread.sleep(80);}check(full[0],"larger_preview_restored_after_gesture");report.put("last_preview_latency_ms_emulator",get(editor,"lastPreviewLatencyMs")).put("physical_OPPO_colour_validated",false).put("physical_OPPO_latency_validated",false);phase("EDITOR_INTERACTION_READY");Thread.sleep(12000);
    }
    private void touch(View v,long down,int action,float x,float y){runOnMainSync(()->{MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);v.dispatchTouchEvent(e);e.recycle();});}
}
