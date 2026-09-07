package com.example.memorylab

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

class MemoryLabViewModel : ViewModel() {
    val views: StateFlow<Int> = MemoryLabTarget.views
    val subscribers: StateFlow<Int> = MemoryLabTarget.subscribers
    val revenue: StateFlow<Float> = MemoryLabTarget.revenue
    val watchTime: StateFlow<Double> = MemoryLabTarget.watchTime

    fun incrementViews(amount: Int = 1) {
        MemoryLabTarget.incrementViews(amount)
    }

    fun incrementSubscribers(amount: Int = 1) {
        MemoryLabTarget.incrementSubscribers(amount)
    }

    fun incrementRevenue(amount: Float = 1.0f) {
        MemoryLabTarget.incrementRevenue(amount)
    }

    fun incrementWatchTime(amount: Double = 1.0) {
        MemoryLabTarget.incrementWatchTime(amount)
    }

    fun reset() {
        MemoryLabTarget.reset()
    }
}
