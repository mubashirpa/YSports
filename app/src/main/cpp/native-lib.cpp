#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_ysports_app_PrivateKeys_newsApiKey(
        JNIEnv* env,
        jobject /* this */) {
    std::string key = "5e19afc5fd374c9481de96b5676f3a05";
    return env->NewStringUTF(key.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_ysports_app_PrivateKeys_youtubeApiKey(
        JNIEnv* env,
jobject /* this */) {
std::string key = "AIzaSyBxhTZehuwMgVUdVtvI4f76FQFj-MnRPe4";
return env->NewStringUTF(key.c_str());
}