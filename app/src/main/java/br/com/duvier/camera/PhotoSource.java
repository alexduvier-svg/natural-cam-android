package br.com.duvier.camera;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import androidx.exifinterface.media.ExifInterface;
import java.io.*;

/** Decodes original pixels in oriented stripes, rather than exporting a preview. */
final class PhotoSource implements AutoCloseable {
    final File file;final int encodedWidth,encodedHeight,width,height,orientation;
    final Matrix toDisplay=new Matrix(),fromDisplay=new Matrix();
    private final BitmapRegionDecoder decoder;
    PhotoSource(File file)throws IOException {
        this.file=file;BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getAbsolutePath(),o);
        if(o.outWidth<1||o.outHeight<1)throw new IOException("Formato não decodificável. Para DNG, abra o JPEG pareado.");
        encodedWidth=o.outWidth;encodedHeight=o.outHeight;int ori=1;
        try{ori=new ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION,1);}catch(Exception ignored){}
        orientation=ori;
        switch(ori){case 2:toDisplay.setScale(-1,1);break;case 3:toDisplay.setRotate(180);break;case 4:toDisplay.setScale(1,-1);break;case 5:toDisplay.setRotate(90);toDisplay.postScale(-1,1);break;case 6:toDisplay.setRotate(90);break;case 7:toDisplay.setRotate(270);toDisplay.postScale(-1,1);break;case 8:toDisplay.setRotate(270);break;}
        RectF bounds=new RectF(0,0,encodedWidth,encodedHeight);toDisplay.mapRect(bounds);toDisplay.postTranslate(-bounds.left,-bounds.top);toDisplay.invert(fromDisplay);
        width=Math.round(bounds.width());height=Math.round(bounds.height());decoder=BitmapRegionDecoder.newInstance(file.getAbsolutePath(),false);
    }
    Rect crop(double zoom){int[] c=FrameGeometry.centerCrop(width,height,zoom);return new Rect(c[0],c[1],c[0]+c[2],c[1]+c[3]);}
    Bitmap preview(int longest,double zoom)throws IOException {
        BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=1;while(Math.max(encodedWidth,encodedHeight)/o.inSampleSize>longest*1.5)o.inSampleSize*=2;
        o.inPreferredColorSpace=ColorSpace.get(ColorSpace.Named.SRGB);o.inPreferredConfig=Bitmap.Config.ARGB_8888;
        Bitmap b=BitmapFactory.decodeFile(file.getAbsolutePath(),o);if(b==null)throw new IOException("Não foi possível ler a fotografia.");
        Matrix m=new Matrix(toDisplay);Bitmap oriented=Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),m,false);if(oriented!=b)b.recycle();
        if(zoom>1.0001){int[] c=FrameGeometry.centerCrop(oriented.getWidth(),oriented.getHeight(),zoom);Bitmap crop=Bitmap.createBitmap(oriented,c[0],c[1],c[2],c[3]);if(crop!=oriented)oriented.recycle();oriented=crop;}
        if(Math.max(oriented.getWidth(),oriented.getHeight())>longest){float scale=longest/(float)Math.max(oriented.getWidth(),oriented.getHeight());Bitmap small=Bitmap.createScaledBitmap(oriented,Math.max(1,Math.round(oriented.getWidth()*scale)),Math.max(1,Math.round(oriented.getHeight()*scale)),true);if(small!=oriented)oriented.recycle();oriented=small;}
        return oriented;
    }
    Bitmap region(Rect display)throws IOException {
        RectF mapped=new RectF(display);fromDisplay.mapRect(mapped);
        Rect raw=new Rect(Math.max(0,(int)Math.floor(mapped.left)),Math.max(0,(int)Math.floor(mapped.top)),Math.min(encodedWidth,(int)Math.ceil(mapped.right)),Math.min(encodedHeight,(int)Math.ceil(mapped.bottom)));
        if(raw.isEmpty())throw new IOException("Região de imagem inválida.");
        BitmapFactory.Options o=new BitmapFactory.Options();o.inPreferredColorSpace=ColorSpace.get(ColorSpace.Named.SRGB);o.inPreferredConfig=Bitmap.Config.ARGB_8888;
        Bitmap tile=decoder.decodeRegion(raw,o);if(tile==null)throw new IOException("Não foi possível ler a região da imagem.");
        Bitmap out=Bitmap.createBitmap(display.width(),display.height(),Bitmap.Config.ARGB_8888);
        Matrix m=new Matrix(toDisplay);m.preTranslate(raw.left,raw.top);m.postTranslate(-display.left,-display.top);new Canvas(out).drawBitmap(tile,m,null);tile.recycle();return out;
    }
    public void close(){decoder.recycle();}
    static File importFile(Context context,Uri uri)throws IOException {
        File f=File.createTempFile("editor_source_",".image",context.getCacheDir());
        try(InputStream in=context.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(f)){if(in==null)throw new IOException("Arquivo sem acesso.");copy(in,out);}catch(Exception e){f.delete();if(e instanceof IOException)throw(IOException)e;throw new IOException(e);}
        return f;
    }
    static void copy(InputStream in,OutputStream out)throws IOException {byte[] buffer=new byte[256*1024];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);}
}
