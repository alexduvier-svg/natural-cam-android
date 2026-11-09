package br.com.duvier.camera;
import java.io.*;
/** JPEG 4:4:4 exported scanline by scanline; output resolution is never silently reduced. */
final class NativeJpeg implements AutoCloseable {
    static {System.loadLibrary("duvier_jpeg");}
    private long handle;
    private static native long open(String path,int w,int h,int quality)throws IOException;
    private static native void rows(long handle,int[] pixels,int count)throws IOException;
    private static native void finish(long handle)throws IOException;
    private static native void abort(long handle);
    NativeJpeg(File target,int width,int height)throws IOException{handle=open(target.getAbsolutePath(),width,height,100);if(handle==0)throw new IOException("Codificador JPEG indisponível.");}
    void write(int[] pixels,int lines)throws IOException{if(handle==0)throw new IOException("Codificador fechado.");rows(handle,pixels,lines);}
    void complete()throws IOException{long h=handle;handle=0;finish(h);}
    public void close(){if(handle!=0){abort(handle);handle=0;}}
}
