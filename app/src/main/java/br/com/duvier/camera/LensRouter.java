package br.com.duvier.camera;
import java.util.*;
/** Pure selector used by the camera engine and regression checks. */
public final class LensRouter {
    public static final class Candidate {
        public final String id;public final double ratio,priority;public final boolean raw;
        public Candidate(String id,double ratio,boolean raw,double priority){this.id=id;this.ratio=ratio;this.raw=raw;this.priority=priority;}
    }
    public static String choose(List<Candidate> candidates,double zoom,boolean raw){
        Candidate best=null;for(Candidate c:candidates){if(raw&&!c.raw||!FrameGeometry.opticalCandidate(zoom,c.ratio))continue;
            if(best==null||c.ratio>best.ratio+.02||Math.abs(c.ratio-best.ratio)<=.02&&c.priority>best.priority)best=c;
        }return best==null?null:best.id;
    }
}
