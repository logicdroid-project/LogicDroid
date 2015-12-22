#define LOG_TAG "LogicDroid"

#include "JNIHelp.h"
#include "JniConstants.h"
#include "JniException.h"

#include <unistd.h>

#include <sys/syscall.h>

/*
  For emulator :
  - 361 is the syscall number for check event
  - 362 is the syscall number for init monitor
  - 363 is the syscall number for change predicate
*/

static jint LogicDroid_checkEvent(JNIEnv*, jobject, jint policyID, jint caller, jint target)
{
	return (jint)syscall(376, policyID, caller, target);
	//return (jint)syscall(361, policyID, caller, target);
}

static jint LogicDroid_initializeMonitor(JNIEnv* mEnv, jobject, jintArray UID) {
	jsize len = mEnv->GetArrayLength(UID);
	jint *bufferUID = mEnv->GetIntArrayElements(UID, 0);
	int argUID[len];
	int i;
	for (i = 0; i < len; i++)
	{
		argUID[i] = (int)bufferUID[i];
	}
	int policyID = syscall(377, argUID, len);
	//int policyID = syscall(362, argUID, len);
	mEnv->ReleaseIntArrayElements(UID, bufferUID, 0);
        return (jint)policyID;
}

// In this case, we know that there will only be one variable
static jint LogicDroid_modifyStaticVariable(JNIEnv*, jobject, jint policyID, jint UID, jboolean value, jint rel) {
	return (jint)syscall(378, policyID, rel, (int)UID, (char)value);
	//return (jint)syscall(363, policyID, rel, (int)UID, (char)value);
}

static jstring LogicDroid_getRelationName(JNIEnv* mEnv, jobject, jint ID)
{
	char temp[25];
	int res = syscall(379, ID, temp);
	//int res = syscall(364, ID, temp);
	if (res > 0) return mEnv->NewStringUTF(temp);
	return mEnv->NewStringUTF("OUTSIDE-BOUND");
}

static jboolean LogicDroid_isMonitorPresent(JNIEnv*, jobject)
{
	return (jboolean)(syscall(380) == 1);
	//return (jboolean)(syscall(365) == 1);
}

static JNINativeMethod gMethods[] = {
	NATIVE_METHOD(LogicDroid, initializeMonitor, "([I)I"),
	NATIVE_METHOD(LogicDroid, modifyStaticVariable, "(IIZI)I"),
	NATIVE_METHOD(LogicDroid, checkEvent, "(III)I"),
	NATIVE_METHOD(LogicDroid, getRelationName, "(I)Ljava/lang/String;"),
	NATIVE_METHOD(LogicDroid, isMonitorPresent, "()Z")
};

void register_java_security_LogicDroid(JNIEnv* env) {
    jniRegisterNativeMethods(env, "java/security/LogicDroid", gMethods, NELEM(gMethods));
}
