package com.example.memscan.model

data class ProfileVariable(
    val label: String,
    val targetValueStr: String,
    val dataType: DataType,
    val preferredStrategy: ScanStrategy = ScanStrategy.NUMERIC_EXACT
)

data class FreezeProfile(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val targetPackage: String,
    val variables: List<ProfileVariable>,
    val createdAt: Long = System.currentTimeMillis()
)
