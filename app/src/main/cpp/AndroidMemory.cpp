#include <jni.h>

#include <android/log.h>

#include <cerrno>
#include <cstdio>
#include <cstdlib>
#include <cstring>

#include <string>

#define LOG_TAG "AndroidMemory"

#define LOGI(...) \
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#define LOGW(...) \
    __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

static bool g_enabled = false;

static unsigned long long readMemInfo(
        const char *name)
{
    FILE *file = fopen(
            "/proc/meminfo",
            "r");

    if (!file) {
        LOGW(
                "Não foi possível abrir /proc/meminfo: %s",
                strerror(errno));

        return 0;
    }

    char line[256];

    while (fgets(line, sizeof(line), file)) {

        unsigned long long value = 0;

        if (sscanf(
                line,
                "%*[^:]: %llu kB",
                &value) == 1) {

            if (strncmp(
                    line,
                    name,
                    strlen(name)) == 0) {

                fclose(file);

                /*
                 * /proc/meminfo usa KiB.
                 * O JNI retorna bytes.
                 */
                return value * 1024ULL;
            }
        }
    }

    fclose(file);

    return 0;
}

static unsigned long long getMemTotal()
{
    return readMemInfo("MemTotal");
}

static unsigned long long getMemAvailable()
{
    unsigned long long available =
            readMemInfo("MemAvailable");

    if (available != 0) {
        return available;
    }

    /*
     * Fallback para kernels Android antigos.
     */
    unsigned long long free =
            readMemInfo("MemFree");

    unsigned long long buffers =
            readMemInfo("Buffers");

    unsigned long long cached =
            readMemInfo("Cached");

    return free + buffers + cached;
}

static unsigned long long getSwapTotal()
{
    return readMemInfo("SwapTotal");
}

static unsigned long long getSwapFree()
{
    return readMemInfo("SwapFree");
}

static void setEnvironmentValue(
        const char *name,
        unsigned long long value)
{
    char buffer[64];

    snprintf(
            buffer,
            sizeof(buffer),
            "%llu",
            value);

    setenv(
            name,
            buffer,
            1);
}

static void applyMemoryEnvironment()
{
    const unsigned long long physical =
            getMemTotal();

    const unsigned long long available =
            getMemAvailable();

    const unsigned long long swap =
            getSwapTotal();

    const unsigned long long swapFree =
            getSwapFree();

    unsigned long long reportedTotal =
            physical;

    unsigned long long reportedAvailable =
            available;

    if (g_enabled) {

        reportedTotal =
                physical + swap;

        reportedAvailable =
                available + swapFree;
    }

    setEnvironmentValue(
            "WINLATOR_PHYSICAL_RAM",
            physical);

    setEnvironmentValue(
            "WINLATOR_AVAILABLE_RAM",
            available);

    setEnvironmentValue(
            "WINLATOR_SWAP_TOTAL",
            swap);

    setEnvironmentValue(
            "WINLATOR_SWAP_FREE",
            swapFree);

    setEnvironmentValue(
            "WINLATOR_REPORTED_RAM",
            reportedTotal);

    setEnvironmentValue(
            "WINLATOR_REPORTED_AVAILABLE",
            reportedAvailable);

    setenv(
            "WINLATOR_MEMORY_MODE",
            g_enabled
                    ? "ram+swap"
                    : "physical",
            1);

    LOGI(
            "Physical=%llu MB Swap=%llu MB Reported=%llu MB",
            physical / 1024 / 1024,
            swap / 1024 / 1024,
            reportedTotal / 1024 / 1024);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_winlator_core_MemoryManager_nativeGetPhysicalRam(
        JNIEnv *,
        jclass)
{
    return static_cast<jlong>(
            getMemTotal());
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_winlator_core_MemoryManager_nativeGetAvailableRam(
        JNIEnv *,
        jclass)
{
    return static_cast<jlong>(
            getMemAvailable());
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_winlator_core_MemoryManager_nativeGetSwapTotal(
        JNIEnv *,
        jclass)
{
    return static_cast<jlong>(
            getSwapTotal());
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_winlator_core_MemoryManager_nativeGetSwapFree(
        JNIEnv *,
        jclass)
{
    return static_cast<jlong>(
            getSwapFree());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_winlator_core_MemoryManager_nativeSetEnabled(
        JNIEnv *,
        jclass,
        jboolean enabled)
{
    g_enabled = enabled == JNI_TRUE;

    applyMemoryEnvironment();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_winlator_core_MemoryManager_nativeApply(
        JNIEnv *,
        jclass)
{
    applyMemoryEnvironment();

    return JNI_TRUE;
}
