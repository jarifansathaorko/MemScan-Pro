package com.example.memorylab

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * MemoryLabTarget represents a controlled in-memory target application.
 * It stores actual primitive variables at designated simulated virtual addresses
 * for memory scanning, filtering, and editing verification.
 */
object MemoryLabTarget {
    const val PID = 10542
    const val PACKAGE_NAME = "com.example.memorylab"
    const val PROCESS_NAME = "MemoryLab Target"

    // Designated memory addresses (aligned 32-bit and 64-bit boundaries)
    const val ADDR_VIEWS = 0x7B42A108L        // Int32
    const val ADDR_SUBSCRIBERS = 0x7B42B44CL  // Int32
    const val ADDR_REVENUE = 0x7B42C010L      // Float32
    const val ADDR_WATCH_TIME = 0x7B42C020L   // Double64

    // Initial constant specifications
    const val INITIAL_VIEWS = 12450
    const val INITIAL_SUBSCRIBERS = 427
    const val INITIAL_REVENUE = 31.42f
    const val INITIAL_WATCH_TIME = 183.7

    // Actual runtime variables
    private val _views = MutableStateFlow(INITIAL_VIEWS)
    val views: StateFlow<Int> = _views.asStateFlow()

    private val _subscribers = MutableStateFlow(INITIAL_SUBSCRIBERS)
    val subscribers: StateFlow<Int> = _subscribers.asStateFlow()

    private val _revenue = MutableStateFlow(INITIAL_REVENUE)
    val revenue: StateFlow<Float> = _revenue.asStateFlow()

    private val _watchTime = MutableStateFlow(INITIAL_WATCH_TIME)
    val watchTime: StateFlow<Double> = _watchTime.asStateFlow()

    // Mutation helpers
    fun incrementViews(amount: Int = 1) {
        _views.value += amount
    }

    fun incrementSubscribers(amount: Int = 1) {
        _subscribers.value += amount
    }

    fun incrementRevenue(amount: Float = 1.0f) {
        _revenue.value = String.format(java.util.Locale.US, "%.2f", _revenue.value + amount).toFloat()
    }

    fun incrementWatchTime(amount: Double = 1.0) {
        _watchTime.value = String.format(java.util.Locale.US, "%.1f", _watchTime.value + amount).toDouble()
    }

    fun setViews(value: Int) {
        _views.value = value
    }

    fun setSubscribers(value: Int) {
        _subscribers.value = value
    }

    fun setRevenue(value: Float) {
        _revenue.value = value
    }

    fun setWatchTime(value: Double) {
        _watchTime.value = value
    }

    fun reset() {
        _views.value = INITIAL_VIEWS
        _subscribers.value = INITIAL_SUBSCRIBERS
        _revenue.value = INITIAL_REVENUE
        _watchTime.value = INITIAL_WATCH_TIME
    }

    // Byte serialization helpers for memory read/write synchronization
    fun getViewsBytes(): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(_views.value).array()

    fun getSubscribersBytes(): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(_subscribers.value).array()

    fun getRevenueBytes(): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(_revenue.value).array()

    fun getWatchTimeBytes(): ByteArray =
        ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(_watchTime.value).array()

    fun updateFromMemoryWrite(address: Long, bytes: ByteArray) {
        if (address == ADDR_VIEWS && bytes.size >= 4) {
            val v = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int
            _views.value = v
        } else if (address == ADDR_SUBSCRIBERS && bytes.size >= 4) {
            val s = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int
            _subscribers.value = s
        } else if (address == ADDR_REVENUE && bytes.size >= 4) {
            val r = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).float
            _revenue.value = r
        } else if (address == ADDR_WATCH_TIME && bytes.size >= 8) {
            val w = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).double
            _watchTime.value = w
        }
    }
}
