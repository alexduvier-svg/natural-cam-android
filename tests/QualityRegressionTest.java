import br.com.duvier.camera.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;

public final class QualityRegressionTest {
    static int checks=0;static void ok(boolean v,String msg){checks++;if(!v)throw new AssertionError(msg);}
    static LensRouter.Candidate candidate(String id,double optical,boolean raw,int priority){return new LensRouter.Candidate(id,optical,raw,priority);}
    static void lensChecks(){
        List<LensRouter.Candidate> x8=Arrays.asList(candidate("logical",1,true,12),candidate("main",1,true,58),candidate("ultra",.6,true,10),candidate("tele3",3,true,50),candidate("tele6",6,true,50));
        ok(LensRouter.choose(x8,1,true).equals("main"),"prefer physical main RAW over logical binned RAW");
        ok(LensRouter.choose(x8,3,true).equals("tele3"),"X8 3x optic");ok(LensRouter.choose(x8,6,true).equals("tele6"),"X8 6x optic");ok(LensRouter.choose(x8,7,true).equals("tele6"),"X8 7x uses 6x optic plus crop");ok(LensRouter.choose(x8,8,true).equals("tele6"),"X8 8x uses 6x optic plus crop");
        ok(Math.abs(FrameGeometry.localZoom(7,6)-7.0/6)<1e-8,"7x digital ratio");
        List<LensRouter.Candidate> reno=Arrays.asList(candidate("main200",1,true,200),candidate("tele50",3.5,true,50));
        ok(LensRouter.choose(reno,3,true).equals("main200"),"3.5x optic cannot provide wider 3x field");ok(LensRouter.choose(reno,3.5,true).equals("tele50"),"Reno native tele");ok(LensRouter.choose(reno,7,true).equals("tele50"),"Reno 7x tele plus 2x crop");ok(FrameGeometry.localZoom(7,3.5)==2,"Reno 7x crop math");
        int[] crop=FrameGeometry.centerCrop(8192,6144,2);ok(crop[2]==4096&&crop[3]==3072,"50 MP at 2x crop yields real ~12.6 MP");
        ok(LensRouter.choose(Arrays.asList(candidate("main",1,false,1)),3,true)==null,"never fabricate RAW when unsupported");
    }
    static void dngChecks(boolean little,boolean sub)throws Exception {
        File f=File.createTempFile("natural-crop-",".dng");byte[] bytes=new byte[4096];new Random(71).nextBytes(bytes);ByteBuffer b=ByteBuffer.wrap(bytes).order(little?ByteOrder.LITTLE_ENDIAN:ByteOrder.BIG_ENDIAN);b.put(0,(byte)(little?'I':'M'));b.put(1,(byte)(little?'I':'M'));b.putShort(2,(short)42);b.putInt(4,8);int raw=sub?128:8;
        if(sub){b.position(8);b.putShort((short)1);entry(b,330,4,1,raw);b.putInt(0);}
        b.position(raw);b.putShort((short)6);entry(b,256,4,1,800);entry(b,257,4,1,600);entry(b,262,3,1,little?32803:32803<<16);entry(b,273,4,1,512);entry(b,279,4,1,3584);entry(b,50829,4,4,400);b.putInt(0);b.position(400);b.putInt(4).putInt(8).putInt(596).putInt(792);Files.write(f.toPath(),bytes);
        byte[] pixels=Arrays.copyOfRange(bytes,512,4096);RawDngCrop.Crop c=RawDngCrop.apply(f,2);byte[] after=Files.readAllBytes(f.toPath());
        ok(c.width==392&&c.height==296,"DNG default crop respects ActiveArea "+little+sub);ok(c.x==196&&c.y==148,"DNG origin relative to ActiveArea "+little+sub);ok(Arrays.equals(pixels,Arrays.copyOfRange(after,512,4096)),"RAW pixel strips never altered "+little+sub);
        ByteBuffer out=ByteBuffer.wrap(after).order(b.order());int first=out.getInt(4);if(sub){ok(first==8,"thumbnail root preserved");first=out.getInt(8+2+8);}else ok(first>=4096,"raw directory appended safely");
        int count=out.getShort(first)&65535;boolean found=false;for(int n=0;n<count;n++){int at=first+2+n*12;if((out.getShort(at)&65535)==50720){int ptr=out.getInt(at+8);found=out.getInt(ptr)==392&&out.getInt(ptr+4)==296;}}
        ok(found,"standard DefaultCropSize written in RAW IFD "+little+sub);f.delete();
    }
    static void entry(ByteBuffer b,int tag,int type,int count,int value){b.putShort((short)tag).putShort((short)type).putInt(count).putInt(value);}
    static void imageChecks()throws Exception {
        ok(StyleCatalog.ALL.size()==41,"natural plus exactly 40 styles");for(String f:new String[]{"Leica","Fuji","Kodak","Hasselblad"})ok(StyleCatalog.ALL.stream().filter(s->s.family.equals(f)).count()==10,"ten styles for "+f);
        int[] original=new int[200];Random r=new Random(912);for(int i=0;i<original.length;i++)original[i]=r.nextInt()|0xff000000;int[] edited=original.clone();new ColorPipeline(new EditRecipe()).process(edited,20,10,0,10);ok(Arrays.equals(original,edited),"Natural defaults preserve every preview pixel exactly");
        EditRecipe e=new EditRecipe();e.exposure=1;int[] grey={0xff808080,0xff000000};new ColorPipeline(e).process(grey,2,1,0,1);int red=(grey[0]>>16)&255;ok(red>=174&&red<=178,"exposure uses linear-light gain");ok(grey[1]==0xff000000,"black is not lifted automatically");
        e=new EditRecipe();e.sharpness=-1;int[] point={0xff000000,0xff000000,0xff000000,0xff000000,0xffffffff,0xff000000,0xff000000,0xff000000,0xff000000};new ColorPipeline(e).process(point,3,3,0,3);ok(((point[4]>>16)&255)<255,"negative detail softens on request");
        int[] scene=new int[100];Arrays.fill(scene,0xffffffff);JSONObject noon=SceneIntelligence.collect(scene,10,10,12),night=SceneIntelligence.collect(scene,10,10,2);ok(noon.getString("environment_heuristic").equals(night.getString("environment_heuristic")),"clock never overrides measured scene");ok(SceneIntelligence.highlightProtectionSteps(noon,1.0/3,true)==0,"manual exposure has priority");ok(SceneIntelligence.highlightProtectionSteps(noon,1.0/3,false)<=0,"highlight protection never brightens all shadows");
        ScenePolicy policy=new ScenePolicy();JSONObject exposed=new JSONObject(noon.toString()).put("median_luma",.6).put("mean_luma",.6);int first=policy.update(exposed,1.0/3,false,true);JSONObject corrected=new JSONObject(exposed.toString()).put("median_luma",.6*Math.pow(2,first/3.0/2.2)).put("mean_luma",.6*Math.pow(2,first/3.0/2.2)).put("highlight_clipped_fraction",0);ok(policy.update(corrected,1.0/3,false,true)==first,"highlight action does not undo its own correction each frame");ok(Math.abs(SceneIntelligence.highlightProtectionSteps(noon,.25,false)*.25)<=.67,"protection respects its EV bound");ok(policy.update(corrected,1.0/3,true,true)==0,"user override clears policy");
        e=new EditRecipe();e.hslSaturation[1]=-1;int[] orange={0xffff8000,0xffff0000};new ColorPipeline(e).process(orange,2,1,0,1);int or=(orange[0]>>16)&255,og=(orange[0]>>8)&255,ob=orange[0]&255;ok(Math.abs(or-og)<3&&Math.abs(og-ob)<3&&or>125&&or<132,"orange HSL desaturation preserves lightness");ok(orange[1]==0xffff0000,"orange HSL control leaves pure red unchanged");
        e=new EditRecipe();e.localExposure=1;e.strokes.add(new float[]{.5f,.5f,.1f,1});int[] local=new int[100*100];Arrays.fill(local,0xff808080);new ColorPipeline(e).process(local,100,100,0,100);ok(((local[50*100+50]>>16)&255)>170&&local[0]==0xff808080,"local face exposure changes the painted area only");
        e=new EditRecipe();e.cropZoom=2;e.localExposure=.8f;e.strokes.add(new float[]{.5f,.4f,.1f,1});EditRecipe copy=e.copy();ok(copy.cropZoom==2&&copy.strokes.size()==1&&copy.localExposure==e.localExposure,"non-destructive recipe round trip");
    }
    static void interactionChecks(){
        for(int[] range:new int[][]{{-24,24},{-9,9},{-3,12},{0,0}}){
            float last=Float.POSITIVE_INFINITY;boolean bounded=true,monotonic=true,roundTrip=true;
            for(int step=range[0];step<=range[1];step++){float f=ExposureScale.fraction(step,range[0],range[1]),y=ExposureScale.thumb(f,20,150,5);bounded&=y>=25&&y<=145;monotonic&=y<=last;last=y;roundTrip&=ExposureScale.steps(ExposureScale.fromY(y,20,150,5),range[0],range[1])==step;}
            ok(bounded,"EV knob remains entirely within track "+Arrays.toString(range));ok(monotonic,"EV moves monotonically "+Arrays.toString(range));ok(roundTrip,"EV geometry respects physical step range "+Arrays.toString(range));
        }
        ok(ExposureScale.thumb(-3,0,130,5)==125&&ExposureScale.thumb(4,0,130,5)==5,"out-of-range dragging is clamped");
        float a=ExposureScale.thumb(.501f,0,130,5),b=ExposureScale.thumb(.502f,0,130,5);ok(a>b&&a-b<1,"marker can move continuously below one camera step");
        for(float exposure:new float[]{-1,-.4f,.4f,1}){EditRecipe r=new EditRecipe();r.exposure=exposure;int[] grey={0xff101010,0xff606060,0xffb0b0b0};new ColorPipeline(r).process(grey,3,1,0,1);boolean neutral=true;for(int c:grey)neutral&=((c>>16)&255)==((c>>8)&255)&&((c>>8)&255)==(c&255);ok(neutral,"exposure adjustment never adds a green cast "+exposure);}
    }
    public static void main(String[] args)throws Exception{lensChecks();for(boolean little:new boolean[]{false,true})for(boolean sub:new boolean[]{false,true})dngChecks(little,sub);imageChecks();interactionChecks();System.out.println(checks+" meaningful quality/RAW/optics checks passed");}
}
