import br.com.duvier.camera.CaptureInteractionPolicy;

public final class CaptureInteractionPolicyTest {
    private static int checks;
    static void verify(boolean condition, String message){checks++;if(!condition)throw new AssertionError(message);}
    public static void main(String[] args){
        verify(CaptureInteractionPolicy.MAX_AWB_GRACE_MS>0&&CaptureInteractionPolicy.MAX_AWB_GRACE_MS<=120,"AWB grace strictly bounded");
        verify(CaptureInteractionPolicy.waitForAwb(0,true,true),"first unsettled auto shot gets one grace");
        verify(!CaptureInteractionPolicy.waitForAwb(1,true,true),"second attempt must capture, not delay again");
        verify(!CaptureInteractionPolicy.waitForAwb(0,true,false),"settled white balance captures immediately");
        verify(!CaptureInteractionPolicy.waitForAwb(0,false,true),"manual exposure never incurs automatic AWB delay");
        for(boolean recovery:new boolean[]{false,true})for(boolean prev:new boolean[]{false,true})for(boolean different:new boolean[]{false,true}){
            boolean answer=CaptureInteractionPolicy.restorePrevious(recovery,prev,different);
            verify(answer==(!recovery&&prev&&different),"exact one-time restore decision");
        }
        verify(!CaptureInteractionPolicy.restorePrevious(true,true,true),"failed restoration cannot recur");
        System.out.println(checks+" captura/recuperação checks passed");
    }
}
