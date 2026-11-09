package br.com.duvier.camera;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import org.tensorflow.lite.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.Arrays;

/** Local relative-depth inference. The virtual f-number is artistic, not calibrated optical aperture.
 * Model: Depth Anything V2 Small (Apache-2.0), Qualcomm AI Hub float conversion.
 * Tensor contract checked against Photon Camera a57a1877 (Apache-2.0); original Java implementation.
 */
final class DepthEngine {
    static synchronized Bokeh prepare(Context context,Bitmap image,float fx,float fy,float aperture)throws Exception {
        ActivityManager.MemoryInfo memory=new ActivityManager.MemoryInfo();((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(memory);
        if(memory.lowMemory||memory.availMem<800L*1024*1024)throw new IOException("Memória disponível insuficiente para estimar profundidade. O original foi preservado.");
        try(AssetFileDescriptor asset=context.getAssets().openFd("depth_anything_v2.tflite");FileInputStream file=new FileInputStream(asset.getFileDescriptor())){
            ByteBuffer model=file.getChannel().map(FileChannel.MapMode.READ_ONLY,asset.getStartOffset(),asset.getDeclaredLength());
            try(Interpreter net=new Interpreter(model,new Interpreter.Options().setNumThreads(Math.min(4,Math.max(1,Runtime.getRuntime().availableProcessors()/2))).setUseXNNPACK(true))){
                Tensor input=net.getInputTensor(0),output=net.getOutputTensor(0);int[] in=input.shape(),out=output.shape();
                if(in.length!=4||input.dataType()!=DataType.FLOAT32||output.dataType()!=DataType.FLOAT32)throw new IOException("Contrato do modelo de profundidade incompatível.");
                boolean first=in[1]==3;int iw=first?in[3]:in[2],ih=first?in[2]:in[1];
                int ow=out.length==3?out[2]:out.length==4&&out[3]==1?out[2]:out[out.length-1],oh=out.length==3?out[1]:out.length==4&&out[3]==1?out[1]:out[out.length-2];
                if(iw<1||ih<1||ow<1||oh<1||ow*oh>2_000_000)throw new IOException("Dimensões de profundidade inválidas.");
                Bitmap scaled=Bitmap.createScaledBitmap(image,iw,ih,true);int[] p=new int[iw*ih];scaled.getPixels(p,0,iw,0,0,iw,ih);if(scaled!=image)scaled.recycle();
                ByteBuffer ib=ByteBuffer.allocateDirect(iw*ih*3*4).order(ByteOrder.nativeOrder());
                if(first){for(int c=0;c<3;c++)for(int pixel:p)ib.putFloat(((pixel>>(16-c*8))&255)/255f);}else for(int pixel:p){ib.putFloat(((pixel>>16)&255)/255f);ib.putFloat(((pixel>>8)&255)/255f);ib.putFloat((pixel&255)/255f);}ib.rewind();
                ByteBuffer ob=ByteBuffer.allocateDirect(output.numBytes()).order(ByteOrder.nativeOrder());net.run(ib,ob);ob.rewind();float[] depth=new float[ow*oh];ob.asFloatBuffer().get(depth);
                float[] sorted=depth.clone();Arrays.sort(sorted);float min=sorted[Math.max(0,(int)(sorted.length*.02))],max=sorted[Math.min(sorted.length-1,(int)(sorted.length*.98))];
                if(!Float.isFinite(min)||!Float.isFinite(max)||max-min<1e-5f)throw new IOException("Mapa de profundidade sem variação confiável; original preservado.");
                for(int i=0;i<depth.length;i++)depth[i]=clamp((depth[i]-min)/(max-min));
                return new Bokeh(image,depth,ow,oh,fx,fy,aperture);
            }
        }
    }
    static final class Bokeh implements AutoCloseable {
        private final int width,height;private int[] original;private int[][] layers;private float[] depth;
        private final float focus;private float aperture;
        Bokeh(Bitmap guide,float[] small,int sw,int sh,float fx,float fy,float aperture){
            width=guide.getWidth();height=guide.getHeight();this.aperture=Math.max(1.4f,Math.min(16,aperture));original=new int[width*height];guide.getPixels(original,0,width,0,0,width,height);depth=new float[width*height];
            for(int y=0;y<height;y++)for(int x=0;x<width;x++)depth[y*width+x]=sample(small,sw,sh,x/(float)Math.max(1,width-1),y/(float)Math.max(1,height-1));
            int cx=Math.max(0,Math.min(width-1,(int)(fx*width))),cy=Math.max(0,Math.min(height-1,(int)(fy*height)));float[] values=new float[25];int k=0;for(int yy=-2;yy<=2;yy++)for(int xx=-2;xx<=2;xx++)values[k++]=depth[Math.max(0,Math.min(height-1,cy+yy))*width+Math.max(0,Math.min(width-1,cx+xx))];Arrays.sort(values);focus=values[12];
            // Four depth-gated disk layers; blur does not borrow across the in-focus subject.
            int[] radii={2,6,14,30};layers=new int[4][];for(int i=0;i<4;i++)layers[i]=blur(radii[i]);
        }
        private int[] blur(int radius){
            int[] out=new int[original.length];float scale=width/1440f;
            for(int y=0;y<height;y++)for(int x=0;x<width;x++){
                int index=y*width+x;float d=depth[index],delta=d-focus;if(Math.abs(delta)<.055f){out[index]=original[index];continue;}
                float r=0,g=0,b=0,w=0;for(int n=0;n<25;n++){double angle=n*2.39996323;double rr=radius*scale*Math.sqrt((n+.5)/25.);int xx=Math.max(0,Math.min(width-1,x+(int)Math.round(rr*Math.cos(angle)))),yy=Math.max(0,Math.min(height-1,y+(int)Math.round(rr*Math.sin(angle))));int at=yy*width+xx;float nd=depth[at];
                    if(delta<0&&nd>focus-.025f||delta>0&&nd<focus+.025f||Math.abs(nd-d)>.18f)continue;
                    int c=original[at];float weight=1-Math.min(1,Math.abs(nd-d)/.18f);r+=((c>>16)&255)*weight;g+=((c>>8)&255)*weight;b+=(c&255)*weight;w+=weight;
                }
                out[index]=w<1?original[index]:0xff000000|(Math.min(255,Math.round(r/w))<<16)|(Math.min(255,Math.round(g/w))<<8)|Math.min(255,Math.round(b/w));
            }return out;
        }
        void aperture(float value){aperture=Math.max(1.4f,Math.min(16,value));}
        void apply(int[] pixels,int outputWidth,int rows,int top,int fullHeight){
            for(int y=0;y<rows;y++)for(int x=0;x<outputWidth;x++){
                float nx=x/(float)Math.max(1,outputWidth-1),ny=(top+y)/(float)Math.max(1,fullHeight-1);float d=sample(depth,width,height,nx,ny),delta=Math.abs(d-focus);
                if(delta<.065f)continue;float radius=(delta-.065f)*55*(5.6f/aperture);if(radius<.15f)continue;
                int lo;float f;if(radius<2){lo=-1;f=radius/2;}else if(radius<6){lo=0;f=(radius-2)/4;}else if(radius<14){lo=1;f=(radius-6)/8;}else{lo=2;f=Math.min(1,(radius-14)/16);}
                int at=y*outputWidth+x,a=lo<0?pixels[at]:sampleColor(layers[lo],nx,ny),b=sampleColor(layers[lo+1],nx,ny);pixels[at]=blend(a,b,f);
            }
        }
        private int sampleColor(int[] pixels,float x,float y){float xx=clamp(x)*(width-1),yy=clamp(y)*(height-1);int ix=Math.min(width-2,(int)xx),iy=Math.min(height-2,(int)yy);float fx=xx-ix,fy=yy-iy;return blend(blend(pixels[iy*width+ix],pixels[iy*width+ix+1],fx),blend(pixels[(iy+1)*width+ix],pixels[(iy+1)*width+ix+1],fx),fy);}
        public void close(){original=null;layers=null;depth=null;}
    }
    private static int blend(int a,int b,float f){int r=Math.round(((a>>16)&255)*(1-f)+((b>>16)&255)*f),g=Math.round(((a>>8)&255)*(1-f)+((b>>8)&255)*f),bb=Math.round((a&255)*(1-f)+(b&255)*f);return 0xff000000|(r<<16)|(g<<8)|bb;}
    private static float sample(float[] a,int w,int h,float x,float y){float xx=clamp(x)*(w-1),yy=clamp(y)*(h-1);int ix=Math.max(0,Math.min(w-2,(int)xx)),iy=Math.max(0,Math.min(h-2,(int)yy));float fx=xx-ix,fy=yy-iy;return (a[iy*w+ix]*(1-fx)+a[iy*w+ix+1]*fx)*(1-fy)+(a[(iy+1)*w+ix]*(1-fx)+a[(iy+1)*w+ix+1]*fx)*fy;}
    private static float clamp(float v){return Math.max(0,Math.min(1,v));}
}
