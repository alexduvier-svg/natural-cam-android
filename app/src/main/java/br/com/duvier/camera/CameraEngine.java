package br.com.duvier.camera;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.*;
import android.hardware.camera2.*;
import android.hardware.camera2.params.*;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.*;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import org.json.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/** Camera calls are confined to one worker; zoom updates are coalesced, never queued per animation frame. */
public final class CameraEngine {
    public static final int PHOTO=0,RAW=1,HDR=2,PORTRAIT=3;
    public interface Listener {
        void capabilities(List<CameraCaps> all,CameraCaps current,double base35,float minimum,float maximum);
        void preview(Size size,int sensorOrientation,boolean front,double opticalRatio,double requestedZoom);
        void zoomApplied(double appliedZoom);
        void zoomRestored(double restoredZoom);
        void highRestored(boolean high);
        void ready(String description,boolean depthControl);
        void features(boolean hdrAvailable);
        void modeChanged(int mode);
        void state(String message,boolean busy);
        void exposure(String text);
        void compensation(float ev,boolean automatic);
        void locked(boolean locked);
        void saved(Uri image,Uri original,EditRecipe recipe,String name,String detail);
        void error(String message);
        void diagnostic(Uri uri);
        void palette(Uri uri);
    }
    private final Context context;private final CameraManager manager;private final Listener listener;
    private final HandlerThread thread=new HandlerThread("DuvierCamera");private final Handler handler;private final Executor executor;
    private final ExecutorService io=Executors.newSingleThreadExecutor();private final MediaWriter writer;
    private final ArrayList<CameraCaps> cameras=new ArrayList<>();private final ArrayList<JSONObject> checks=new ArrayList<>();
    private final ArrayList<ImageReader> deferredReaders=new ArrayList<>();private final HashSet<String> hdrFailures=new HashSet<>();
    private CameraCaps caps,main;private CameraDevice device;private CameraCaptureSession session;private CameraExtensionSession extension;
    private SurfaceTexture texture;private Surface previewSurface;private ImageReader jpegReader,rawReader;private CaptureResult lastResult;
    private Size captureSize,rawSize;private int generation=0,mode=PHOTO,pixelMode=0,fallback=0,ev=0,sceneEv=0,manualIso=0;
    private boolean active=false,configured=false,opening=false,high=false,processingOff=false,awbLocked=false,focusLocked=false,exposureLocked=false,intelligence=false,userEv=false;
    private long manualTime=0;private Float manualFocus;
    private MeteringRectangle[] metering;private double base35=24,opticalRatio=1,requestedZoom=1;private float focusX=.5f,focusY=.42f;private float aperture=5.6f;
    private final ScenePolicy scenePolicy=new ScenePolicy();
    private JSONObject scene;private Job job;private byte[] lastJpeg;private Uri lastPalettePhoto;private String lastBasename;private long lastUiExposure=0;
    private Double pendingZoom;private double queuedZoom=1;private boolean zoomScheduled=false,previewScheduled=false;
    private CameraCaps lastReadyCaps;private double lastReadyZoom=1;private int lastReadyMode=PHOTO;private boolean lastReadyHigh=false,recoveringOpen=false;
    private volatile long previewUpdateCount=0,zoomApplyCount=0;
    private final Runnable applyZoom=()->{zoomScheduled=false;applyQueuedZoom();};
    private final Runnable previewUpdate=()->{previewScheduled=false;updatePreview();};
    private void requestPreviewUpdate(){if(!previewScheduled){previewScheduled=true;handler.postDelayed(previewUpdate,16);}}
    private final Runnable configureTimeout=this::onConfigureTimeout;
    private void onConfigureTimeout(){if(active&&opening&&!configured)recoverOpening("A câmera demorou a responder.");}

    private static final class Job {
        String name,actualId,rawIssue="";int mode,rotation,pixelMode,width,height;long started,pressedAt;boolean wantsRaw,saving,physicalMetadata;
        byte[] jpeg;Image rawImage;long rawTimestamp,jpegTimestamp;CaptureResult result;TotalCaptureResult logicalResult;
        CameraCaps route;double zoom,optical,digital;JSONObject settings;EditRecipe recipe;
    }
    public CameraEngine(Context c,Listener l){context=c.getApplicationContext();manager=(CameraManager)context.getSystemService(Context.CAMERA_SERVICE);listener=l;writer=new MediaWriter(context);thread.start();handler=new Handler(thread.getLooper());executor=handler::post;}
    public void start(SurfaceTexture t){handler.post(()->{active=true;texture=t;listener.state("Consultando as câmeras…",true);probe();if(main!=null){if(caps==null)caps=main;open();}});}
    public void stop(){handler.post(()->{active=false;handler.removeCallbacks(applyZoom);zoomScheduled=false;pendingZoom=null;close();});}
    public void destroy(){handler.post(()->{active=false;close();io.execute(()->handler.post(()->{closeDeferred();thread.quitSafely();}));io.shutdown();});}
    public void selectMode(int next){handler.post(()->{
        if(job!=null){listener.modeChanged(mode);return;}
        if(next==HDR&&(caps==null||!caps.hdr||hdrFailures.contains(caps.id))){listener.modeChanged(mode);listener.features(false);listener.error("HDR não disponível nesta lente.");return;}
        CameraCaps route=choose(requestedZoom,next==RAW);
        if(route==null){listener.modeChanged(mode);listener.error("RAW não está liberado neste enquadramento. Tente 1× ou outro zoom.");return;}
        mode=next;listener.modeChanged(mode);resetControls();fallback=0;caps=route;open();
    });}
    public void highResolution(boolean value){handler.post(()->{if(job!=null)return;high=value;fallback=0;open();});}
    public void zoom(float value){handler.post(()->{if(!active)return;queuedZoom=Math.max(.1,Math.min(100,value));if(!zoomScheduled){zoomScheduled=true;handler.postDelayed(applyZoom,16);}});}
    private void applyQueuedZoom(){
        if(!active)return;if(job!=null||opening){pendingZoom=queuedZoom;return;}double value=queuedZoom;CameraCaps route=choose(value,mode==RAW);if(route==null){listener.error("Não há RAW acessível com esse campo de visão.");return;}
        boolean change=caps==null||!route.id.equals(caps.id)||!route.openId.equals(caps.openId);requestedZoom=value;caps=route;opticalRatio=optical(caps);metering=null;zoomApplyCount++;
        if(mode==HDR&&(!caps.hdr||hdrFailures.contains(caps.id))){mode=PHOTO;listener.modeChanged(mode);}
        if(change||!configured){fallback=0;open();}else{requestPreviewUpdate();listener.zoomApplied(value);}
    }
    public void processingOff(boolean value){handler.post(()->{processingOff=value;requestPreviewUpdate();});}
    public void exposureCompensation(int value){handler.post(()->{if(caps==null||job!=null)return;Range<Integer> r=caps.characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);ev=r==null?0:Math.max(r.getLower(),Math.min(r.getUpper(),value));userEv=true;sceneEv=0;requestPreviewUpdate();});}
    public void awbLock(boolean value){handler.post(()->{if(caps==null||job!=null)return;awbLocked=value&&caps.awbLock;requestPreviewUpdate();});}
    public void lock(boolean value){handler.post(()->{
        if(caps==null||job!=null||!configured)return;if(value&&!caps.aeLock&&caps.minFocus==0){listener.error("Trava não disponível nesta câmera.");listener.locked(false);return;}exposureLocked=value&&caps.aeLock;focusLocked=value;
        if(value&&lastResult!=null){Float focus=lastResult.get(CaptureResult.LENS_FOCUS_DISTANCE);if(focus!=null&&caps.minFocus>0)manualFocus=focus;}
        else manualFocus=null;
        listener.locked(value);requestPreviewUpdate();
    });}
    public void manual(int iso,long time,Float focus){handler.post(()->{
        if(caps==null||job!=null||mode==HDR)return;
        if(iso>0){if(!caps.manual||caps.iso==null||caps.exposure==null){listener.error("Exposição manual não está disponível nesta lente.");return;}
            manualIso=Math.max(caps.iso.getLower(),Math.min(caps.iso.getUpper(),iso));manualTime=Math.max(caps.exposure.getLower(),Math.min(caps.exposure.getUpper(),time));
        }else manualIso=0;
        manualFocus=focus;requestPreviewUpdate();
    });}
    public void aperture(double value){handler.post(()->aperture=(float)CaptureMath.clamp(value,1.4,16));}
    public void reset(){handler.post(()->{if(job!=null)return;resetControls();requestPreviewUpdate();});}
    public void intelligence(boolean value){handler.post(()->{intelligence=value;sceneEv=0;requestPreviewUpdate();});}
    public void observeScene(JSONObject observed){handler.post(()->{
        scene=observed;if(caps==null||!configured||job!=null)return;
        android.util.Rational step=caps.characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);
        int next=scenePolicy.update(scene,step==null?1:step.doubleValue(),userEv||metering!=null||exposureLocked||manualIso>0||FrameGeometry.localZoom(requestedZoom,opticalRatio)>1.04,intelligence);
        if(sceneEv!=next){sceneEv=next;requestPreviewUpdate();}
    });}
    private void resetControls(){scenePolicy.reset();manualIso=0;manualTime=0;manualFocus=null;metering=null;awbLocked=false;exposureLocked=false;focusLocked=false;ev=0;sceneEv=0;userEv=false;listener.locked(false);}
    private void probe(){
        if(!cameras.isEmpty())return;
        try{
            String[] ids=manager.getCameraIdList();Set<String> direct=new HashSet<>(Arrays.asList(ids));
            for(String id:ids)try{CameraCaps c=new CameraCaps(manager,id);if(!c.jpegSizes.isEmpty())cameras.add(c);}catch(Exception ignored){}
            ArrayList<CameraCaps> roots=new ArrayList<>(cameras);
            for(CameraCaps root:roots)if(!root.front)for(String id:root.characteristics.getPhysicalCameraIds())if(!direct.contains(id))try{CameraCaps c=new CameraCaps(manager,id,root.id,id);if(!c.jpegSizes.isEmpty()&&cameras.stream().noneMatch(x->x.id.equals(id)))cameras.add(c);}catch(Exception ignored){}
            cameras.sort((a,b)->Boolean.compare(a.front,b.front));
            if(cameras.isEmpty()){listener.error("Nenhuma câmera acessível.");return;}
            String preferred="";
            if(Build.MODEL.equalsIgnoreCase("PKJ110")||Build.MODEL.equalsIgnoreCase("PKU110"))preferred="2";
            for(CameraCaps c:cameras)if(!c.front&&c.id.equals(preferred))main=c;
            if(main==null){double best=Double.MAX_VALUE;for(CameraCaps c:cameras)if(!c.front){double score=c.equivalent35>0?Math.abs(c.equivalent35-24):100;if(c.physicalId!=null)score+=.2;if(score<best){best=score;main=c;}}}
            if(main==null)main=cameras.get(0);base35=main.equivalent35>0?main.equivalent35:24;caps=main;opticalRatio=1;
        }catch(Exception e){listener.error("Não foi possível consultar as câmeras: "+safe(e));}
    }
    private double optical(CameraCaps c){return c.equivalent35>0?c.equivalent35/base35:1;}
    private float minGlobal(){double min=1;for(CameraCaps c:cameras)if(!c.front)min=Math.min(min,optical(c));return(float)Math.max(.1,min);}
    private float maxGlobal(){double max=1;for(CameraCaps c:cameras)if(!c.front)max=Math.max(max,optical(c)*Math.max(1,c.maxZoom));return(float)Math.min(30,max);}
    private CameraCaps choose(double zoom,boolean needsRaw){
        ArrayList<LensRouter.Candidate> candidates=new ArrayList<>();
        for(CameraCaps c:cameras){if(c.front!=main.front)continue;double quality=0;if(!c.rawSizes.isEmpty())quality=CameraCaps.area(c.rawSizes.get(0))/1e6;
            double priority=quality+(c.characteristics.getPhysicalCameraIds().isEmpty()?5:0)+(c.physicalId!=null?2:0)+(c.id.equals(main.id)?3:0);
            candidates.add(new LensRouter.Candidate(c.id,optical(c),c.raw&&!c.rawSizes.isEmpty(),priority));
        }
        String chosen=LensRouter.choose(candidates,zoom,needsRaw);if(chosen!=null)for(CameraCaps c:cameras)if(c.id.equals(chosen))return c;return null;
    }
    private void releaseJob(Job j){if(j!=null&&j.rawImage!=null){try{j.rawImage.close();}catch(Exception ignored){}j.rawImage=null;}}
    private void close(){
        generation++;configured=false;opening=false;previewScheduled=false;handler.removeCallbacks(previewUpdate);handler.removeCallbacks(configureTimeout);
        if(job!=null&&!job.saving){releaseJob(job);job=null;listener.state("Captura interrompida.",false);}
        try{if(session!=null)session.close();}catch(Exception ignored){}session=null;
        try{if(extension!=null)extension.close();}catch(Exception ignored){}extension=null;
        if(device!=null){device.close();device=null;}
        if(jpegReader!=null){jpegReader.close();jpegReader=null;}
        if(rawReader!=null){if(job!=null&&job.saving&&job.rawImage!=null)deferredReaders.add(rawReader);else rawReader.close();rawReader=null;}
        if(previewSurface!=null){previewSurface.release();previewSurface=null;}lastResult=null;
    }
    private void open(){
        close();if(!active||texture==null||caps==null)return;opening=true;opticalRatio=optical(caps);
        listener.capabilities(Collections.unmodifiableList(new ArrayList<>(cameras)),caps,base35,minGlobal(),maxGlobal());listener.features(caps.hdr&&!hdrFailures.contains(caps.id));listener.state("Preparando a lente…",true);
        final int token=generation;handler.postDelayed(configureTimeout,10000);
        try{manager.openCamera(caps.openId,new CameraDevice.StateCallback(){
            public void onOpened(CameraDevice camera){if(token!=generation||!active){camera.close();return;}device=camera;configure(token);}
            public void onDisconnected(CameraDevice camera){camera.close();if(token==generation)recoverOpening("Lente desconectada.");}
            public void onError(CameraDevice camera,int code){camera.close();if(token==generation)recoverOpening("A câmera recusou a abertura ("+code+").");}
        },handler);}catch(Exception e){recoverOpening("Falha ao abrir a lente: "+safe(e));}
    }
    private void recoverOpening(String message){
        // Invalidate late Camera2 callbacks before restarting: a disconnected device cannot
        // consume an old zoom request or trigger an unlimited re-open loop.
        boolean previousAvailable=active&&lastReadyCaps!=null;
        boolean different=previousAvailable&&(caps==null||!caps.id.equals(lastReadyCaps.id)
                ||!caps.openId.equals(lastReadyCaps.openId)||mode!=lastReadyMode
                ||high!=lastReadyHigh||Math.abs(requestedZoom-lastReadyZoom)>.01);
        // Also permit one safe restart when an already-working lens disconnects unexpectedly.
        boolean restore=CaptureInteractionPolicy.restorePrevious(recoveringOpen,previousAvailable,different||configured);
        pendingZoom=null;handler.removeCallbacks(applyZoom);zoomScheduled=false;
        close();
        if(restore){
            recoveringOpen=true;caps=lastReadyCaps;requestedZoom=lastReadyZoom;queuedZoom=lastReadyZoom;
            mode=lastReadyMode;high=lastReadyHigh;fallback=0;
            listener.modeChanged(mode);listener.highRestored(high);listener.zoomRestored(lastReadyZoom);
            listener.state("Falha na lente; restaurando a última câmera funcional…",true);
            open();
        }else{
            recoveringOpen=false;listener.zoomRestored(lastReadyZoom);
            listener.error(message+" Selecione outra lente ou reabra a câmera.");
        }
    }
    private Size currentPreview;
    private long rawBudget(){ActivityManager.MemoryInfo m=new ActivityManager.MemoryInfo();((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(m);return Math.max(12_000_000L,Math.min(210_000_000L,m.availMem/10));}
    private void configure(int token){
        try{
            pixelMode=CameraMetadata.SENSOR_PIXEL_MODE_DEFAULT;rawSize=null;boolean ext=mode==HDR;
            List<Size> options=caps.jpegSizes;
            if(ext){options=new ArrayList<>(manager.getCameraExtensionCharacteristics(caps.id).getExtensionSupportedSizes(CameraExtensionCharacteristics.EXTENSION_HDR,ImageFormat.JPEG));options.sort((a,b)->Long.compare(CameraCaps.area(b),CameraCaps.area(a)));}
            if(mode==RAW){
                long budget=fallback<2?rawBudget():16_000_000;
                if(high&&fallback==0&&!caps.maxRawSizes.isEmpty()&&!caps.maxJpegSizes.isEmpty()){
                    Size candidate=CameraCaps.pick(caps.maxRawSizes,budget);if(candidate!=null&&CameraCaps.area(candidate)<=budget){pixelMode=CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION;rawSize=candidate;options=caps.maxJpegSizes;}
                }
                if(rawSize==null)rawSize=CameraCaps.pick(caps.rawSizes,budget);
                if(rawSize==null||CameraCaps.area(rawSize)>budget)throw new IOException("RAW excede a memória disponível. Desative Máxima ou use Foto.");
            }else if(!ext&&high&&fallback==0&&!caps.maxJpegSizes.isEmpty()&&CameraCaps.area(caps.maxJpegSizes.get(0))>CameraCaps.area(caps.jpegSizes.get(0))){pixelMode=CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION;options=caps.maxJpegSizes;}
            long jpegBudget=high&&(fallback==0||fallback==1&&mode!=RAW)?Long.MAX_VALUE:mode==RAW&&fallback==0&&rawSize!=null?CameraCaps.area(rawSize):16_000_000;
            captureSize=CameraCaps.pick(options,jpegBudget);if(captureSize==null)throw new IOException("Sem saída JPEG nessa configuração.");
            Size[] previews;
            if(ext)previews=manager.getCameraExtensionCharacteristics(caps.id).getExtensionSupportedSizes(CameraExtensionCharacteristics.EXTENSION_HDR,SurfaceTexture.class).toArray(new Size[0]);
            else {StreamConfigurationMap map=caps.characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);previews=map.getOutputSizes(SurfaceTexture.class);}
            currentPreview=CameraCaps.preview(previews,captureSize);
            if(fallback>0&&previews!=null){List<Size> list=new ArrayList<>(Arrays.asList(previews));list.sort((a,b)->Long.compare(CameraCaps.area(a),CameraCaps.area(b)));for(Size p:list)if(p.getWidth()>=640){currentPreview=p;break;}}
            texture.setDefaultBufferSize(currentPreview.getWidth(),currentPreview.getHeight());previewSurface=new Surface(texture);listener.preview(currentPreview,caps.sensorOrientation,caps.front,opticalRatio,requestedZoom);
            jpegReader=ImageReader.newInstance(captureSize.getWidth(),captureSize.getHeight(),ImageFormat.JPEG,2);jpegReader.setOnImageAvailableListener(r->receive(r,false),handler);
            List<OutputConfiguration> outputs=new ArrayList<>();outputs.add(output(previewSurface,false));outputs.add(output(jpegReader.getSurface(),true));
            if(rawSize!=null){rawReader=ImageReader.newInstance(rawSize.getWidth(),rawSize.getHeight(),ImageFormat.RAW_SENSOR,2);rawReader.setOnImageAvailableListener(r->receive(r,true),handler);outputs.add(output(rawReader.getSurface(),true));}
            if(ext)device.createExtensionSession(new ExtensionSessionConfiguration(CameraExtensionCharacteristics.EXTENSION_HDR,outputs,executor,new CameraExtensionSession.StateCallback(){
                public void onConfigured(CameraExtensionSession s){if(token!=generation){try{s.close();}catch(Exception ignored){}return;}extension=s;configured=true;if(updatePreview())sessionReady();else configurationFailed("A prévia HDR não iniciou.");}
                public void onConfigureFailed(CameraExtensionSession s){if(token==generation){hdrFailures.add(caps.id);listener.features(false);mode=PHOTO;open();}}
            }));
            else device.createCaptureSession(new SessionConfiguration(SessionConfiguration.SESSION_REGULAR,outputs,executor,new CameraCaptureSession.StateCallback(){
                public void onConfigured(CameraCaptureSession s){if(token!=generation){s.close();return;}session=s;configured=true;if(updatePreview())sessionReady();else configurationFailed("A prévia da lente não iniciou.");}
                public void onConfigureFailed(CameraCaptureSession s){if(token==generation)configurationFailed("Combinação de resolução e RAW recusada pela câmera.");}
            }));
        }catch(Exception|OutOfMemoryError e){configurationFailed(safe(e));}
    }
    private OutputConfiguration output(Surface surface,boolean still){OutputConfiguration o=new OutputConfiguration(surface);if(caps.physicalId!=null)o.setPhysicalCameraId(caps.physicalId);if(still&&pixelMode==CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION)o.addSensorPixelModeUsed(pixelMode);return o;}
    private void configurationFailed(String message){configured=false;opening=false;try{checks.add(new JSONObject().put("camera_id",caps.id).put("configured",false).put("error",message));}catch(Exception ignored){}
        if(active&&mode!=HDR&&fallback<2){fallback++;listener.state("Adaptando a combinação de captura…",true);open();}else recoverOpening(message+" Tente outro zoom ou desative Máxima.");
    }
    private void sessionReady(){
        opening=false;recoveringOpen=false;lastReadyCaps=caps;lastReadyZoom=requestedZoom;lastReadyMode=mode;lastReadyHigh=high;handler.removeCallbacks(configureTimeout);try{checks.add(new JSONObject().put("camera_id",caps.id).put("mode",modeName(mode)).put("jpeg_size",captureSize.toString()).put("raw_size",rawSize==null?JSONObject.NULL:rawSize.toString()).put("sensor_pixel_mode",pixelMode).put("configured",true).put("capture_verified",false));}catch(Exception ignored){}
        String sizes=String.format(Locale.getDefault(),"%.1f MP JPEG%s",CameraCaps.area(captureSize)/1e6,rawSize==null?"":String.format(Locale.getDefault()," · %.1f MP RAW",CameraCaps.area(rawSize)/1e6));
        listener.ready(sizes,mode==PORTRAIT);listener.state("Pronto · "+String.format(Locale.getDefault(),"%.1f mm",caps.focalMm)+(FrameGeometry.localZoom(requestedZoom,opticalRatio)>1.03?" · recorte digital":""),false);drainZoom();
    }
    private CaptureRequest.Builder request(int template)throws CameraAccessException{return caps.physicalId==null?device.createCaptureRequest(template):device.createCaptureRequest(template,Collections.singleton(caps.physicalId));}
    private Rect activeArray(CameraCharacteristics c,boolean maximum){Rect a=maximum?c.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION):null;return a!=null?a:c.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);}
    private MeteringRectangle[] mapRegions(MeteringRectangle[] regions,Rect from,Rect to){
        if(from==null||to==null||from.equals(to))return regions;MeteringRectangle[] out=new MeteringRectangle[regions.length];for(int i=0;i<regions.length;i++){Rect a=regions[i].getRect();int l=to.left+Math.round((a.left-from.left)*to.width()/(float)from.width()),t=to.top+Math.round((a.top-from.top)*to.height()/(float)from.height()),r=to.left+Math.round((a.right-from.left)*to.width()/(float)from.width()),b=to.top+Math.round((a.bottom-from.top)*to.height()/(float)from.height());Rect mapped=new Rect(l,t,Math.max(l+1,r),Math.max(t+1,b));mapped.intersect(to);out[i]=new MeteringRectangle(mapped,regions[i].getMeteringWeight());}return out;
    }
    @SuppressWarnings("unchecked") private <T> void set(CaptureRequest.Builder r,CaptureRequest.Key<T> key,T value){T global=value;
        if(caps.physicalId!=null&&value instanceof MeteringRectangle[]){boolean maximum=Integer.valueOf(CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION).equals(r.get(CaptureRequest.SENSOR_PIXEL_MODE));global=(T)mapRegions((MeteringRectangle[])value,activeArray(caps.characteristics,maximum),activeArray(caps.hostCharacteristics,maximum));}
        r.set(key,global);if(caps.physicalId!=null&&caps.physicalKeys.contains(key))r.setPhysicalCameraKey(key,value,caps.physicalId);
    }
    private void settings(CaptureRequest.Builder r,boolean still,int rotation){
        if(mode==HDR){if(Build.VERSION.SDK_INT>=33&&caps.hdrKeys.contains(CaptureRequest.JPEG_ORIENTATION)&&still)r.set(CaptureRequest.JPEG_ORIENTATION,rotation);return;}
        if(still&&pixelMode==CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION)set(r,CaptureRequest.SENSOR_PIXEL_MODE,pixelMode);
        set(r,CaptureRequest.CONTROL_MODE,CameraMetadata.CONTROL_MODE_AUTO);set(r,CaptureRequest.CONTROL_AWB_MODE,CameraMetadata.CONTROL_AWB_MODE_AUTO);
        if(caps.awbLock)set(r,CaptureRequest.CONTROL_AWB_LOCK,awbLocked);if(caps.aeLock)set(r,CaptureRequest.CONTROL_AE_LOCK,exposureLocked);
        int af=CameraMetadata.CONTROL_AF_MODE_OFF;int[] modes=caps.characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        if(manualFocus==null&&modes!=null){for(int v:modes)if(v==CameraMetadata.CONTROL_AF_MODE_AUTO)af=v;for(int v:modes)if(v==CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)af=v;}
        if(focusLocked&&manualFocus==null&&modes!=null)for(int v:modes)if(v==CameraMetadata.CONTROL_AF_MODE_AUTO)af=v;
        set(r,CaptureRequest.CONTROL_AF_MODE,af);if(manualFocus!=null)set(r,CaptureRequest.LENS_FOCUS_DISTANCE,(float)CaptureMath.clamp(manualFocus,0,caps.minFocus));
        set(r,CaptureRequest.CONTROL_AE_MODE,CameraMetadata.CONTROL_AE_MODE_ON);Range<Integer> compensation=caps.characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);int ec=compensation==null?0:Math.max(compensation.getLower(),Math.min(compensation.getUpper(),ev+sceneEv));set(r,CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,ec);
        set(r,CaptureRequest.FLASH_MODE,CameraMetadata.FLASH_MODE_OFF);set(r,CaptureRequest.CONTROL_EFFECT_MODE,CameraMetadata.CONTROL_EFFECT_MODE_OFF);
        // All stills are sensor framing at 1x on the selected physical optic. Digital crop is explicit and shared with DNG metadata.
        if(caps.characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)!=null&&caps.physicalId==null)r.set(CaptureRequest.CONTROL_ZOOM_RATIO,1f);
        int[] ois=caps.characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION);if(contains(ois,CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON))set(r,CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON);
        int[] edge=caps.characteristics.get(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES);if(processingOff&&contains(edge,CameraMetadata.EDGE_MODE_OFF))set(r,CaptureRequest.EDGE_MODE,CameraMetadata.EDGE_MODE_OFF);
        int[] noise=caps.characteristics.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES);if(processingOff&&contains(noise,CameraMetadata.NOISE_REDUCTION_MODE_OFF))set(r,CaptureRequest.NOISE_REDUCTION_MODE,CameraMetadata.NOISE_REDUCTION_MODE_OFF);
        int[] shading=caps.characteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_LENS_SHADING_MAP_MODES);if(contains(shading,CameraMetadata.STATISTICS_LENS_SHADING_MAP_MODE_ON))set(r,CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE,CameraMetadata.STATISTICS_LENS_SHADING_MAP_MODE_ON);
        if(metering!=null){MeteringRectangle[] selectedRegions=mapRegions(metering,activeArray(caps.characteristics,false),activeArray(caps.characteristics,still&&pixelMode==CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION));Integer max=caps.characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);if(max!=null&&max>0)set(r,CaptureRequest.CONTROL_AF_REGIONS,selectedRegions);max=caps.characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);if(max!=null&&max>0)set(r,CaptureRequest.CONTROL_AE_REGIONS,selectedRegions);}
        if(manualIso>0){set(r,CaptureRequest.CONTROL_AE_MODE,CameraMetadata.CONTROL_AE_MODE_OFF);set(r,CaptureRequest.SENSOR_SENSITIVITY,manualIso);set(r,CaptureRequest.SENSOR_EXPOSURE_TIME,manualTime);Long max=caps.characteristics.get(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION);if(max!=null)set(r,CaptureRequest.SENSOR_FRAME_DURATION,Math.min(max,Math.max(manualTime,33_333_333L)));}
        if(still){r.set(CaptureRequest.JPEG_QUALITY,(byte)100);r.set(CaptureRequest.JPEG_ORIENTATION,rotation);if(pixelMode==CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION)set(r,CaptureRequest.SENSOR_PIXEL_MODE,pixelMode);}
    }
    private static boolean contains(int[] a,int v){if(a!=null)for(int n:a)if(n==v)return true;return false;}
    private boolean updatePreview(){
        if(!configured||device==null)return false;final int token=generation;
        try{CaptureRequest.Builder r=request(CameraDevice.TEMPLATE_PREVIEW);r.addTarget(previewSurface);settings(r,false,0);
            if(extension!=null)extension.setRepeatingRequest(r.build(),executor,new CameraExtensionSession.ExtensionCaptureCallback(){});
            else session.setRepeatingRequest(r.build(),new CameraCaptureSession.CaptureCallback(){public void onCaptureCompleted(CameraCaptureSession s,CaptureRequest request,TotalCaptureResult result){if(token!=generation)return;lastResult=physical(result,caps);publishExposure(lastResult);}},handler);
            previewUpdateCount++;return true;
        }catch(Exception e){configured=false;listener.error("A câmera recusou o ajuste: "+safe(e));return false;}
    }
    private CaptureResult physical(TotalCaptureResult result,CameraCaps route){if(route.physicalId!=null){CaptureResult p=result.getPhysicalCameraResults().get(route.physicalId);if(p!=null)return p;}return result;}
    private void publishExposure(CaptureResult result){if(SystemClock.elapsedRealtime()-lastUiExposure<500)return;lastUiExposure=SystemClock.elapsedRealtime();Integer iso=result.get(CaptureResult.SENSOR_SENSITIVITY);Long time=result.get(CaptureResult.SENSOR_EXPOSURE_TIME);Float focal=result.get(CaptureResult.LENS_FOCAL_LENGTH);String s=(iso==null?"":"ISO "+iso)+(time==null?"":" · 1/"+Math.max(1,Math.round(1e9/time)))+(focal==null?"":String.format(Locale.getDefault()," · %.1f mm",focal));listener.exposure(s);Integer ec=result.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION);android.util.Rational step=caps.characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);if(ec!=null&&step!=null)listener.compensation(ec*step.floatValue(),sceneEv!=0&&!userEv);}
    public void focus(float x,float y){handler.post(()->{
        if(!configured||session==null||device==null||job!=null)return;focusX=x;focusY=y;exposureLocked=false;focusLocked=false;manualFocus=null;listener.locked(false);final int token=generation;
        try{Rect a=caps.characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);if(a==null)return;float u=caps.front?1-x:x,v=y;int ori=caps.sensorOrientation;
            if(ori==90){float t=u;u=v;v=1-t;}else if(ori==270){float t=u;u=1-v;v=t;}else if(ori==180){u=1-u;v=1-v;}
            double digital=FrameGeometry.localZoom(requestedZoom,opticalRatio);int w=(int)(a.width()/digital),h=(int)(a.height()/digital),left=a.centerX()-w/2,top=a.centerY()-h/2;
            int rw=Math.max(32,w/10),rh=Math.max(32,h/10),px=(int)CaptureMath.clamp(left+u*w-rw/2,a.left,a.right-rw),py=(int)CaptureMath.clamp(top+v*h-rh/2,a.top,a.bottom-rh);
            metering=new MeteringRectangle[]{new MeteringRectangle(new Rect(px,py,px+rw,py+rh),MeteringRectangle.METERING_WEIGHT_MAX)};sceneEv=0;
            CaptureRequest.Builder r=request(CameraDevice.TEMPLATE_PREVIEW);r.addTarget(previewSurface);settings(r,false,0);if(r.get(CaptureRequest.CONTROL_AF_MODE)!=CameraMetadata.CONTROL_AF_MODE_OFF)set(r,CaptureRequest.CONTROL_AF_TRIGGER,CameraMetadata.CONTROL_AF_TRIGGER_START);session.capture(r.build(),null,handler);requestPreviewUpdate();
        }catch(Exception e){listener.error("Não foi possível ajustar foco/exposição nessa lente.");}
    });}
    public void focusAndLock(float x,float y){focus(x,y);handler.post(()->awaitFocusLock(generation,0));}
    private void awaitFocusLock(int token,int attempt){if(token!=generation||!configured)return;Integer af=lastResult==null?null:lastResult.get(CaptureResult.CONTROL_AF_STATE);if(attempt>=8||Integer.valueOf(CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED).equals(af)||Integer.valueOf(CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED).equals(af)){lock(true);return;}handler.postDelayed(()->awaitFocusLock(token,attempt+1),100);}
    public void capture(int orientation){long pressedAt=SystemClock.elapsedRealtime();handler.post(()->captureInternal(orientation,0,pressedAt));}
    private void captureInternal(int orientation,int attempt,long pressedAt){
        if(job!=null||!configured||device==null){listener.state("Aguarde a preparação da lente.",job!=null||!configured);return;}
        Integer awb=lastResult==null?null:lastResult.get(CaptureResult.CONTROL_AWB_STATE);
        if(mode!=HDR&&CaptureInteractionPolicy.waitForAwb(attempt,manualIso==0,
                lastResult==null||Integer.valueOf(CaptureResult.CONTROL_AWB_STATE_SEARCHING).equals(awb))){
            listener.state("Ajustando cor para o disparo…",true);int token=generation;
            handler.postDelayed(()->{if(token==generation)captureInternal(orientation,attempt+1,pressedAt);},CaptureInteractionPolicy.MAX_AWB_GRACE_MS);return;
        }
        Job j=new Job();j.started=SystemClock.elapsedRealtime();j.pressedAt=pressedAt;long shutterQueueMs=Math.max(0,j.started-pressedAt);j.name="NATURAL_"+new SimpleDateFormat("yyyyMMdd_HHmmss_SSS",Locale.US).format(new Date())+"_"+String.format(Locale.US,"%.1fx",requestedZoom).replace('.','p');j.mode=mode;j.wantsRaw=mode==RAW;j.pixelMode=pixelMode;j.route=caps;j.zoom=requestedZoom;j.optical=opticalRatio;j.digital=FrameGeometry.localZoom(j.zoom,j.optical);j.rotation=CaptureMath.jpegOrientation(caps.sensorOrientation,orientation,caps.front);
        j.recipe=new EditRecipe();j.recipe.cropZoom=(float)j.digital;j.recipe.portrait=mode==PORTRAIT;j.recipe.aperture=aperture;j.recipe.focusX=focusX;j.recipe.focusY=focusY;
        try{j.settings=new JSONObject().put("shutter_request_to_job_ms",shutterQueueMs).put("awb_grace_limit_ms",CaptureInteractionPolicy.MAX_AWB_GRACE_MS).put("app_version","0.2.2-teste.1").put("camera_id",caps.id).put("open_id",caps.openId).put("pinned_physical_id",caps.physicalId==null?JSONObject.NULL:caps.physicalId).put("lens_label",caps.label).put("mode",modeName(mode)).put("zoom_requested",j.zoom).put("optical_ratio_calculated",j.optical).put("digital_crop_ratio",j.digital).put("equivalent_35mm_native_calculated",caps.equivalent35).put("equivalent_35mm_effective_calculated",caps.equivalent35*j.digital).put("jpeg_sensor_requested",captureSize.toString()).put("raw_requested",rawSize==null?JSONObject.NULL:rawSize.toString()).put("sensor_pixel_mode_requested",pixelMode).put("fixed_temperature_applied",false).put("raw_pixels_filtered",false).put("edge_mode_OFF_requested",processingOff).put("noise_reduction_OFF_requested",processingOff).put("jpeg_processing_policy",processingOff?"user_explicit_OFF":"camera_template_defaults").put("manual_exposure_keeps_camera_AWB",true).put("quality_validation","pending_physical_OPPO");
            JSONObject intelligenceReport=new JSONObject().put("collector",scene==null?JSONObject.NULL:scene).put("trained_scene_model",false).put("action",new JSONObject().put("enabled",intelligence).put("user_ev_steps",ev).put("highlight_protection_steps",sceneEv).put("auto_shadow_lift",false).put("user_priority",userEv||metering!=null||exposureLocked||manualIso>0));j.settings.put("intelligence",intelligenceReport);
        }catch(Exception e){listener.error(safe(e));return;}
        job=j;listener.state("Capturando…",true);
        try{CaptureRequest.Builder r=request(CameraDevice.TEMPLATE_STILL_CAPTURE);r.addTarget(jpegReader.getSurface());if(j.wantsRaw)r.addTarget(rawReader.getSurface());settings(r,true,j.rotation);r.setTag(j);
            if(extension!=null)extension.capture(r.build(),executor,new CameraExtensionSession.ExtensionCaptureCallback(){
                public void onCaptureResultAvailable(CameraExtensionSession s,CaptureRequest r,TotalCaptureResult result){result(j,result);}
                public void onCaptureFailed(CameraExtensionSession s,CaptureRequest r){fail(j,"HDR não concluído.");}
            });
            else session.capture(r.build(),new CameraCaptureSession.CaptureCallback(){
                public void onCaptureCompleted(CameraCaptureSession s,CaptureRequest r,TotalCaptureResult result){result(j,result);}
                public void onCaptureFailed(CameraCaptureSession s,CaptureRequest r,CaptureFailure failure){fail(j,"Falha no disparo ("+failure.getReason()+").");}
                public void onCaptureSequenceAborted(CameraCaptureSession s,int id){fail(j,"Disparo interrompido pelo sistema.");}
            },handler);
            j.settings.put("shutter_request_to_capture_submit_ms",Math.max(0,SystemClock.elapsedRealtime()-pressedAt));
            handler.postDelayed(()->{if(job==j&&!j.saving)fail(j,"Tempo de captura excedido. Tente desativar Máxima.");},45000);
        }catch(Exception|OutOfMemoryError e){fail(j,safe(e));}
    }
    private void result(Job j,TotalCaptureResult result){if(job!=j)return;j.logicalResult=result;j.result=physical(result,j.route);j.physicalMetadata=j.route.physicalId==null||result.getPhysicalCameraResults().containsKey(j.route.physicalId);j.actualId=j.route.physicalId!=null?(j.physicalMetadata?j.route.physicalId:null):j.route.characteristics.getPhysicalCameraIds().isEmpty()?j.route.id:result.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID);finishIfReady(j);}
    private void receive(ImageReader reader,boolean raw){Image image=null;try{
        image=reader.acquireNextImage();if(image==null||job==null||job.saving)return;Job j=job;
        if(raw){j.rawImage=image;j.width=image.getWidth();j.height=image.getHeight();j.rawTimestamp=image.getTimestamp();image=null;}
        else {ByteBuffer b=image.getPlanes()[0].getBuffer();j.jpeg=new byte[b.remaining()];b.get(j.jpeg);j.jpegTimestamp=image.getTimestamp();}
        finishIfReady(j);
    }catch(Exception|OutOfMemoryError e){if(job!=null)fail(job,"Falha no recebimento: "+safe(e));}finally{if(image!=null)image.close();}}
    private void finishIfReady(Job j){if(job!=j||j.saving||j.jpeg==null)return;if(j.wantsRaw&&(j.rawImage==null||j.result==null))return;if(j.mode!=HDR&&j.result==null)return;
        if(j.wantsRaw){Long time=j.result.get(CaptureResult.SENSOR_TIMESTAMP);
            if(time==null||time!=j.rawTimestamp||time!=j.jpegTimestamp){j.rawIssue="JPEG preservado; RAW recusado por divergência de timestamp.";j.wantsRaw=false;releaseJob(j);}
            else if(!j.physicalMetadata){j.rawIssue="JPEG preservado; RAW recusado por falta dos metadados da lente física.";j.wantsRaw=false;releaseJob(j);}
        }
        j.saving=true;listener.state("Gravando os originais…",true);io.execute(()->save(j));
    }
    private void save(Job j){
        File sensor=null,rendered=null,dng=null;Uri finalUri=null,originalUri=null;boolean rawSaved=false,depthApplied=false;RawDngCrop.Crop rawCrop=null;String warning=j.rawIssue;
        try{
            sensor=File.createTempFile("capture_sensor_",".jpg",context.getCacheDir());try(OutputStream out=new FileOutputStream(sensor)){out.write(j.jpeg);}j.settings.put("actual_camera_metadata",resultJson(j.result)).put("actual_camera_id",j.actualId==null?JSONObject.NULL:j.actualId);JSONObject sensorMetadata=new JSONObject(j.settings.toString()).put("digital_crop_ratio",1).put("equivalent_35mm_effective_calculated",j.route.equivalent35);try{ExifMetadata.capture(sensor,sensorMetadata);}catch(Exception e){warning+=(warning.isEmpty()?"":" ")+"Metadados JPEG não foram atualizados.";}
            File originalFile=sensor;originalUri=writer.image(j.name+(j.digital>1.001||j.mode==PORTRAIT?"_sensor":"")+".jpg","image/jpeg",out->{try(InputStream in=new FileInputStream(originalFile)){PhotoSource.copy(in,out);}});
            if(j.wantsRaw){
                try{dng=File.createTempFile("capture_raw_",".dng",context.getCacheDir());try(OutputStream out=new FileOutputStream(dng);DngCreator creator=new DngCreator(j.route.characteristics,j.result)){creator.setOrientation(CaptureMath.exifOrientation(j.rotation));creator.writeImage(out,j.rawImage);}rawCrop=RawDngCrop.apply(dng,j.digital);File rawFile=dng;writer.image(j.name+".dng","image/x-adobe-dng",out->{try(InputStream in=new FileInputStream(rawFile)){PhotoSource.copy(in,out);}});rawSaved=true;
                }catch(Exception|OutOfMemoryError e){warning="JPEG preservado; RAW não publicado: "+safe(e);}
                finally{releaseJob(j);}
            }
            finalUri=originalUri;
            if(j.digital>1.001||j.mode==PORTRAIT){
                rendered=File.createTempFile("capture_render_",".jpg",context.getCacheDir());listener.state(j.mode==PORTRAIT?"Estimando profundidade local…":"Aplicando o mesmo enquadramento…",true);
                try{ImageExporter.Result result=ImageExporter.export(context,sensor,rendered,j.recipe,null);depthApplied=result.depthApplied;ExifMetadata.capture(rendered,j.settings);File finalFile=rendered;finalUri=writer.image(j.name+".jpg","image/jpeg",out->{try(InputStream in=new FileInputStream(finalFile)){PhotoSource.copy(in,out);}});}
                catch(Exception|OutOfMemoryError|LinkageError e){warning+=(warning.isEmpty()?"":" ")+"Original salvo; processamento não concluído: "+safe(e);j.recipe.portrait=false;}
            }
            int outWidth,outHeight;try(PhotoSource actual=new PhotoSource(finalUri.equals(originalUri)?sensor:rendered)){outWidth=actual.width;outHeight=actual.height;}
            JSONObject audit=SceneIntelligence.audit(j.route.id,j.actualId,j.zoom,j.optical,rawSaved,j.wantsRaw&&rawSaved,outWidth,outHeight,rawCrop);j.settings.getJSONObject("intelligence").put("audit",audit);
            j.settings.put("shutter_press_to_save_ms",Math.max(0,SystemClock.elapsedRealtime()-j.pressedAt)).put("capture_and_processing_ms",SystemClock.elapsedRealtime()-j.started).put("jpeg_saved",true).put("raw_saved",rawSaved).put("depth_applied",depthApplied).put("virtual_f_number",j.recipe.aperture).put("native_200mp_verified",false).put("warning",warning).put("jpeg_width",outWidth).put("jpeg_height",outHeight).put("source_preserved",true).put("raw_crop_metadata_only",rawSaved&&j.digital>1.001).put("raw_jpeg_framing_metadata_consistent",rawSaved&&(j.digital<=1.001||!finalUri.equals(originalUri)));
            if(rawCrop!=null)j.settings.put("raw_default_crop",new JSONArray(new int[]{rawCrop.x,rawCrop.y,rawCrop.width,rawCrop.height}));
            try{JSONObject report=j.settings;writer.document(j.name+"_captura.json","application/json",out->out.write(report.toString(2).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){warning+=(warning.isEmpty()?"":" ")+"Relatório não pôde ser gravado.";}
            Uri photo=finalUri,original=originalUri;String finalWarning=warning;boolean rawOk=rawSaved;String detail=String.format(Locale.getDefault(),"%d × %d · %.1f MP%s",outWidth,outHeight,outWidth*(double)outHeight/1e6,rawSaved?" + DNG":"");
            handler.post(()->{
                lastPalettePhoto=photo;lastJpeg=null;lastBasename=j.name;if(job==j)job=null;closeDeferred();try{checks.add(new JSONObject().put("camera_id",j.route.id).put("capture_verified",true).put("raw_saved",rawOk).put("audit",audit));}catch(Exception ignored){}
                listener.saved(photo,original,j.recipe,j.name,detail);listener.state(finalWarning.isEmpty()?"Salvo · "+detail:finalWarning,false);drainZoom();
            });
        }catch(Exception|OutOfMemoryError|LinkageError e){Uri original=originalUri;handler.post(()->{if(job==j)job=null;closeDeferred();if(original!=null)listener.saved(original,original,j.recipe,j.name,"Original salvo");listener.error("Falha na gravação: "+safe(e));drainZoom();});}
        finally{releaseJob(j);if(sensor!=null)sensor.delete();if(rendered!=null)rendered.delete();if(dng!=null)dng.delete();}
    }
    private void closeDeferred(){for(ImageReader r:deferredReaders)try{r.close();}catch(Exception ignored){}deferredReaders.clear();}
    private void drainZoom(){if(pendingZoom!=null&&active){queuedZoom=pendingZoom;pendingZoom=null;applyQueuedZoom();}}
    private void fail(Job j,String message){if(job!=j||j.saving)return;releaseJob(j);job=null;listener.error(message);drainZoom();}
    private JSONObject resultJson(CaptureResult r)throws JSONException{
        JSONObject j=new JSONObject();if(r==null)return j.put("available",false);j.put("available",true);put(j,"iso",r.get(CaptureResult.SENSOR_SENSITIVITY));put(j,"exposure_ns",r.get(CaptureResult.SENSOR_EXPOSURE_TIME));put(j,"focus_distance_diopters",r.get(CaptureResult.LENS_FOCUS_DISTANCE));put(j,"optical_aperture",r.get(CaptureResult.LENS_APERTURE));put(j,"focal_length_mm",r.get(CaptureResult.LENS_FOCAL_LENGTH));put(j,"awb_state",r.get(CaptureResult.CONTROL_AWB_STATE));put(j,"awb_mode",r.get(CaptureResult.CONTROL_AWB_MODE));put(j,"colour_correction_mode",r.get(CaptureResult.COLOR_CORRECTION_MODE));put(j,"colour_transform",r.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)==null?null:r.get(CaptureResult.COLOR_CORRECTION_TRANSFORM).toString());put(j,"neutral_colour_point",r.get(CaptureResult.SENSOR_NEUTRAL_COLOR_POINT)==null?null:Arrays.toString(r.get(CaptureResult.SENSOR_NEUTRAL_COLOR_POINT)));put(j,"dynamic_black_level",r.get(CaptureResult.SENSOR_DYNAMIC_BLACK_LEVEL)==null?null:Arrays.toString(r.get(CaptureResult.SENSOR_DYNAMIC_BLACK_LEVEL)));put(j,"sensor_pixel_mode",r.get(CaptureResult.SENSOR_PIXEL_MODE));put(j,"active_physical_id_reported",r.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID));put(j,"exposure_compensation_steps_observed",r.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION));put(j,"ois_mode_observed",r.get(CaptureResult.LENS_OPTICAL_STABILIZATION_MODE));put(j,"edge_mode_observed",r.get(CaptureResult.EDGE_MODE));put(j,"noise_reduction_observed",r.get(CaptureResult.NOISE_REDUCTION_MODE));put(j,"crop_region_observed",r.get(CaptureResult.SCALER_CROP_REGION)==null?null:r.get(CaptureResult.SCALER_CROP_REGION).toString());RggbChannelVector g=r.get(CaptureResult.COLOR_CORRECTION_GAINS);if(g!=null)j.put("awb_gains",new JSONArray(new float[]{g.getRed(),g.getGreenEven(),g.getGreenOdd(),g.getBlue()}));return j;
    }
    private static void put(JSONObject j,String key,Object v)throws JSONException{j.put(key,v==null?JSONObject.NULL:v);}
    public void exportDiagnostic(){handler.post(()->{try{
        JSONObject root=new JSONObject().put("app_name","Natural Cam").put("app_version","0.2.2-teste.1").put("model",Build.MODEL).put("manufacturer",Build.MANUFACTURER).put("android",Build.VERSION.RELEASE).put("sdk",Build.VERSION.SDK_INT).put("base_equivalent35_calculated",base35).put("created_at",new Date().toString()).put("privacy","No GPS, IMEI, serial number, photos or automatic transmission.");JSONArray list=new JSONArray();for(CameraCaps c:cameras)list.put(c.json());root.put("interaction_counters",new JSONObject().put("preview_requests_applied",previewUpdateCount).put("zoom_intents_applied",zoomApplyCount).put("physical_fluency_verified",false)).put("quality_validation","pending_real_OPPO_files").put("cameras",list).put("manufacturer_integration",ManufacturerReference.diagnostic(context)).put("checks",new JSONArray(checks)).put("unimplemented",new JSONArray(Arrays.asList("own_RAW_burst_fusion","three_camera_fusion","calibrated_optical_depth_f_number","professional_RAW_development","trained_OPPO_scene_model")));
        io.execute(()->{try{Uri uri=writer.document("Natural_Cam_Diagnostico_"+System.currentTimeMillis()+".json","application/json",out->out.write(root.toString(2).getBytes(StandardCharsets.UTF_8)));listener.diagnostic(uri);}catch(Exception e){listener.error(safe(e));}});
    }catch(Exception e){listener.error(safe(e));}});}
    public void exportPalette(){handler.post(()->{if(lastPalettePhoto==null){listener.error("Fotografe antes de extrair a paleta.");return;}Uri photo=lastPalettePhoto;String name=lastBasename;io.execute(()->{try{listener.palette(PaletteExporter.export(writer,context.getContentResolver(),photo,name));}catch(Exception e){listener.error(safe(e));}});});}
    static String modeName(int m){return m==RAW?"RAW_JPEG_PHYSICAL_OR_EXPLICIT_CROP":m==HDR?"OEM_HDR":m==PORTRAIT?"LOCAL_DEPTH_PORTRAIT":"PHOTO_NATURAL";}
    static String safe(Throwable e){String message=e.getMessage();return message==null?e.getClass().getSimpleName():message;}
}
