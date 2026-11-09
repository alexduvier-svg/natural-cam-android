/* Duvier Camera: bounded-memory JPEG export. libjpeg-turbo is separately licensed. */
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <setjmp.h>
#include <stdint.h>
#include <jpeglib.h>

typedef struct {struct jpeg_error_mgr pub;jmp_buf jump;char message[JMSG_LENGTH_MAX];} Error;
typedef struct {struct jpeg_compress_struct jpeg;Error error;FILE *file;unsigned char *rgb;jint *argb;int width;} Writer;
static void on_error(j_common_ptr c){Error *e=(Error*)c->err;(*c->err->format_message)(c,e->message);longjmp(e->jump,1);}
static void fail(JNIEnv *env,const char *message){jclass c=(*env)->FindClass(env,"java/io/IOException");if(c)(*env)->ThrowNew(env,c,message);}
static void destroy(Writer *w){if(!w)return;jpeg_destroy_compress(&w->jpeg);if(w->file)fclose(w->file);free(w->rgb);free(w->argb);free(w);}
JNIEXPORT jlong JNICALL Java_br_com_duvier_camera_NativeJpeg_open(JNIEnv *env,jclass cls,jstring path,jint width,jint height,jint quality){
    if(width<1||height<1||width>65500||height>65500){fail(env,"Dimensões JPEG inválidas.");return 0;}
    Writer *w=calloc(1,sizeof(Writer));if(!w){fail(env,"Memória insuficiente.");return 0;}
    w->jpeg.err=jpeg_std_error(&w->error.pub);w->error.pub.error_exit=on_error;
    if(setjmp(w->error.jump)){fail(env,w->error.message);destroy(w);return 0;}
    jpeg_create_compress(&w->jpeg);
    const char *name=(*env)->GetStringUTFChars(env,path,0);w->file=fopen(name,"wb");(*env)->ReleaseStringUTFChars(env,path,name);
    if(!w->file){fail(env,"Não foi possível criar o JPEG temporário.");destroy(w);return 0;}
    w->width=width;w->rgb=malloc((size_t)width*3);w->argb=malloc((size_t)width*sizeof(jint));
    if(!w->rgb||!w->argb){fail(env,"Memória insuficiente para linha JPEG.");destroy(w);return 0;}
    jpeg_stdio_dest(&w->jpeg,w->file);w->jpeg.image_width=width;w->jpeg.image_height=height;w->jpeg.input_components=3;w->jpeg.in_color_space=JCS_RGB;
    jpeg_set_defaults(&w->jpeg);jpeg_set_quality(&w->jpeg,quality,TRUE);
    /* Preserve chroma detail: 4:4:4, optimized Huffman coding, no resize. */
    for(int i=0;i<3;i++){w->jpeg.comp_info[i].h_samp_factor=1;w->jpeg.comp_info[i].v_samp_factor=1;}
    w->jpeg.optimize_coding=TRUE;jpeg_start_compress(&w->jpeg,TRUE);
    return (jlong)(intptr_t)w;
}
JNIEXPORT void JNICALL Java_br_com_duvier_camera_NativeJpeg_rows(JNIEnv *env,jclass cls,jlong handle,jintArray pixels,jint count){
    Writer *w=(Writer*)(intptr_t)handle;if(!w){fail(env,"Codificador JPEG fechado.");return;}
    if(count<1||(*env)->GetArrayLength(env,pixels)<(jlong)w->width*count||w->jpeg.next_scanline+count>w->jpeg.image_height){fail(env,"Linhas JPEG inválidas.");return;}
    if(setjmp(w->error.jump)){fail(env,w->error.message);return;}
    for(int y=0;y<count;y++){
        (*env)->GetIntArrayRegion(env,pixels,y*w->width,w->width,w->argb);if((*env)->ExceptionCheck(env))return;
        for(int x=0;x<w->width;x++){unsigned int p=(unsigned int)w->argb[x];w->rgb[x*3]=(p>>16)&255;w->rgb[x*3+1]=(p>>8)&255;w->rgb[x*3+2]=p&255;}
        JSAMPROW row=w->rgb;jpeg_write_scanlines(&w->jpeg,&row,1);
    }
}
JNIEXPORT void JNICALL Java_br_com_duvier_camera_NativeJpeg_finish(JNIEnv *env,jclass cls,jlong handle){
    Writer *w=(Writer*)(intptr_t)handle;if(!w)return;
    if(setjmp(w->error.jump)){fail(env,w->error.message);destroy(w);return;}
    jpeg_finish_compress(&w->jpeg);destroy(w);
}
JNIEXPORT void JNICALL Java_br_com_duvier_camera_NativeJpeg_abort(JNIEnv *env,jclass cls,jlong handle){Writer *w=(Writer*)(intptr_t)handle;if(w)destroy(w);}
