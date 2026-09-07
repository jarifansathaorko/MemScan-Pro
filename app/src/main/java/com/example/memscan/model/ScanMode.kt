package com.example.memscan.model

enum class ScanMode(val displayName: String, val requiresInput: Boolean) {
    EXACT("Exact Value", true),
    CHANGED("Value Changed", false),
    UNCHANGED("Value Unchanged", false),
    INCREASED("Value Increased", false),
    DECREASED("Value Decreased", false),
    UNKNOWN_INITIAL("Unknown Initial Value", false);

    fun isValidForNextScan(): Boolean {
        return this != UNKNOWN_INITIAL
    }

    fun isValidForFirstScan(): Boolean {
        return this == EXACT || this == UNKNOWN_INITIAL
    }
}
