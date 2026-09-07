#include <jni.h>
#include <string>
#include <vector>
#include <sys/uio.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <android/log.h>

#define TAG "MemScanNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_example_memscan_nativebridge_NativeMemoryBridge_isNativeSupported(
        JNIEnv *env,
        jobject /* this */) {
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_example_memscan_nativebridge_NativeMemoryBridge_getNativeArch(
        JNIEnv *env,
        jobject /* this */) {
#if defined(__aarch64__)
    return env->NewStringUTF("arm64-v8a");
#elif defined(__arm__)
    return env->NewStringUTF("armeabi-v7a");
#elif defined(__x86_64__)
    return env->NewStringUTF("x86_64");
#elif defined(__i386__)
    return env->NewStringUTF("x86");
#else
    return env->NewStringUTF("unknown");
#endif
}

JNIEXPORT jbyteArray JNICALL
Java_com_example_memscan_nativebridge_NativeMemoryBridge_readProcessMemory(
        JNIEnv *env,
        jobject /* this */,
        jint pid,
        jlong address,
        jint length) {
    if (length <= 0 || length > 1024 * 1024 * 16) { // limit single read to 16MB for safety
        return nullptr;
    }

    std::vector<uint8_t> buffer(length);

    // Try process_vm_readv first (fastest, requires ptrace or root/debuggable permissions)
    struct iovec local_iov;
    local_iov.iov_base = buffer.data();
    local_iov.iov_len = length;

    struct iovec remote_iov;
    remote_iov.iov_base = reinterpret_cast<void *>(static_cast<uintptr_t>(address));
    remote_iov.iov_len = length;

    ssize_t nread = process_vm_readv(pid, &local_iov, 1, &remote_iov, 1, 0);

    if (nread < 0) {
        // Fallback: try opening /proc/<pid>/mem
        char path[64];
        snprintf(path, sizeof(path), "/proc/%d/mem", pid);
        int fd = open(path, O_RDONLY);
        if (fd >= 0) {
            nread = pread64(fd, buffer.data(), length, static_cast<off64_t>(address));
            close(fd);
        }
    }

    if (nread <= 0) {
        return nullptr;
    }

    jbyteArray result = env->NewByteArray(nread);
    if (result != nullptr) {
        env->SetByteArrayRegion(result, 0, nread, reinterpret_cast<const jbyte *>(buffer.data()));
    }
    return result;
}

JNIEXPORT jboolean JNICALL
Java_com_example_memscan_nativebridge_NativeMemoryBridge_writeProcessMemory(
        JNIEnv *env,
        jobject /* this */,
        jint pid,
        jlong address,
        jbyteArray data) {
    if (data == nullptr) {
        return JNI_FALSE;
    }

    jsize length = env->GetArrayLength(data);
    if (length <= 0) {
        return JNI_FALSE;
    }

    std::vector<uint8_t> buffer(length);
    env->GetByteArrayRegion(data, 0, length, reinterpret_cast<jbyte *>(buffer.data()));

    // Try process_vm_writev
    struct iovec local_iov;
    local_iov.iov_base = buffer.data();
    local_iov.iov_len = length;

    struct iovec remote_iov;
    remote_iov.iov_base = reinterpret_cast<void *>(static_cast<uintptr_t>(address));
    remote_iov.iov_len = length;

    ssize_t nwritten = process_vm_writev(pid, &local_iov, 1, &remote_iov, 1, 0);

    if (nwritten < 0) {
        // Fallback: try /proc/<pid>/mem
        char path[64];
        snprintf(path, sizeof(path), "/proc/%d/mem", pid);
        int fd = open(path, O_RDWR);
        if (fd >= 0) {
            nwritten = pwrite64(fd, buffer.data(), length, static_cast<off64_t>(address));
            close(fd);
        }
    }

    return (nwritten == length) ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
