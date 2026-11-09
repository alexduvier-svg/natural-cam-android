package br.com.duvier.camera;

/** Pure decision helpers: bounded shutter hold and finite camera recovery. */
public final class CaptureInteractionPolicy {
    private CaptureInteractionPolicy() {}

    /** One bounded AWB grace interval, never the previous five sequential 160-ms waits. */
    public static final int MAX_AWB_GRACE_MS = 110;
    public static boolean waitForAwb(int attempt, boolean automatic, boolean resultMissingOrSearching) {
        return automatic && attempt == 0 && resultMissingOrSearching;
    }

    /** Recovery may happen once, only to a previously configured state that differs. */
    public static boolean restorePrevious(boolean recoveryAlreadyAttempted, boolean hasPrevious,
                                          boolean settingsDiffer) {
        return !recoveryAlreadyAttempted && hasPrevious && settingsDiffer;
    }
}
