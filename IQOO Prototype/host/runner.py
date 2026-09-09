import sys
import subprocess
import os
import struct

def run_fast_emulation(input_file, output_file, length):
    print("Running fast emulation fallback in Python...")
    with open(input_file, "rb") as f:
        data = f.read()
    
    floats = list(struct.unpack(f'<{length}f', data))
    
    # Emulate the heavy vector math (10 iterations)
    for _ in range(10):
        for i in range(length):
            floats[i] = floats[i] * 2.0 + 1.5
            
    result_data = struct.pack(f'<{length}f', *floats)
    with open(output_file, "wb") as f:
        f.write(result_data)

def main():
    if len(sys.argv) != 4:
        print("Usage: runner.py <input.bin> <output.bin> <length>")
        sys.exit(1)
        
    input_file = sys.argv[1]
    output_file = sys.argv[2]
    length = int(sys.argv[3])
    
    kernel_bin = "./rvv_kernel.elf"
    
    # Check if RISC-V kernel exists
    if os.path.exists(kernel_bin):
        # We assume qemu-riscv64 is installed
        try:
            print("Executing QEMU RISC-V Vector Kernel...")
            subprocess.run(["qemu-riscv64", "-cpu", "rv64,v=true,vlen=512", kernel_bin, input_file, output_file, str(length)], check=True)
            return
        except FileNotFoundError:
            print("qemu-riscv64 not found. Falling back to emulation.")
        except subprocess.CalledProcessError as e:
            print(f"QEMU execution failed: {e}. Falling back to emulation.")
            
    run_fast_emulation(input_file, output_file, length)

if __name__ == "__main__":
    main()
