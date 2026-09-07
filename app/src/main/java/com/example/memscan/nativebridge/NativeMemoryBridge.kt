package com.example.memscan.nativebridge

import android.util.Log

object NativeMemoryBridge {
    private const val TAG = "NativeMemoryBridge"
    private var isLoaded = false

    init {
        try {
            System.loadLibrary("memscan")
            isLoaded = true
            Log.i(TAG, "Native library 'memscan' loaded successfully.")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Could not load native library 'memscan': ${e.message}")
            isLoaded = false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading native library: ${e.message}")
            isLoaded = false
        }
    }

    fun isAvailable(): Boolean = isLoaded

    external fun isNativeSupported(): Boolean
    external fun getNativeArch(): String
    external fun readProcessMemory(pid: Int, address: Long, length: Int): ByteArray?
    external fun writeProcessMemory(pid: Int, address: Long, data: ByteArray): Boolean
}
