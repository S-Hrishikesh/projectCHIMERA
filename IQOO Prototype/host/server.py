import socket
import struct
import threading
import subprocess
import os

HOST = '0.0.0.0'
PORT = 8080

def handle_client(conn, addr):
    print(f"Connected by {addr}")
    try:
        while True:
            # Read header (8 bytes)
            header = conn.recv(8)
            if not header:
                break
            if len(header) < 8:
                break
            
            opcode, length = struct.unpack('<II', header)
            
            # Read payload
            total_data_bytes = length * 4
            data_bytes = bytearray()
            while len(data_bytes) < total_data_bytes:
                packet = conn.recv(total_data_bytes - len(data_bytes))
                if not packet:
                    break
                data_bytes.extend(packet)
            
            if len(data_bytes) < total_data_bytes:
                break

            print(f"Received workload: opcode={opcode}, length={length} floats")

            # Write data to a temporary file for the kernel to process
            thread_id = threading.get_ident()
            input_file = f"input_{thread_id}.bin"
            output_file = f"output_{thread_id}.bin"
            
            with open(input_file, "wb") as f:
                f.write(data_bytes)
            
            # Run the runner script
            print("Dispatching to RISC-V runner...")
            subprocess.run(["python3", "runner.py", input_file, output_file, str(length)], check=True)
            
            # Read the result
            with open(output_file, "rb") as f:
                result_data = f.read()
            
            # Send back the same header + result_data
            conn.sendall(header + result_data)
            print("Sent result back to client.")
            
            # Cleanup temp files
            if os.path.exists(input_file): os.remove(input_file)
            if os.path.exists(output_file): os.remove(output_file)
            
    except Exception as e:
        print(f"Error handling client {addr}: {e}")
    finally:
        conn.close()

def start_server():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        print(f"RISC-V Co-Processor Server listening on {HOST}:{PORT}")
        while True:
            conn, addr = s.accept()
            thread = threading.Thread(target=handle_client, args=(conn, addr))
            thread.start()

if __name__ == "__main__":
    start_server()
