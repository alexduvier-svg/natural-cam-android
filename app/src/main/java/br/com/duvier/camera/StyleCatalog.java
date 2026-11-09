package br.com.duvier.camera;
import java.util.*;
/** Original recipes inspired by published references; no proprietary LUTs are distributed. */
public final class StyleCatalog {
    public static final class Style {
        public final String family,name;public final float contrast,saturation,warmth,lift,shoulder;public final boolean mono;public final float[] mix;
        Style(String family,String name,float contrast,float sat,float warm,float lift,float shoulder,boolean mono,float[] mix){this.family=family;this.name=name;this.contrast=contrast;saturation=sat;warmth=warm;this.lift=lift;this.shoulder=shoulder;this.mono=mono;this.mix=mix;}
    }
    public static final String[] FAMILIES={"Original","Leica","Fuji","Kodak","Hasselblad"};
    private static final float[] NEUTRAL={1,0,0,0,1,0,0,0,1}, WARM={1.025f,.005f,-.03f,-.005f,1.01f,-.005f,-.025f,.015f,1.01f}, CHROME={1.03f,-.015f,-.015f,-.015f,1.035f,-.02f,-.035f,.045f,.99f}, PASTEL={.98f,.025f,-.005f,.01f,.995f,-.005f,.01f,.015f,.975f};
    public static final List<Style> ALL;
    static {
        ArrayList<Style> s=new ArrayList<>();s.add(new Style("Original","Natural",0,1,0,0,0,false,NEUTRAL));
        add(s,"Leica","L · Natural",.04f,.98f,0,0,.02f,false,NEUTRAL);
        add(s,"Leica","L · Clássico",.14f,.88f,.04f,.006f,.04f,false,CHROME);
        add(s,"Leica","L · Cor intensa",.17f,1.16f,0,0,.03f,false,CHROME);
        add(s,"Leica","L · Retrato",.03f,.94f,.035f,.008f,.08f,false,WARM);
        add(s,"Leica","L · Rua cromática",.21f,1.08f,.015f,0,.02f,false,CHROME);
        add(s,"Leica","L · Monocromático",.14f,0,0,0,.035f,true,new float[]{.28f,.60f,.12f});
        add(s,"Leica","L · Prata suave",.04f,0,0,.018f,.08f,true,new float[]{.32f,.58f,.10f});
        add(s,"Leica","L · Documental denso",.31f,0,0,0,.025f,true,new float[]{.30f,.60f,.10f});
        add(s,"Leica","L · P&B vermelho",.19f,0,0,0,.04f,true,new float[]{.58f,.35f,.07f});
        add(s,"Leica","L · Noite P&B",.22f,0,0,.008f,.10f,true,new float[]{.25f,.65f,.10f});
        add(s,"Fuji","F · Equilíbrio",.04f,1.02f,0,0,.04f,false,NEUTRAL);
        add(s,"Fuji","F · Natureza viva",.18f,1.28f,-.01f,0,.025f,false,CHROME);
        add(s,"Fuji","F · Retrato suave",-.04f,.92f,.025f,.012f,.08f,false,PASTEL);
        add(s,"Fuji","F · Cromo clássico",.14f,.79f,-.015f,.012f,.06f,false,CHROME);
        add(s,"Fuji","F · Negativo clássico",.22f,.90f,.055f,.018f,.08f,false,WARM);
        add(s,"Fuji","F · Cinema suave",-.07f,.80f,-.025f,.025f,.12f,false,CHROME);
        add(s,"Fuji","F · Cor real",.07f,1.0f,.01f,0,.07f,false,NEUTRAL);
        add(s,"Fuji","F · Prata",.12f,0,0,.004f,.06f,true,new float[]{.29f,.61f,.10f});
        add(s,"Fuji","F · Prata amarelo",.17f,0,0,0,.055f,true,new float[]{.40f,.53f,.07f});
        add(s,"Fuji","F · Prata vermelho",.24f,0,0,0,.045f,true,new float[]{.63f,.32f,.05f});
        add(s,"Kodak","K · Retrato 160",-.035f,.91f,.04f,.008f,.09f,false,WARM);
        add(s,"Kodak","K · Retrato 400",.045f,.97f,.055f,.012f,.07f,false,WARM);
        add(s,"Kodak","K · Retrato 800",.08f,.92f,.075f,.02f,.095f,false,WARM);
        add(s,"Kodak","K · Dourado",.13f,1.08f,.12f,.015f,.045f,false,WARM);
        add(s,"Kodak","K · Paisagem 100",.19f,1.22f,.025f,0,.035f,false,CHROME);
        add(s,"Kodak","K · Cromo 100",.16f,1.07f,-.025f,0,.025f,false,NEUTRAL);
        add(s,"Kodak","K · Cromo documental",.24f,1.14f,.045f,0,.04f,false,CHROME);
        add(s,"Kodak","K · Cinema diurno",.08f,.91f,.03f,.015f,.105f,false,PASTEL);
        add(s,"Kodak","K · Prata 400",.25f,0,0,.005f,.055f,true,new float[]{.31f,.59f,.10f});
        add(s,"Kodak","K · Prata fina",.14f,0,0,0,.04f,true,new float[]{.25f,.64f,.11f});
        add(s,"Hasselblad","H · Natural",.025f,1.0f,0,0,.065f,false,NEUTRAL);
        add(s,"Hasselblad","H · Pele neutra",-.015f,.95f,.012f,.004f,.09f,false,PASTEL);
        add(s,"Hasselblad","H · Paisagem",.11f,1.10f,-.015f,0,.08f,false,NEUTRAL);
        add(s,"Hasselblad","H · Documental",.09f,.94f,0,0,.06f,false,CHROME);
        add(s,"Hasselblad","H · Luz suave",-.08f,.94f,.02f,.015f,.12f,false,PASTEL);
        add(s,"Hasselblad","H · Sombras profundas",.21f,.97f,0,0,.075f,false,NEUTRAL);
        add(s,"Hasselblad","H · Pastel",-.09f,.84f,.018f,.025f,.13f,false,PASTEL);
        add(s,"Hasselblad","H · Estúdio",.065f,1.015f,0,0,.10f,false,NEUTRAL);
        add(s,"Hasselblad","H · Prata natural",.09f,0,0,0,.08f,true,new float[]{.30f,.60f,.10f});
        add(s,"Hasselblad","H · P&B clássico",.20f,0,0,.006f,.07f,true,new float[]{.35f,.55f,.10f});
        ALL=Collections.unmodifiableList(s);
    }
    private static void add(List<Style>s,String f,String n,float c,float sat,float w,float b,float h,boolean mono,float[]m){s.add(new Style(f,n,c,sat,w,b,h,mono,m));}
    public static Style get(int index){return ALL.get(Math.max(0,Math.min(ALL.size()-1,index)));}
}
