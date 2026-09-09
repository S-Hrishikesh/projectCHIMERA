package com.example.chimera

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class NetworkClient(private val host: String, private val port: Int) {
    
    suspend fun executeRemoteVectorWorkload(payload: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            socket = Socket(host, port)
            val outputStream: OutputStream = socket.getOutputStream()
            val inputStream: InputStream = socket.getInputStream()
            
            // Send payload
            outputStream.write(payload)
            outputStream.flush()
            
            // Read response. The response format matches the payload: [opcode][len][data...]
            // We read the first 8 bytes to determine total length
            val header = ByteArray(8)
            var bytesRead = 0
            while (bytesRead < 8) {
                val read = inputStream.read(header, bytesRead, 8 - bytesRead)
                if (read == -1) throw Exception("Socket closed prematurely")
                bytesRead += read
            }
            
            // Reconstruct length (bytes 4-7, little endian)
            val len = (header[4].toInt() and 0xFF) or
                      ((header[5].toInt() and 0xFF) shl 8) or
                      ((header[6].toInt() and 0xFF) shl 16) or
                      ((header[7].toInt() and 0xFF) shl 24)
            
            val totalBytes = 8 + len * 4
            val resultBuffer = ByteArray(totalBytes)
            System.arraycopy(header, 0, resultBuffer, 0, 8)
            
            bytesRead = 8
            while (bytesRead < totalBytes) {
                val read = inputStream.read(resultBuffer, bytesRead, totalBytes - bytesRead)
                if (read == -1) throw Exception("Socket closed prematurely while reading data")
                bytesRead += read
            }
            
            resultBuffer
        } finally {
            socket?.close()
        }
    }
}
