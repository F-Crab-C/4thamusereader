package com.NFCread.arcadetest.models

data class CardData(
    val name: String,        // 사용자 지정 이름
    val idm: String,
    val pmm: String,
    val systemCode: String,
    val timestamp: Long = System.currentTimeMillis()
)
