CC = riscv64-linux-gnu-gcc
CFLAGS = -O3 -march=rv64gcv -mabi=lp64d

all: rvv_kernel.elf

rvv_kernel.elf: rvv_kernel.c
	$(CC) $(CFLAGS) -o $@ $<

clean:
	rm -f rvv_kernel.elf input*.bin output*.bin
