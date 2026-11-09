package br.com.duvier.camera;

import java.io.*;
import java.util.*;

/** Changes default framing metadata only; RAW strips, CFA and colour matrices remain untouched. */
public final class RawDngCrop {
    private static final int ORIGIN=50719, SIZE=50720, ACTIVE=50829;
    public static final class Crop {
        public final int x,y,width,height,sensorWidth,sensorHeight;
        Crop(int x,int y,int w,int h,int sw,int sh){this.x=x;this.y=y;width=w;height=h;sensorWidth=sw;sensorHeight=sh;}
    }
    private static final class Entry {int tag,type;long count,value,position;byte[] bytes;}
    private static final class Directory {long offset,reference,next;List<Entry> entries=new ArrayList<>();}
    private final RandomAccessFile file;
    private final boolean little;
    private RawDngCrop(RandomAccessFile f)throws IOException{
        file=f;int a=f.readUnsignedByte(),b=f.readUnsignedByte();
        if(a==73&&b==73)little=true;else if(a==77&&b==77)little=false;else throw new IOException("DNG sem cabeçalho TIFF.");
        if(u16()!=42)throw new IOException("Formato TIFF não suportado.");
    }
    public static Crop apply(File file,double zoom)throws IOException {
        try(RandomAccessFile f=new RandomAccessFile(file,"rw")){
            RawDngCrop t=new RawDngCrop(f);long first=t.u32();
            Directory d=t.find(first,4,new HashSet<Long>());
            if(d==null)throw new IOException("Plano RAW do DNG não identificado; enquadramento não foi alterado.");
            int width=(int)t.scalar(d,256),height=(int)t.scalar(d,257);
            long[] active=t.values(t.entry(d,ACTIVE));
            int aw=width,ah=height;
            if(active!=null&&active.length==4){aw=(int)(active[3]-active[1]);ah=(int)(active[2]-active[0]);}
            if(aw<2||ah<2||aw>width||ah>height)throw new IOException("Área ativa inválida no DNG.");
            int[] c=FrameGeometry.centerCrop(aw,ah,zoom);
            if(zoom<=1.00001)return new Crop(0,0,aw,ah,width,height);
            t.append(d,c);
            return new Crop(c[0],c[1],c[2],c[3],width,height);
        }
    }
    private Directory find(long offset,long reference,Set<Long> seen)throws IOException{
        if(offset<8||offset>file.length()-6||seen.size()>48||!seen.add(offset))return null;
        file.seek(offset);int count=u16();if(count>4096||offset+2+count*12L+4>file.length())return null;
        Directory d=new Directory();d.offset=offset;d.reference=reference;
        for(int i=0;i<count;i++){
            Entry e=new Entry();e.position=file.getFilePointer();e.bytes=new byte[12];file.readFully(e.bytes);
            file.seek(e.position);e.tag=u16();e.type=u16();e.count=u32();e.value=u32();d.entries.add(e);
        }
        long nextReference=file.getFilePointer();d.next=u32();
        long photo=scalar(d,262);
        if((photo==32803||photo==34892)&&scalar(d,256)>0&&scalar(d,257)>0)return d;
        Entry sub=entry(d,330);
        if(sub!=null){long[] pointers=values(sub);if(pointers!=null)for(int i=0;i<pointers.length;i++){
            long pointerPosition=sub.count==1?sub.position+8:sub.value+i*4L;
            Directory found=find(pointers[i],pointerPosition,seen);if(found!=null)return found;
        }}
        return find(d.next,nextReference,seen);
    }
    private Entry entry(Directory d,int tag){for(Entry e:d.entries)if(e.tag==tag)return e;return null;}
    private long scalar(Directory d,int tag)throws IOException {long[] v=values(entry(d,tag));return v==null||v.length==0?0:v[0];}
    private long[] values(Entry e)throws IOException{
        if(e==null||e.count<1||e.count>128||(e.type!=3&&e.type!=4))return null;
        int size=e.type==3?2:4;long pos=size*e.count<=4?e.position+8:e.value;
        if(pos<0||pos+size*e.count>file.length())return null;
        file.seek(pos);long[] result=new long[(int)e.count];for(int i=0;i<result.length;i++)result[i]=size==2?u16():u32();return result;
    }
    private void append(Directory old,int[] crop)throws IOException{
        TreeMap<Integer,Entry> entries=new TreeMap<>();for(Entry e:old.entries)if(e.tag!=ORIGIN&&e.tag!=SIZE)entries.put(e.tag,e);
        long start=(file.length()+3)&~3L;file.seek(start);
        int count=entries.size()+2;long values=start+2+count*12L+4;
        entries.put(ORIGIN,null);entries.put(SIZE,null);w16(count);
        for(Map.Entry<Integer,Entry> pair:entries.entrySet()){
            if(pair.getValue()!=null)file.write(pair.getValue().bytes);
            else {w16(pair.getKey());w16(4);w32(2);w32(values+(pair.getKey()==SIZE?8:0));}
        }
        w32(old.next);w32(crop[0]);w32(crop[1]);w32(crop[2]);w32(crop[3]);
        file.seek(old.reference);w32(start);
        file.getFD().sync();
    }
    private int u16()throws IOException{int a=file.readUnsignedByte(),b=file.readUnsignedByte();return little?a|(b<<8):(a<<8)|b;}
    private long u32()throws IOException{long a=u16(),b=u16();return little?a|(b<<16):(a<<16)|b;}
    private void w16(long n)throws IOException{if(little){file.write((int)n&255);file.write((int)(n>>8)&255);}else{file.write((int)(n>>8)&255);file.write((int)n&255);}}
    private void w32(long n)throws IOException{if(n<0||n>0xffffffffL)throw new IOException("DNG excede o TIFF clássico.");if(little){w16(n);w16(n>>16);}else{w16(n>>16);w16(n);}}
}
