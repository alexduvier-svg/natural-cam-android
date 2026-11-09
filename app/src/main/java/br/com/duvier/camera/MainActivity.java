package br.com.duvier.camera;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.hardware.camera2.CameraCharacteristics;
import android.net.Uri;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity implements CameraEngine.Listener {
    private CameraEngine engine;private TextureView texture;private FrameLayout root,stage;private Overlay overlay;
    private TextView status,details,exposure,hdr,depth,evLabel;private Ui.Icon lock,quality,info;
    private LinearLayout zooms,modes;private Shutter shutter;private CameraCaps current;private List<CameraCaps> all=Collections.emptyList();
    private boolean busy=true,started=false,destroyed=false,high=false,processingOff=false,grid=true,peaking=false,locked=false,intelligent=false,hdrAvailable=false,sampling=false;
    private long lastEvGesture=0;private int lastSentEv=Integer.MIN_VALUE;private float builtZoomMin=Float.NaN,builtZoomMax=Float.NaN;
    private int mode=CameraEngine.PHOTO,orientation=0,ev=0,manualIso=0;private long manualTime=10_000_000;
    private double base35=24,optical=1,pendingOptical=1,displayZoom=1,targetZoom=1,pendingFrameZoom=1;private boolean awaitingFrame=false,frameReady=false,animateZoomOnApply=true;private float pendingRatio=.75f;private float minimum=.6f,maximum=8,aperture=5.6f,ratio=.75f;
    private Uri lastPhoto,lastOriginal;private EditRecipe lastRecipe=new EditRecipe();private OrientationEventListener orientationListener;
    private final Handler uiHandler=new Handler(Looper.getMainLooper());private final ExecutorService monitor=Executors.newSingleThreadExecutor();private ValueAnimator zoomAnimation;
    private final Runnable sampler=new Runnable(){public void run(){sample();if(!destroyed&&started)uiHandler.postDelayed(this,peaking?250:900);}};
    private int dp(float n){return Ui.dp(this,n);}
    public void onCreate(Bundle b){super.onCreate(b);getWindow().setDecorFitsSystemWindows(false);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        engine=new CameraEngine(this,this);build();root.post(()->{WindowInsetsController c=getWindow().getInsetsController();if(c!=null){c.hide(WindowInsets.Type.systemBars());c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}});restoreLast();orientationListener=new OrientationEventListener(this){public void onOrientationChanged(int angle){if(angle!=ORIENTATION_UNKNOWN)orientation=((angle+45)/90*90)%360;}};
    }
    private void build(){
        root=new FrameLayout(this);root.setBackgroundColor(Ui.BG);setContentView(root);
        stage=new FrameLayout(this);texture=new TextureView(this);overlay=new Overlay(this);stage.addView(texture,new FrameLayout.LayoutParams(-1,-1));stage.addView(overlay,new FrameLayout.LayoutParams(-1,-1));root.addView(stage,new FrameLayout.LayoutParams(-1,-1));
        root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets a=insets.getInsets(WindowInsets.Type.displayCutout()|WindowInsets.Type.systemBars());root.setPadding(a.left,a.top,a.right,a.bottom);root.post(this::layoutStage);return insets;});root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->layoutStage());
        LinearLayout top=Ui.row(this);top.setPadding(dp(12),0,dp(8),0);top.setBackgroundColor(0x8808090b);
        evLabel=Ui.action(this,"EV 0,0");evLabel.setTextColor(Ui.ACCENT);evLabel.setSingleLine(true);evLabel.setTextSize(12);evLabel.setPadding(dp(6),0,dp(6),0);evLabel.setContentDescription("Compensação de exposição");evLabel.setOnClickListener(v->showExposure());top.addView(evLabel,new LinearLayout.LayoutParams(dp(84),dp(48)));
        lock=new Ui.Icon(this,"lock","Travar ou liberar exposição e foco");lock.setOnClickListener(v->engine.lock(!locked));top.addView(lock,new LinearLayout.LayoutParams(dp(48),dp(48)));
        hdr=Ui.action(this,"HDR");hdr.setVisibility(View.GONE);hdr.setOnClickListener(v->{if(!hdrAvailable)return;mode=mode==CameraEngine.HDR?CameraEngine.PHOTO:CameraEngine.HDR;engine.selectMode(mode);refreshModes();});
        View spacer=new View(this);top.addView(spacer,new LinearLayout.LayoutParams(0,1,1));top.addView(hdr,new LinearLayout.LayoutParams(dp(52),dp(48)));info=new Ui.Icon(this,"info","Informações e ajustes");info.setOnClickListener(v->showInfo());top.addView(info,new LinearLayout.LayoutParams(dp(48),dp(48)));FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-1,dp(52),Gravity.TOP);root.addView(top,tp);
        exposure=Ui.text(this,"",10,Ui.MUTED);exposure.setPadding(dp(16),0,dp(16),0);FrameLayout.LayoutParams ep=new FrameLayout.LayoutParams(-1,dp(20),Gravity.TOP);ep.topMargin=dp(52);root.addView(exposure,ep);
        zooms=Ui.row(this);zooms.setGravity(Gravity.CENTER);HorizontalScrollView zoomScroll=Ui.horizontal(this,zooms);zoomScroll.setFillViewport(true);zoomScroll.setBackgroundColor(Ui.BG);FrameLayout.LayoutParams zp=new FrameLayout.LayoutParams(-1,dp(48),Gravity.BOTTOM);zp.bottomMargin=dp(184);root.addView(zoomScroll,zp);
        depth=Ui.action(this,"f/5,6 · virtual");depth.setTextSize(12);depth.setBackground(Ui.rounded(this,0x9908090b,18));depth.setOnClickListener(v->showDepth());depth.setVisibility(View.GONE);FrameLayout.LayoutParams df=new FrameLayout.LayoutParams(-2,dp(40),Gravity.BOTTOM|Gravity.RIGHT);df.bottomMargin=dp(240);df.rightMargin=dp(12);root.addView(depth,df);
        LinearLayout bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.VERTICAL);bottom.setBackgroundColor(Ui.BG);bottom.setPadding(dp(14),0,dp(14),dp(4));
        LinearLayout shoot=Ui.row(this);shoot.setGravity(Gravity.CENTER);quality=new Ui.Icon(this,"quality","Qualidade máxima disponível");quality.label="Máxima";quality.setOnClickListener(v->{if(busy)return;high=!high;quality.selected(high);engine.highResolution(high);});shoot.addView(quality,new LinearLayout.LayoutParams(0,dp(72),1));
        shutter=new Shutter(this);shutter.setContentDescription("Fotografar");shutter.setOnClickListener(v->{if(!busy)engine.capture(orientation);});LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(82),dp(82));sp.setMargins(dp(25),dp(4),dp(25),dp(4));shoot.addView(shutter,sp);
        Ui.Icon edit=new Ui.Icon(this,"edit","Abrir edição rápida");edit.label="Edição";edit.setOnClickListener(v->edit());shoot.addView(edit,new LinearLayout.LayoutParams(0,dp(72),1));bottom.addView(shoot);
        modes=Ui.row(this);modes.setGravity(Gravity.CENTER);String[] names={"Foto","RAW","Retrato","Pro"};for(int i=0;i<4;i++){final int index=i;TextView t=Ui.action(this,names[i]);t.setPadding(dp(8),0,dp(8),0);t.setOnClickListener(v->{if(index==3){showPro();return;}if(busy)return;mode=index==0?CameraEngine.PHOTO:index==1?CameraEngine.RAW:CameraEngine.PORTRAIT;ev=0;manualIso=0;engine.selectMode(mode);refreshModes();});modes.addView(t,new LinearLayout.LayoutParams(0,dp(44),1));}bottom.addView(modes);
        details=Ui.text(this,"JPEG · Natural",11,Ui.MUTED);details.setGravity(Gravity.CENTER);bottom.addView(details,new LinearLayout.LayoutParams(-1,dp(18)));
        status=Ui.text(this,"Permita o acesso à câmera.",11,Ui.MUTED);status.setGravity(Gravity.CENTER);status.setMaxLines(2);status.setPadding(dp(8),dp(2),dp(8),dp(2));bottom.addView(status,new LinearLayout.LayoutParams(-1,dp(32)));status.setOnClickListener(v->{if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.CAMERA},7);});
        root.addView(bottom,new FrameLayout.LayoutParams(-1,dp(184),Gravity.BOTTOM));
        texture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener(){public void onSurfaceTextureAvailable(SurfaceTexture s,int w,int h){start();}public void onSurfaceTextureSizeChanged(SurfaceTexture s,int w,int h){transform();}public boolean onSurfaceTextureDestroyed(SurfaceTexture s){started=false;engine.stop();return true;}public void onSurfaceTextureUpdated(SurfaceTexture s){if(awaitingFrame&&frameReady){awaitingFrame=false;frameReady=false;optical=pendingOptical;ratio=pendingRatio;displayZoom=pendingFrameZoom;layoutStage();transform();}}});
        refreshModes();
    }
    private void layoutStage(){if(stage==null||root.getWidth()==0)return;int w=root.getWidth()-root.getPaddingLeft()-root.getPaddingRight(),available=root.getHeight()-root.getPaddingTop()-root.getPaddingBottom()-dp(284);int h=Math.min(available,Math.round(w/ratio));FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)stage.getLayoutParams();if(p.height!=h||p.topMargin!=dp(52)+(available-h)/2){p.width=-1;p.height=Math.max(dp(180),h);p.topMargin=dp(52)+Math.max(0,(available-h)/2);p.gravity=Gravity.TOP;stage.setLayoutParams(p);}}
    private void transform(){if(texture==null)return;Matrix m=new Matrix();float scale=(float)Math.max(1,displayZoom/Math.max(.01,optical));m.postScale(current!=null&&current.front?-scale:scale,scale,texture.getWidth()/2f,texture.getHeight()/2f);texture.setTransform(m);}
    private void refreshModes(){for(int i=0;i<3;i++){TextView t=(TextView)modes.getChildAt(i);boolean selected=i==0&&(mode==0||mode==2)||i==1&&mode==1||i==2&&mode==3;t.setTextColor(selected?Ui.ACCENT:Ui.TEXT);t.setTypeface(null,selected?Typeface.BOLD:Typeface.NORMAL);}depth.setVisibility(mode==CameraEngine.PORTRAIT?View.VISIBLE:View.GONE);hdr.setTextColor(mode==CameraEngine.HDR?Ui.ACCENT:Ui.TEXT);hdr.setVisibility(hdrAvailable&&(mode==CameraEngine.PHOTO||mode==CameraEngine.HDR)?View.VISIBLE:View.GONE);}
    private void start(){if(destroyed||started||!texture.isAvailable())return;if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},7);return;}started=true;busy=true;status.setText("Consultando as câmeras…");shutter.invalidate();engine.start(texture.getSurfaceTexture());uiHandler.removeCallbacks(sampler);uiHandler.postDelayed(sampler,700);}
    protected void onResume(){super.onResume();orientationListener.enable();if(texture.isAvailable())start();}
    protected void onPause(){started=false;engine.stop();orientationListener.disable();uiHandler.removeCallbacks(sampler);super.onPause();}
    protected void onDestroy(){destroyed=true;if(zoomAnimation!=null)zoomAnimation.cancel();engine.destroy();monitor.shutdownNow();uiHandler.removeCallbacksAndMessages(null);super.onDestroy();}
    public void onRequestPermissionsResult(int n,String[] p,int[] r){super.onRequestPermissionsResult(n,p,r);if(n==7&&r.length>0&&r[0]==PackageManager.PERMISSION_GRANTED)start();else{busy=false;status.setText("Toque aqui para permitir a câmera.");shutter.invalidate();}}
    public boolean onKeyDown(int key,KeyEvent e){if(key==KeyEvent.KEYCODE_VOLUME_UP||key==KeyEvent.KEYCODE_VOLUME_DOWN){if(e.getRepeatCount()==0&&!busy)engine.capture(orientation);return true;}return super.onKeyDown(key,e);}
    private void ui(Runnable action){runOnUiThread(()->{if(!destroyed)action.run();});}
    public void capabilities(List<CameraCaps> list,CameraCaps c,double base,float min,float max){ui(()->{
        if(current==null||!current.id.equals(c.id)){lastSentEv=Integer.MIN_VALUE;if(zoomAnimation!=null)zoomAnimation.cancel();}
        all=list;current=c;base35=base;minimum=min;maximum=max;rebuildZoom();
    });}
    public void preview(Size s,int sensor,boolean front,double optical,double requested){ui(()->{
        pendingOptical=optical;pendingRatio=s.getHeight()/(float)s.getWidth();pendingFrameZoom=requested;
        awaitingFrame=true;frameReady=false;
    });}
    public void zoomApplied(double applied){ui(()->{
        // Camera2 accepted this zoom on the existing optic; now animate the displayed crop.
        if(Math.abs(applied-targetZoom)>.05)return;
        if(awaitingFrame){pendingFrameZoom=applied;return;}
        if(zoomAnimation!=null)zoomAnimation.cancel();
        if(animateZoomOnApply){zoomAnimation=ValueAnimator.ofFloat((float)displayZoom,(float)applied);zoomAnimation.setDuration(110);
            zoomAnimation.addUpdateListener(a->{displayZoom=(float)a.getAnimatedValue();transform();});zoomAnimation.start();}
        else{displayZoom=applied;transform();}
    });}
    public void highRestored(boolean restored){ui(()->{high=restored;quality.selected(high);});}
    public void zoomRestored(double restored){ui(()->{
        if(zoomAnimation!=null)zoomAnimation.cancel();targetZoom=restored;displayZoom=restored;
        awaitingFrame=false;frameReady=false;updateZoomSelection();transform();
    });}
    public void ready(String text,boolean control){ui(()->{frameReady=true;details.setText(text);refreshModes();});}
    public void modeChanged(int actual){ui(()->{mode=actual;refreshModes();});}
    public void features(boolean available){ui(()->{hdrAvailable=available;if(!available&&mode==CameraEngine.HDR)mode=CameraEngine.PHOTO;refreshModes();});}
    public void state(String text,boolean value){ui(()->{busy=value;status.setText(text);status.setTextColor(Ui.MUTED);shutter.invalidate();});}
    public void exposure(String text){ui(()->exposure.setText(text));}
    public void compensation(float value,boolean automatic){ui(()->{if(overlay.moving||SystemClock.uptimeMillis()-lastEvGesture<250)return;Rational step=current==null?null:current.characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);if(step!=null&&step.floatValue()>0)ev=Math.round(value/step.floatValue());evLabel.setText(String.format(Locale.getDefault(),"EV %+.1f%s",value,automatic?" A":""));overlay.invalidate();});}
    public void locked(boolean value){ui(()->{locked=value;lock.selected(value);overlay.invalidate();});}
    public void error(String text){ui(()->{busy=false;status.setText(text);status.setTextColor(0xffffc49b);shutter.invalidate();});}
    public void saved(Uri photo,Uri original,EditRecipe recipe,String name,String text){ui(()->{lastPhoto=photo;lastOriginal=original;lastRecipe=recipe.copy();details.setText(text);try{getPreferences(0).edit().putString("last_photo",photo.toString()).putString("last_original",original.toString()).putString("last_recipe",recipe.json().toString()).apply();}catch(Exception ignored){}});}
    public void diagnostic(Uri uri){ui(()->share(uri,"application/json","Diagnóstico Natural Cam"));}
    public void palette(Uri uri){ui(()->{try{startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"image/png").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));}catch(Exception e){share(uri,"image/png","Paleta");}});}
    private void restoreLast(){String s=getPreferences(0).getString("last_original",null);if(s!=null)try{lastOriginal=Uri.parse(s);lastPhoto=Uri.parse(getPreferences(0).getString("last_photo",s));lastRecipe=EditRecipe.from(new JSONObject(getPreferences(0).getString("last_recipe","{}")));}catch(Exception ignored){}}
    private void rebuildZoom(){
        if(zooms.getChildCount()>0&&minimum==builtZoomMin&&maximum==builtZoomMax){updateZoomSelection();return;}builtZoomMin=minimum;builtZoomMax=maximum;zooms.removeAllViews();float[] choices={.6f,1,2,3,3.5f,6,7,8};
        for(float z:choices){if(z<minimum-.04||z>maximum+.01)continue;String name=(z==(int)z?Integer.toString((int)z):String.format(Locale.getDefault(),"%.1f",z))+"×";TextView t=Ui.action(this,name);t.setTextSize(13);t.setSingleLine(true);t.setMinWidth(dp(40));t.setPadding(dp(6),0,dp(6),0);t.setTag(z);t.setBackground(Ui.rounded(this,0xff202226,18));t.setOnClickListener(v->zoomTo(z));LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(-2,dp(36));cell.setMargins(dp(2),dp(6),dp(2),dp(6));zooms.addView(t,cell);}updateZoomSelection();
    }
    private void updateZoomSelection(){for(int i=0;i<zooms.getChildCount();i++){TextView t=(TextView)zooms.getChildAt(i);boolean chosen=Math.abs(targetZoom-(Float)t.getTag())<.04;t.setTextColor(chosen?Ui.ACCENT:Ui.TEXT);if(t.isSelected()!=chosen){t.setSelected(chosen);t.setBackground(Ui.rounded(this,chosen?0xff36302a:0xff202226,18));}}}
    private void zoomTo(float value){zoomTo(value,true);}
    private void zoomTo(float value,boolean animate){
        value=Math.max(minimum,Math.min(maximum,value));targetZoom=value;animateZoomOnApply=animate;
        if(zoomAnimation!=null)zoomAnimation.cancel();
        // Do not digitally stretch the OLD lens to the target focal length.
        // Await CameraEngine.zoomApplied (same lens) or a new configured preview frame.
        engine.zoom(value);updateZoomSelection();
    }
    private void edit(){Intent intent=new Intent(this,EditorActivity.class);if(lastOriginal!=null){intent.setData(lastOriginal).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{intent.putExtra("recipe",lastRecipe.json().toString());}catch(Exception ignored){}}startActivity(intent);}
    private void showExposure(){if(current==null)return;LinearLayout content=new LinearLayout(this);content.setOrientation(1);addEv(content);Ui.sheet(this,"Exposição",content).show();}
    private void addEv(LinearLayout panel){Range<Integer> r=current.characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);int low=r==null?0:r.getLower(),high=r==null?0:r.getUpper();TextView title=Ui.text(this,"Compensação de exposição",14,Ui.TEXT);panel.addView(title);SeekBar slider=new SeekBar(this);slider.setMax(high-low);slider.setProgress(ev-low);panel.addView(slider,new LinearLayout.LayoutParams(-1,dp(48)));slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean user){if(user)setEv(p+low);}});}
    private void setEv(int value){if(current==null)return;Range<Integer> range=current.characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);if(range==null)return;lastEvGesture=SystemClock.uptimeMillis();ev=Math.max(range.getLower(),Math.min(range.getUpper(),value));Rational step=current.characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);evLabel.setText(String.format(Locale.getDefault(),"EV %+.1f",ev*(step==null?1:step.doubleValue())));if(lastSentEv!=ev){lastSentEv=ev;engine.exposureCompensation(ev);}}
    private void showDepth(){double[] f={1.4,2,2.8,4,5.6,8,11,16};LinearLayout content=new LinearLayout(this);content.setOrientation(1);TextView help=Ui.text(this,"Profundidade local. A abertura óptica permanece nos metadados.",13,Ui.MUTED);help.setPadding(0,dp(6),0,dp(12));content.addView(help);LinearLayout options=Ui.row(this);Dialog d=Ui.sheet(this,"Abertura virtual",content);for(double v:f){TextView t=Ui.action(this,String.format(Locale.getDefault(),"f/%.1f",v));if(Math.abs(v-aperture)<.01)t.setTextColor(Ui.ACCENT);t.setOnClickListener(x->{aperture=(float)v;depth.setText(String.format(Locale.getDefault(),"f/%.1f · virtual",v));engine.aperture(v);d.dismiss();});options.addView(t);}content.addView(Ui.horizontal(this,options),new LinearLayout.LayoutParams(-1,dp(48)));d.show();}
    private void showPro(){if(current==null)return;LinearLayout content=new LinearLayout(this);content.setOrientation(1);addEv(content);
        TextView title=Ui.text(this,"ISO",13,Ui.MUTED);content.addView(title);LinearLayout isos=Ui.row(this);int[] values={0,50,100,200,400,800,1600,3200,6400};for(int value:values){if(value>0&&(current.iso==null||!current.iso.contains(value)))continue;TextView t=Ui.action(this,value==0?"Auto":Integer.toString(value));t.setOnClickListener(v->{manualIso=value;engine.manual(manualIso,manualTime,null);});isos.addView(t);}content.addView(Ui.horizontal(this,isos),new LinearLayout.LayoutParams(-1,dp(48)));
        content.addView(Ui.text(this,"Velocidade",13,Ui.MUTED));LinearLayout speeds=Ui.row(this);int[] denominators={1000,500,250,125,60,30,15,4,1};for(int denominator:denominators){long time=1_000_000_000L/denominator;if(current.exposure!=null&&!current.exposure.contains(time))continue;TextView t=Ui.action(this,denominator==1?"1 s":"1/"+denominator);t.setOnClickListener(v->{manualTime=time;if(manualIso==0)manualIso=current.iso==null?100:Math.max(current.iso.getLower(),100);engine.manual(manualIso,manualTime,null);});speeds.addView(t);}content.addView(Ui.horizontal(this,speeds),new LinearLayout.LayoutParams(-1,dp(48)));
        if(current.minFocus>0){content.addView(Ui.text(this,"Foco manual · perto → infinito",13,Ui.MUTED));SeekBar focus=new SeekBar(this);focus.setMax(100);focus.setProgress(100);focus.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean user){if(user)engine.manual(manualIso,manualTime,current.minFocus*(1-p/100f));}});content.addView(focus,new LinearLayout.LayoutParams(-1,dp(48)));}
        TextView peak=Ui.action(this,peaking?"Desativar focus peaking":"Ativar focus peaking");peak.setOnClickListener(v->{peaking=!peaking;peak.setText(peaking?"Desativar focus peaking":"Ativar focus peaking");overlay.invalidate();});content.addView(peak);
        TextView wb=Ui.action(this,"Travar balanço de branco");wb.setOnClickListener(v->{engine.awbLock(!wb.isSelected());wb.setSelected(!wb.isSelected());wb.setText(wb.isSelected()?"Liberar balanço de branco":"Travar balanço de branco");});content.addView(wb);
        TextView process=Ui.action(this,"JPEG · "+(processingOff?"sem redução de ruído":"processamento da câmera"));process.setOnClickListener(v->{processingOff=!processingOff;engine.processingOff(processingOff);process.setText("JPEG · "+(processingOff?"sem redução de ruído":"processamento da câmera"));});content.addView(process);
        TextView reset=Ui.action(this,"Redefinir controles");reset.setTextColor(Ui.ACCENT);reset.setOnClickListener(v->{manualIso=0;ev=0;lastSentEv=Integer.MIN_VALUE;processingOff=false;engine.processingOff(false);process.setText("JPEG · processamento da câmera");evLabel.setText("EV 0,0");engine.reset();});content.addView(reset);Ui.sheet(this,"Pro",content).show();
    }
    private void showInfo(){LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);String[] names={"Sobre Natural Cam 0.2.2","Exportar diagnóstico","Focus peaking","Grade","Inteligência de proteção de luzes","Paleta da última foto"};Dialog dialog=Ui.sheet(this,"Natural Cam",panel);for(int i=0;i<names.length;i++){final int n=i;TextView t=Ui.action(this,names[i]);t.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);panel.addView(t,new LinearLayout.LayoutParams(-1,dp(48)));t.setOnClickListener(v->{dialog.dismiss();switch(n){case 0:new AlertDialog.Builder(this).setTitle("Natural Cam · 0.2.2 em teste").setMessage("Captura, estilos e edição locais. JPEG e DNG quando liberados pelo aparelho. Zoom escolhe a óptica acessível e registra o recorte digital no DNG.\n\n40 estilos próprios inspirados em Leica, Fuji, Kodak e Hasselblad; sem afiliação ou LUTs oficiais. Originais preservados.\n\nRetrato usa profundidade relativa local. O número f é virtual, sem calibração óptica. Empilhamento RAW, fusão de três câmeras e revelação DNG própria ainda não estão implementados.\n\nO motor coleta métricas da cena, protege altas luzes dentro de limites e audita lente, recorte e arquivo. Não foi treinado nos OPPO. Não clareia automaticamente todas as sombras.\n\nNão modifica firmware nem substitui a câmera original. Sem permissão de internet.").setPositiveButton("Fechar",null).show();break;case 1:engine.exportDiagnostic();break;case 2:peaking=!peaking;overlay.invalidate();status.setText(peaking?"Peaking ligado · guia visual de bordas":"Peaking desligado");break;case 3:grid=!grid;overlay.invalidate();break;case 4:intelligent=!intelligent;engine.intelligence(intelligent);status.setText(intelligent?"Proteção conservadora de altas luzes ligada":"Proteção automática desligada");break;case 5:engine.exportPalette();break;}});}dialog.show();}
    private void share(Uri uri,String mime,String title){Intent i=new Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);i.setClipData(ClipData.newRawUri("arquivo",uri));try{startActivity(Intent.createChooser(i,title));}catch(Exception e){status.setText("Arquivo salvo. Não há aplicativo compatível para compartilhar.");}}
    private void sample(){if(!started||destroyed||sampling||!texture.isAvailable()||busy)return;sampling=true;Bitmap bitmap;try{bitmap=texture.getBitmap(192,Math.max(2,Math.round(192/ratio)));}catch(Exception e){sampling=false;return;}if(bitmap==null){sampling=false;return;}final boolean wantPeak=peaking;monitor.execute(()->{Bitmap edges=null;try{int w=bitmap.getWidth(),h=bitmap.getHeight();int[] px=new int[w*h];bitmap.getPixels(px,0,w,0,0,w,h);JSONObject metrics=SceneIntelligence.collect(px,w,h,Calendar.getInstance().get(Calendar.HOUR_OF_DAY));engine.observeScene(metrics);if(wantPeak){int[] mask=new int[px.length];for(int y=1;y<h-1;y++)for(int x=1;x<w-1;x++){int l=luma(px[y*w+x-1]),r=luma(px[y*w+x+1]),u=luma(px[(y-1)*w+x]),d=luma(px[(y+1)*w+x]);if(Math.abs(l-r)+Math.abs(u-d)>70)mask[y*w+x]=0xd8ff623b;}edges=Bitmap.createBitmap(mask,w,h,Bitmap.Config.ARGB_8888);}}catch(Exception ignored){}finally{bitmap.recycle();Bitmap ready=edges;ui(()->{sampling=false;Bitmap old=overlay.peak;overlay.peak=ready;if(old!=null)old.recycle();overlay.invalidate();});}});}
    private static int luma(int c){return(54*((c>>16)&255)+183*((c>>8)&255)+19*(c&255))>>8;}
    private final class Overlay extends View {
        final Paint p=new Paint(3);float x=-1,y=-1,dragOffsetY,dragFraction=.5f;boolean moving=false;Bitmap peak;
        final GestureDetector gestures;final ScaleGestureDetector pinch;
        Overlay(Context c){super(c);setClickable(true);setContentDescription("Visor. Toque para foco e exposição, segure para travar; arraste verticalmente ao lado do ponto para compensação.");gestures=new GestureDetector(c,new GestureDetector.SimpleOnGestureListener(){public boolean onDown(MotionEvent e){return true;}public boolean onSingleTapUp(MotionEvent e){if(moving)return true;x=e.getX();y=e.getY();engine.focus(x/getWidth(),y/getHeight());invalidate();return true;}public void onLongPress(MotionEvent e){if(locked)engine.lock(false);else{x=e.getX();y=e.getY();engine.focusAndLock(x/getWidth(),y/getHeight());invalidate();}}});pinch=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){public boolean onScale(ScaleGestureDetector d){zoomTo((float)(targetZoom*d.getScaleFactor()),false);return true;}});}
        public boolean onTouchEvent(MotionEvent e){
            pinch.onTouchEvent(e);if(e.getPointerCount()>1){moving=false;MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);gestures.onTouchEvent(cancel);cancel.recycle();return true;}
            if(e.getAction()==MotionEvent.ACTION_DOWN){moving=x>=0&&Math.abs(e.getX()-sliderX())<dp(24)&&e.getY()>=trackTop()-dp(14)&&e.getY()<=trackBottom()+dp(14);if(moving){dragFraction=ExposureScale.fraction(ev,evLow(),evHigh());dragOffsetY=e.getY()-ExposureScale.thumb(dragFraction,trackTop(),trackBottom(),dp(5));lastEvGesture=SystemClock.uptimeMillis();return true;}}
            if(moving&&(e.getAction()==MotionEvent.ACTION_MOVE||e.getAction()==MotionEvent.ACTION_UP)){dragFraction=ExposureScale.fromY(e.getY()-dragOffsetY,trackTop(),trackBottom(),dp(5));setEv(ExposureScale.steps(dragFraction,evLow(),evHigh()));invalidate();if(e.getAction()==MotionEvent.ACTION_UP){moving=false;lastEvGesture=SystemClock.uptimeMillis();performClick();}return true;}
            if(e.getAction()==MotionEvent.ACTION_CANCEL){moving=false;invalidate();}gestures.onTouchEvent(e);return true;
        }
        int evLow(){Range<Integer> r=current==null?null:current.characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);return r==null?0:r.getLower();}
        int evHigh(){Range<Integer> r=current==null?null:current.characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);return r==null?0:r.getUpper();}
        float trackTop(){float h=Math.min(dp(130),Math.max(0,getHeight()-dp(24)));return Math.max(dp(12),Math.min(getHeight()-dp(12)-h,y-h/2));}
        float trackBottom(){return trackTop()+Math.min(dp(130),Math.max(0,getHeight()-dp(24)));}
        float sliderX(){return Math.max(dp(18),Math.min(getWidth()-dp(18),x>getWidth()-dp(80)?x-dp(48):x+dp(48)));}
        public boolean performClick(){return super.performClick();}
        protected void onDraw(Canvas c){super.onDraw(c);if(peaking&&peak!=null)c.drawBitmap(peak,null,new Rect(0,0,getWidth(),getHeight()),null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(.6f));p.setColor(0x55ffffff);if(grid)for(int i=1;i<3;i++){c.drawLine(getWidth()*i/3f,0,getWidth()*i/3f,getHeight(),p);c.drawLine(0,getHeight()*i/3f,getWidth(),getHeight()*i/3f,p);}if(x>=0){p.setColor(Ui.ACCENT);p.setStrokeWidth(dp(1.3f));float a=dp(28);c.drawRoundRect(x-a,y-a,x+a,y+a,dp(3),dp(3),p);float sx=sliderX();c.drawLine(sx,trackTop(),sx,trackBottom(),p);c.drawCircle(sx,ExposureScale.thumb(moving?dragFraction:ExposureScale.fraction(ev,evLow(),evHigh()),trackTop(),trackBottom(),dp(5)),dp(5),p);if(locked){p.setTextSize(dp(11));p.setStyle(Paint.Style.FILL);c.drawText(current!=null&&!current.aeLock?"AF LOCK":current!=null&&current.minFocus==0?"AE LOCK":"AE/AF LOCK",Math.max(dp(8),x-dp(35)),y-dp(40),p);}}}
    }
    private final class Shutter extends View {
        final Paint p=new Paint(3);
        Shutter(Context c){super(c);setClickable(true);setFocusable(true);}
        @Override public boolean onTouchEvent(MotionEvent event){
            switch(event.getActionMasked()){
                case MotionEvent.ACTION_DOWN:
                    setPressed(true);invalidate();
                    // Trigger once on finger-down; ACTION_UP must not repeat the capture.
                    performClick();return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    setPressed(false);invalidate();return true;
                case MotionEvent.ACTION_MOVE:return true;
                default:return super.onTouchEvent(event);
            }
        }
        @Override public boolean performClick(){super.performClick();return true;}
        protected void onDraw(Canvas c){float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(x,y)-dp(3);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(busy?0xff777777:Ui.TEXT);c.drawCircle(x,y,r,p);p.setStyle(Paint.Style.FILL);p.setColor(busy?0xff444444:Ui.ACCENT);c.drawCircle(x,y,r-dp(6),p);}}
}
