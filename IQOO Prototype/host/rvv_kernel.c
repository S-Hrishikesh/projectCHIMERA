#include <stdio.h>
#include <stdlib.h>
#include <riscv_vector.h>

void vector_compute(float *data, size_t n) {
    // 10 iterations of: data[i] = data[i] * 2.0f + 1.5f;
    for (int iter = 0; iter < 10; ++iter) {
        size_t vl;
        float *ptr = data;
        size_t avl = n;
        
        while (avl > 0) {
            // Set vector length based on available elements
            vl = __riscv_vsetvl_e32m8(avl);
            
            // Load vector
            vfloat32m8_t v_data = __riscv_vle32_v_f32m8(ptr, vl);
            
            // Multiply by 2.0
            v_data = __riscv_vfmul_vf_f32m8(v_data, 2.0f, vl);
            
            // Add 1.5
            v_data = __riscv_vfadd_vf_f32m8(v_data, 1.5f, vl);
            
            // Store vector
            __riscv_vse32_v_f32m8(ptr, v_data, vl);
            
            ptr += vl;
            avl -= vl;
        }
    }
}

int main(int argc, char **argv) {
    if (argc != 4) {
        fprintf(stderr, "Usage: %s <input_bin> <output_bin> <length>\n", argv[0]);
        return 1;
    }
    
    const char *input_file = argv[1];
    const char *output_file = argv[2];
    size_t length = atoll(argv[3]);
    
    FILE *in = fopen(input_file, "rb");
    if (!in) {
        perror("Failed to open input");
        return 1;
    }
    
    float *data = (float *)malloc(length * sizeof(float));
    if (!data) return 1;
    fread(data, sizeof(float), length, in);
    fclose(in);
    
    vector_compute(data, length);
    
    FILE *out = fopen(output_file, "wb");
    if (!out) {
        perror("Failed to open output");
        free(data);
        return 1;
    }
    
    fwrite(data, sizeof(float), length, out);
    fclose(out);
    
    free(data);
    return 0;
}
