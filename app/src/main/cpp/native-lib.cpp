#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_ysports_app_PrivateKeys_matchesUrlPath(
        JNIEnv* env,
        jobject /* this */) {
    std::string key = "831085549ee2af13a198";
    return env->NewStringUTF(key.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_ysports_app_PrivateKeys_leaguesUrlPath(
        JNIEnv* env,
jobject /* this */) {
std::string key = "ef26b2579a1e6fba29fe";
return env->NewStringUTF(key.c_str());
}