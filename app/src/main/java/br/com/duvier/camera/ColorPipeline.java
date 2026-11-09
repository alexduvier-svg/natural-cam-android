package br.com.duvier.camera;

/** Pixel processing shared by preview and full-resolution export. No texture change at defaults. */
public final class ColorPipeline {
    private final EditRecipe r;private final StyleCatalog.Style style;
    private final float[][] curves=new float[4][256];
    private static final float[] linear=new float[256];private static final int[] encoded=new int[65536];
    static {for(int i=0;i<256;i++){float v=i/255f;linear[i]=v<=.04045f?v/12.92f:(float)Math.pow((v+.055f)/1.055f,2.4);}for(int i=0;i<65536;i++){float v=i/65535f;encoded[i]=clamp255(Math.round(255*(v<=.0031308f?12.92f*v:1.055f*(float)Math.pow(v,1/2.4)-.055f)));}}
    private static final float[] HUE_CENTERS={0,1/12f,1/6f,1/3f,.5f,2/3f,.75f,5/6f,1};
    private final boolean hsl,local,detail,identity;private float[] mask;private int maskHeight=384;private static final int MASK=384;
    public ColorPipeline(EditRecipe recipe){
        r=recipe.copy();style=StyleCatalog.get(r.style);boolean hh=false;for(int i=0;i<8;i++)if(r.hslHue[i]!=0||r.hslSaturation[i]!=0||r.hslLuminance[i]!=0)hh=true;hsl=hh||r.hue!=0;
        local=!r.strokes.isEmpty()&&(r.localExposure!=0||r.localWarmth!=0||r.localSaturation!=0||r.localTexture!=0||r.localTint!=0);detail=r.sharpness!=0||(local&&r.localTexture!=0);
        boolean cc=false;for(int i=0;i<5;i++){if(r.curve[i]!=i/4f)cc=true;for(int k=0;k<3;k++)if(r.rgbCurves[k][i]!=i/4f)cc=true;}
        identity=(r.style==0||r.intensity==0)&&r.exposure==0&&r.contrast==0&&r.saturation==0&&r.temperature==0&&r.tint==0&&r.highlights==0&&r.shadows==0&&r.sharpness==0&&r.grain==0&&r.vignette==0&&!hsl&&!local&&!cc;
        for(int i=0;i<256;i++){float v=i/255f;curves[0][i]=curve(r.curve,v);for(int k=0;k<3;k++)curves[k+1][i]=curve(r.rgbCurves[k],v);}
        mask=null;
    }
    public boolean isIdentity(){return identity;}
    public void process(int[] pixels,int width,int height,int top,int fullHeight){
        if(identity)return;if(local&&mask==null){maskHeight=Math.max(2,Math.min(1536,Math.round(MASK*fullHeight/(float)width)));mask=createMask(r,MASK,maskHeight);}int[] original=detail?pixels.clone():pixels;float exp=(float)Math.pow(2,r.exposure),styleMix=r.intensity;
        float[] hsv=new float[3];
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            int i=y*width+x,c=original[i];float red=((c>>16)&255)/255f,green=((c>>8)&255)/255f,blue=(c&255)/255f;
            float srcR=red,srcG=green,srcB=blue;
            if(r.style!=0&&styleMix>0){
                float rr,gg,bb;if(style.mono){rr=gg=bb=red*style.mix[0]+green*style.mix[1]+blue*style.mix[2];}
                else {float[] m=style.mix;rr=red*m[0]+green*m[1]+blue*m[2];gg=red*m[3]+green*m[4]+blue*m[5];bb=red*m[6]+green*m[7]+blue*m[8];float lum=.2126f*rr+.7152f*gg+.0722f*bb;rr=lum+(rr-lum)*style.saturation;gg=lum+(gg-lum)*style.saturation;bb=lum+(bb-lum)*style.saturation;rr*=1+style.warmth*.25f;bb*=1-style.warmth*.25f;}
                rr=filmTone(rr,style);gg=filmTone(gg,style);bb=filmTone(bb,style);red+=styleMix*(rr-red);green+=styleMix*(gg-green);blue+=styleMix*(bb-blue);
            }
            if(r.exposure!=0){red=encode(linear[clamp255(Math.round(red*255))]*exp);green=encode(linear[clamp255(Math.round(green*255))]*exp);blue=encode(linear[clamp255(Math.round(blue*255))]*exp);}
            float lum=.2126f*red+.7152f*green+.0722f*blue;
            float lift=r.shadows*(1-lum)*(1-lum)*.24f+r.highlights*lum*lum*.24f;
            red+=lift;green+=lift;blue+=lift;
            red=(red-.5f)*(1+r.contrast*.7f)+.5f;green=(green-.5f)*(1+r.contrast*.7f)+.5f;blue=(blue-.5f)*(1+r.contrast*.7f)+.5f;
            red*=1+r.temperature*.28f;blue*=1-r.temperature*.28f;green*=1-r.tint*.22f;
            lum=.2126f*red+.7152f*green+.0722f*blue;float sat=1+r.saturation;red=lum+(red-lum)*sat;green=lum+(green-lum)*sat;blue=lum+(blue-lum)*sat;
            if(hsl){rgbToHsv(red,green,blue,hsv);float hue=hsv[0];float[] centers=HUE_CENTERS;int band=0;while(band<7&&hue>=centers[band+1])band++;int next=(band+1)%8;float f=(hue-centers[band])/(centers[band+1]-centers[band]);
                hsv[0]=wrap(hue+r.hue*.5f+(r.hslHue[band]*(1-f)+r.hslHue[next]*f)*.12f);hsv[1]=clamp(hsv[1]*(1+r.hslSaturation[band]*(1-f)+r.hslSaturation[next]*f));hsv[2]=clamp(hsv[2]+(r.hslLuminance[band]*(1-f)+r.hslLuminance[next]*f)*.25f);
                int out=hsvToRgb(hsv[0],hsv[1],hsv[2]);red=((out>>16)&255)/255f;green=((out>>8)&255)/255f;blue=(out&255)/255f;
            }
            red=lut(curves[1],lut(curves[0],red));green=lut(curves[2],lut(curves[0],green));blue=lut(curves[3],lut(curves[0],blue));
            float a=local?maskAt(x/(float)Math.max(1,width-1),(top+y)/(float)Math.max(1,fullHeight-1)):0;
            if(a>0){float e=(float)Math.pow(2,r.localExposure*a);red=encode(linear[clamp255(Math.round(red*255))]*e);green=encode(linear[clamp255(Math.round(green*255))]*e);blue=encode(linear[clamp255(Math.round(blue*255))]*e);red*=1+r.localWarmth*a*.25f;blue*=1-r.localWarmth*a*.25f;green*=1-r.localTint*a*.2f;lum=.2126f*red+.7152f*green+.0722f*blue;float s=1+r.localSaturation*a;red=lum+(red-lum)*s;green=lum+(green-lum)*s;blue=lum+(blue-lum)*s;}
            if(detail){int l=original[y*width+Math.max(0,x-1)],right=original[y*width+Math.min(width-1,x+1)],up=original[Math.max(0,y-1)*width+x],down=original[Math.min(height-1,y+1)*width+x];
                float amount=r.sharpness*.85f+r.localTexture*a*.70f;float dr=srcR-((((l>>16)&255)+((right>>16)&255)+((up>>16)&255)+((down>>16)&255))/1020f);float dg=srcG-((((l>>8)&255)+((right>>8)&255)+((up>>8)&255)+((down>>8)&255))/1020f);float db=srcB-(((l&255)+(right&255)+(up&255)+(down&255))/1020f);
                red+=dr*amount;green+=dg*amount;blue+=db*amount;
            }
            if(r.vignette!=0){float nx=x/(float)Math.max(1,width-1)*2-1,ny=(top+y)/(float)Math.max(1,fullHeight-1)*2-1;float edge=Math.max(0,(nx*nx+ny*ny-.15f)/1.85f);float v=1-r.vignette*edge*edge*.7f;red*=v;green*=v;blue*=v;}
            if(r.grain>0){int gx=(int)(x*1200f/width),gy=(int)((top+y)*1200f/width);int hash=gx*73856093^gy*19349663;hash=(hash^(hash>>>13))*1274126177;float noise=((hash&65535)/65535f-.5f)*r.grain*.16f;red+=noise;green+=noise;blue+=noise;}
            pixels[i]=0xff000000|(clamp255(Math.round(red*255))<<16)|(clamp255(Math.round(green*255))<<8)|clamp255(Math.round(blue*255));
        }
    }
    private static float filmTone(float v,StyleCatalog.Style s){v=clamp(v);float out=(v-.5f)*(1+s.contrast)+.5f;out=s.lift+(1-s.lift)*out;out-=s.shoulder*Math.max(0,out-.55f)*Math.max(0,out-.55f);return clamp(out);}
    private float encode(float linear){return encoded[Math.max(0,Math.min(65535,Math.round(linear*65535)))]/255f;}
    private static float curve(float[] nodes,float x){float p=clamp(x)*4;int i=Math.min(3,(int)p);return nodes[i]+(nodes[i+1]-nodes[i])*(p-i);}
    private static float lut(float[] table,float x){float p=clamp(x)*255;int i=Math.min(254,(int)p);return table[i]+(table[i+1]-table[i])*(p-i);}
    private static float clamp(float x){return Math.max(0,Math.min(1,x));}private static int clamp255(int x){return Math.max(0,Math.min(255,x));}private static float wrap(float x){return x-(float)Math.floor(x);}
    private static float[] createMask(EditRecipe recipe,int width,int height){
        float[] result=new float[width*height];for(float[] stroke:recipe.strokes){float radius=Math.max(1,stroke[2]*width),cx=stroke[0]*(width-1),cy=stroke[1]*(height-1);int left=Math.max(0,(int)(cx-radius)),right=Math.min(width-1,(int)Math.ceil(cx+radius)),top=Math.max(0,(int)(cy-radius)),bottom=Math.min(height-1,(int)Math.ceil(cy+radius));
            for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++){float d=(float)Math.hypot(x-cx,y-cy)/radius,value=clamp((1-d)/.45f);value=value*value*(3-2*value);int i=y*width+x;result[i]=stroke[3]<0?result[i]*(1-value):Math.max(result[i],value);}
        }return result;
    }
    private float maskAt(float x,float y){float xx=clamp(x)*(MASK-1),yy=clamp(y)*(maskHeight-1);int ix=Math.min(MASK-2,(int)xx),iy=Math.min(maskHeight-2,(int)yy);float fx=xx-ix,fy=yy-iy;return (mask[iy*MASK+ix]*(1-fx)+mask[iy*MASK+ix+1]*fx)*(1-fy)+(mask[(iy+1)*MASK+ix]*(1-fx)+mask[(iy+1)*MASK+ix+1]*fx)*fy;}
    private static void rgbToHsv(float r,float g,float b,float[] hsl){r=clamp(r);g=clamp(g);b=clamp(b);float max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b)),d=max-min,l=(max+min)*.5f;float hue=d==0?0:max==r?((g-b)/d)%6:max==g?(b-r)/d+2:(r-g)/d+4;hsl[0]=wrap(hue/6);hsl[1]=d==0?0:d/Math.max(1e-6f,1-Math.abs(2*l-1));hsl[2]=l;}
    private static int hsvToRgb(float hue,float saturation,float lightness){float h=wrap(hue)*6,c=(1-Math.abs(2*lightness-1))*saturation,x=c*(1-Math.abs(h%2-1)),m=lightness-c/2;float r,g,b;switch((int)h%6){case 0:r=c;g=x;b=0;break;case 1:r=x;g=c;b=0;break;case 2:r=0;g=c;b=x;break;case 3:r=0;g=x;b=c;break;case 4:r=x;g=0;b=c;break;default:r=c;g=0;b=x;}return 0xff000000|(clamp255(Math.round((r+m)*255))<<16)|(clamp255(Math.round((g+m)*255))<<8)|clamp255(Math.round((b+m)*255));}
}
