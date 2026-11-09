package br.com.duvier.camera;
import android.content.Context;
import android.graphics.*;
import java.io.*;

/** Full-size stripe export with bounded memory and cooperative cancellation. */
final class ImageExporter {
    interface Progress {void update(int percent);boolean cancelled();}
    static final class Result {int width,height;boolean depthApplied;}
    static Result export(Context context,File source,File output,EditRecipe recipe,Progress progress)throws Exception {
        try(PhotoSource photo=new PhotoSource(source)){
            Rect crop=photo.crop(recipe.cropZoom);int width=crop.width(),height=crop.height();ColorPipeline colors=new ColorPipeline(recipe);
            DepthEngine.Bokeh bokeh=null;
            if(recipe.portrait){Bitmap guide=photo.preview(1440,recipe.cropZoom);try{bokeh=DepthEngine.prepare(context,guide,recipe.focusX,recipe.focusY,recipe.aperture);}finally{guide.recycle();}}
            final int lines=64,halo=2;int[] pixels=new int[width*(lines+halo*2)],body=new int[width*lines];
            try(NativeJpeg jpeg=new NativeJpeg(output,width,height)){
                for(int y=0;y<height;y+=lines){
                    if(progress!=null&&progress.cancelled())throw new IOException("Exportação cancelada; original preservado.");
                    int n=Math.min(lines,height-y),top=Math.max(0,y-halo),bottom=Math.min(height,y+n+halo);
                    Bitmap tile=photo.region(new Rect(crop.left,crop.top+top,crop.right,crop.top+bottom));
                    tile.getPixels(pixels,0,width,0,0,width,tile.getHeight());int tileHeight=tile.getHeight();tile.recycle();
                    if(bokeh!=null)bokeh.apply(pixels,width,tileHeight,top,height);
                    colors.process(pixels,width,tileHeight,top,height);System.arraycopy(pixels,(y-top)*width,body,0,width*n);jpeg.write(body,n);
                    if(progress!=null)progress.update((int)((y+n)*100L/height));
                }
                jpeg.complete();
            }finally{if(bokeh!=null)bokeh.close();}
            ExifMetadata.copy(source,output,width,height,recipe);Result result=new Result();result.width=width;result.height=height;result.depthApplied=recipe.portrait;return result;
        }catch(Exception|OutOfMemoryError e){output.delete();throw e;}
    }
    static Bitmap preview(Bitmap source,EditRecipe recipe,DepthEngine.Bokeh bokeh){
        int w=source.getWidth(),h=source.getHeight();int[] px=new int[w*h];source.getPixels(px,0,w,0,0,w,h);
        if(bokeh!=null&&recipe.portrait)bokeh.apply(px,w,h,0,h);new ColorPipeline(recipe).process(px,w,h,0,h);
        return Bitmap.createBitmap(px,w,h,Bitmap.Config.ARGB_8888);
    }
}
