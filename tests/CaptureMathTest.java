import br.com.duvier.camera.CaptureMath;
public class CaptureMathTest {
    static void require(boolean v, String msg) { if(!v) throw new AssertionError(msg); }
    public static void main(String[] args) {
        require(CaptureMath.jpegOrientation(90,90,false)==180,"back landscape");
        require(CaptureMath.jpegOrientation(90,90,true)==0,"front landscape");
        require(CaptureMath.jpegOrientation(270,270,false)==180,"rotation wrap");
        require(CaptureMath.exifOrientation(90)==6,"DNG orientation");
        require(CaptureMath.pixelsAfterCrop(50_000_000,2)==12_500_000,"7x does not create native pixels");
        require(CaptureMath.experimentalBokehStrength(1.4)==100,"wide aperture");
        require(CaptureMath.experimentalBokehStrength(16)==0,"small aperture");
        require(CaptureMath.experimentalBokehStrength(4)>CaptureMath.experimentalBokehStrength(5.6),"blur decreases as f increases");
        System.out.println("8 capture geometry checks passed");
    }
}
