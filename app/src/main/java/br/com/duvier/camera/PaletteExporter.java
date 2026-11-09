package br.com.duvier.camera;

import android.graphics.*;
import android.net.Uri;
import android.content.ContentResolver;
import java.io.InputStream;
import org.json.*;
import java.util.*;

/** Bounded decode, weighted colour clustering, no changes to source JPEG/DNG. */
final class PaletteExporter {
    static final int COLORS=6;
    static int[] extract(Bitmap image) {
        int[] count=new int[32768];
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
            int c=image.getPixel(x,y);if(Color.alpha(c)<128)continue;
            int bin=((Color.red(c)>>3)<<10)|((Color.green(c)>>3)<<5)|(Color.blue(c)>>3);count[bin]++;
        }
        List<Integer> bins=new ArrayList<>();for(int i=0;i<count.length;i++)if(count[i]>0)bins.add(i);
        bins.sort((a,b)->Integer.compare(count[b],count[a]));
        if(bins.isEmpty())return new int[]{Color.BLACK};
        double[][] centres=new double[COLORS][3];centres[0]=lab(rgb(bins.get(0)));
        for(int k=1;k<COLORS;k++) {
            int chosen=bins.get(0);double best=-1;
            for(int bin:bins) {double[] p=lab(rgb(bin));double distance=Double.MAX_VALUE;
                for(int q=0;q<k;q++)distance=Math.min(distance,dist(p,centres[q]));
                double score=distance*Math.sqrt(count[bin]);if(score>best){best=score;chosen=bin;}}
            centres[k]=lab(rgb(chosen));
        }
        int[] weights=new int[COLORS],rr=new int[COLORS],gg=new int[COLORS],bb=new int[COLORS];
        for(int loop=0;loop<10;loop++) {
            Arrays.fill(weights,0);Arrays.fill(rr,0);Arrays.fill(gg,0);Arrays.fill(bb,0);double[][] sums=new double[COLORS][3];
            for(int bin:bins) {int c=rgb(bin),pick=0;double[] p=lab(c);double best=Double.MAX_VALUE;
                for(int k=0;k<COLORS;k++){double d=dist(p,centres[k]);if(d<best){best=d;pick=k;}}
                int w=count[bin];weights[pick]+=w;rr[pick]+=Color.red(c)*w;gg[pick]+=Color.green(c)*w;bb[pick]+=Color.blue(c)*w;
                for(int ch=0;ch<3;ch++)sums[pick][ch]+=p[ch]*w;
            }
            for(int k=0;k<COLORS;k++)if(weights[k]>0)for(int ch=0;ch<3;ch++)centres[k][ch]=sums[k][ch]/weights[k];
        }
        Integer[] order={0,1,2,3,4,5};Arrays.sort(order,(a,b)->Integer.compare(weights[b],weights[a]));
        ArrayList<Integer> colors=new ArrayList<>();
        for(int k:order)if(weights[k]>0){int c=Color.rgb(rr[k]/weights[k],gg[k]/weights[k],bb[k]/weights[k]);if(!colors.contains(c))colors.add(c);}
        int[] result=new int[colors.size()];for(int i=0;i<result.length;i++)result[i]=colors.get(i);return result;
    }
    private static int rgb(int b){return Color.rgb(((b>>10)&31)*8+4,((b>>5)&31)*8+4,(b&31)*8+4);}
    private static double linear(int v){double c=v/255.0;return c<=0.04045?c/12.92:Math.pow((c+0.055)/1.055,2.4);}
    private static double[] lab(int c){double r=linear(Color.red(c)),g=linear(Color.green(c)),b=linear(Color.blue(c));
        double l=Math.cbrt(.4122214708*r+.5363325363*g+.0514459929*b),m=Math.cbrt(.2119034982*r+.6806995451*g+.1073969566*b),s=Math.cbrt(.0883024619*r+.2817188376*g+.6299787005*b);
        return new double[]{.2104542553*l+.793617785*m-.0040720468*s,1.9779984951*l-2.428592205*m+.4505937099*s,.0259040371*l+.7827717662*m-.808675766*s};}
    private static double dist(double[]a,double[]b){double d=0;for(int i=0;i<3;i++)d+=(a[i]-b[i])*(a[i]-b[i]);return d;}
    static Uri export(MediaWriter writer,byte[] jpeg,String basename) throws Exception {
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(jpeg,0,jpeg.length,bounds);
        BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=1;while(Math.max(bounds.outWidth,bounds.outHeight)/opts.inSampleSize>512)opts.inSampleSize*=2;
        opts.inPreferredColorSpace=ColorSpace.get(ColorSpace.Named.SRGB);
        Bitmap sample=BitmapFactory.decodeByteArray(jpeg,0,jpeg.length,opts);if(sample==null)throw new IllegalArgumentException("Não foi possível ler a foto.");
        return exportSample(writer,sample,basename);
    }
    static Uri export(MediaWriter writer,ContentResolver resolver,Uri image,String basename)throws Exception {
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
        try(InputStream in=resolver.openInputStream(image)){BitmapFactory.decodeStream(in,null,bounds);}
        BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=1;while(Math.max(bounds.outWidth,bounds.outHeight)/o.inSampleSize>512)o.inSampleSize*=2;o.inPreferredColorSpace=ColorSpace.get(ColorSpace.Named.SRGB);
        Bitmap sample;try(InputStream in=resolver.openInputStream(image)){sample=BitmapFactory.decodeStream(in,null,o);}if(sample==null)throw new java.io.IOException("Foto não decodificável.");return exportSample(writer,sample,basename);
    }
    private static Uri exportSample(MediaWriter writer,Bitmap sample,String basename)throws Exception {
        int[] colors;try{colors=extract(sample);}finally{sample.recycle();}
        JSONObject j=new JSONObject().put("source",basename+".jpg").put("color_space","sRGB").put("method","weighted OKLab clustering of bounded source sample");
        JSONArray list=new JSONArray();for(int c:colors)list.put(String.format(Locale.US,"#%06X",c&0xffffff));j.put("colors",list);
        writer.document(basename+"_paleta.json","application/json",out->out.write(j.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        Bitmap card=Bitmap.createBitmap(1080,480,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(card);canvas.drawColor(Color.rgb(15,17,20));Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.WHITE);p.setTextSize(40);canvas.drawText("Paleta da fotografia",48,68,p);p.setTextSize(23);p.setColor(0xffbfc4cc);canvas.drawText("sRGB · "+basename,48,110,p);
        for(int i=0;i<colors.length;i++){float x=48+i*984f/colors.length,w=984f/colors.length;p.setColor(colors[i]);canvas.drawRect(x,150,x+w,338,p);p.setColor(Color.WHITE);p.setTextSize(24);canvas.drawText(String.format(Locale.US,"#%06X",colors[i]&0xffffff),x+8,382,p);}
        try{return writer.image(basename+"_paleta.png","image/png",out->{if(!card.compress(Bitmap.CompressFormat.PNG,100,out))throw new java.io.IOException("Falha na paleta.");});}finally{card.recycle();}
    }
}
