package com.NFCread.arcadetest.models

data class CardData(
    val idm: String,
    val pmm: String,
    val systemCode: String,
    val timestamp: Long = System.currentTimeMillis()
)
