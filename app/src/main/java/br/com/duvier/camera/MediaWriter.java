package br.com.duvier.camera;

import android.content.*;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.*;

/** Publishes completed files only. Originals are never overwritten. */
final class MediaWriter {
    interface WriteBody {void write(OutputStream out) throws Exception;}
    private final ContentResolver resolver;
    MediaWriter(Context c){resolver=c.getContentResolver();}
    Uri image(String name,String mime,WriteBody body) throws Exception {
        return publish(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,name,mime,Environment.DIRECTORY_DCIM+"/NaturalCam",body);
    }
    Uri document(String name,String mime,WriteBody body) throws Exception {
        return publish(MediaStore.Downloads.EXTERNAL_CONTENT_URI,name,mime,Environment.DIRECTORY_DOWNLOADS+"/NaturalCam",body);
    }
    private Uri publish(Uri collection,String name,String mime,String path,WriteBody body)throws Exception {
        ContentValues values=new ContentValues();values.put(MediaStore.MediaColumns.DISPLAY_NAME,name);
        values.put(MediaStore.MediaColumns.MIME_TYPE,mime);values.put(MediaStore.MediaColumns.RELATIVE_PATH,path);
        values.put(MediaStore.MediaColumns.IS_PENDING,1);
        Uri uri=resolver.insert(collection,values);if(uri==null)throw new IOException("Não foi possível criar o arquivo.");
        try {
            try(OutputStream out=resolver.openOutputStream(uri,"w")){if(out==null)throw new IOException("Sem acesso à gravação.");body.write(out);}
            ContentValues ready=new ContentValues();ready.put(MediaStore.MediaColumns.IS_PENDING,0);
            if(resolver.update(uri,ready,null,null)!=1)throw new IOException("Não foi possível concluir a gravação.");
            return uri;
        }catch(Exception|OutOfMemoryError e){resolver.delete(uri,null,null);throw e;}
    }
}
