package br.com.duvier.camera;
import org.json.JSONObject;
/** Hysteresis prevents AE compensation from oscillating after its own highlight correction. */
public final class ScenePolicy {
    private int applied=0;private double median=-1,chromaR=0,chromaG=0,edges=0;
    public void reset(){applied=0;median=-1;}
    public int update(JSONObject scene,double step,boolean userPriority,boolean enabled){
        if(scene==null||userPriority||!enabled||step<=0){reset();return 0;}
        double m=scene.optDouble("median_luma",.5),corrected=m*Math.pow(2,-applied*step/2.2),mean=scene.optDouble("mean_luma",.5),edge=scene.optDouble("edge_energy",0)/(mean*255+1);
        org.json.JSONArray rgb=scene.optJSONArray("rgb_means");double r=rgb==null?1:rgb.optDouble(0,1),g=rgb==null?1:rgb.optDouble(1,1),b=rgb==null?1:rgb.optDouble(2,1),total=Math.max(1,r+g+b);r/=total;g/=total;
        boolean changed=median<0||Math.abs(Math.log((corrected+.02)/(median+.02))/Math.log(2))>.85||Math.abs(r-chromaR)+Math.abs(g-chromaG)>.16||Math.abs(edge-edges)>.8*Math.max(.2,edges);
        int suggested=SceneIntelligence.highlightProtectionSteps(scene,step,false);
        if(changed){applied=suggested;median=corrected;chromaR=r;chromaG=g;edges=edge;}else if(suggested<applied)applied=suggested;
        if(m<.08)applied=0;
        return applied;
    }
}
