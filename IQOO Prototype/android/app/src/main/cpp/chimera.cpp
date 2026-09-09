#include <jni.h>
#include <arm_neon.h>
#include <time.h>
#include <string.h>
#include <vector>

// Helper to get time in microseconds
long long get_time_us() {
    struct timespec t;
    clock_gettime(CLOCK_MONOTONIC, &t);
    return t.tv_sec * 1000000LL + t.tv_nsec / 1000LL;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_chimera_ChimeraEngine_neonComputeKernel(JNIEnv *env, jobject thiz, jfloatArray data) {
    jsize len = env->GetArrayLength(data);
    jfloat *ptr = env->GetFloatArrayElements(data, nullptr);

    long long start_time = get_time_us();

    // Heavy floating-point vector math (A = A * 2.0f + 1.5f)
    // Process 4 floats at a time (128-bit NEON)
    float32x4_t v_multiplier = vdupq_n_f32(2.0f);
    float32x4_t v_addend = vdupq_n_f32(1.5f);

    // To simulate a "heavy" loop, we run multiple iterations over the data
    int num_iterations = 10;
    for (int iter = 0; iter < num_iterations; ++iter) {
        int i = 0;
        for (; i <= len - 4; i += 4) {
            float32x4_t v_data = vld1q_f32(ptr + i);
            v_data = vmulq_f32(v_data, v_multiplier);
            v_data = vaddq_f32(v_data, v_addend);
            vst1q_f32(ptr + i, v_data);
        }
        // Handle remainder if len is not a multiple of 4
        for (; i < len; ++i) {
            ptr[i] = ptr[i] * 2.0f + 1.5f;
        }
    }

    long long end_time = get_time_us();

    env->ReleaseFloatArrayElements(data, ptr, 0); // 0 means copy back the changes

    return (end_time - start_time); // Return execution time in microseconds
}

// Serialization Utility
// Packs: [Opcode (4 bytes)] [Length (4 bytes)] [Float Array Data...]
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_chimera_ChimeraEngine_serializePayload(JNIEnv *env, jobject thiz, jint opcode, jfloatArray data) {
    jsize len = env->GetArrayLength(data);
    jfloat *ptr = env->GetFloatArrayElements(data, nullptr);

    // Calculate total size: 4 bytes (opcode) + 4 bytes (len) + len * 4 bytes (floats)
    int total_bytes = 4 + 4 + (len * sizeof(float));

    jbyteArray result = env->NewByteArray(total_bytes);
    jbyte *result_ptr = env->GetByteArrayElements(result, nullptr);

    // Pack Opcode
    memcpy(result_ptr, &opcode, 4);
    // Pack Length
    memcpy(result_ptr + 4, &len, 4);
    // Pack Data
    memcpy(result_ptr + 8, ptr, len * sizeof(float));

    env->ReleaseByteArrayElements(result, result_ptr, 0);
    env->ReleaseFloatArrayElements(data, ptr, JNI_ABORT); // JNI_ABORT: do not copy back changes to java side

    return result;
}

// Deserialization Utility
// Unpacks: byte array back to float array, ignoring the 8 byte header (opcode + len)
extern "C" JNIEXPORT void JNICALL
Java_com_example_chimera_ChimeraEngine_deserializePayload(JNIEnv *env, jobject thiz, jbyteArray buffer, jfloatArray output_data) {
    jbyte *buf_ptr = env->GetByteArrayElements(buffer, nullptr);
    jfloat *out_ptr = env->GetFloatArrayElements(output_data, nullptr);
    jsize out_len = env->GetArrayLength(output_data);

    // Extract length from header to verify
    int payload_len = 0;
    memcpy(&payload_len, buf_ptr + 4, 4);

    if (payload_len == out_len) {
        memcpy(out_ptr, buf_ptr + 8, payload_len * sizeof(float));
    }

    env->ReleaseFloatArrayElements(output_data, out_ptr, 0); // 0 means copy back
    env->ReleaseByteArrayElements(buffer, buf_ptr, JNI_ABORT);
}
