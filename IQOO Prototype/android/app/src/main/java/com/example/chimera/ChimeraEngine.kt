package com.example.chimera

class ChimeraEngine {
    companion object {
        init {
            System.loadLibrary("chimera")
        }
    }

    /**
     * Executes the heavy ARM NEON vector operations.
     * @param data The array to process in-place.
     * @return Execution time in microseconds.
     */
    external fun neonComputeKernel(data: FloatArray): Long

    /**
     * Serializes the float array into a byte buffer with an opcode and length header.
     * @param opcode Int representing the operation.
     * @param data The float array to send.
     * @return Serialized byte array.
     */
    external fun serializePayload(opcode: Int, data: FloatArray): ByteArray

    /**
     * Deserializes the result buffer back into the float array.
     * @param buffer The received byte array from the TCP socket.
     * @param outputData The pre-allocated float array to populate.
     */
    external fun deserializePayload(buffer: ByteArray, outputData: FloatArray)
}
