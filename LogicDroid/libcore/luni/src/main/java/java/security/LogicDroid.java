package java.security;

public final class LogicDroid
{
	public static native int initializeMonitor(int[] UID);
	public static native int modifyStaticVariable(int policyID, int UID, boolean value, int rel);
	public static native int checkEvent(int policyID, int caller, int target);
	public static native String getRelationName(int ID);
	public static native boolean isMonitorPresent();
}
