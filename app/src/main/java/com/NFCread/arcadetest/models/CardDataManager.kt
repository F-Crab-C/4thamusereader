package com.NFCread.arcadetest.models

import android.content.Context

class CardDataManager(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences(
        "card_data",
        Context.MODE_PRIVATE
    )

    fun isCardExists(idm: String): Boolean {
        val cards = getCards()
        return cards.any { it.idm == idm }
    }

    fun saveCard(cardData: CardData) {
        val cardJson = """
            {
                "name": "${cardData.name}",
                "idm": "${cardData.idm}",
                "pmm": "${cardData.pmm}",
                "systemCode": "${cardData.systemCode}",
                "timestamp": ${cardData.timestamp}
            }
        """.trimIndent()

        val cards = getCards().toMutableList()
        cards.add(cardJson)

        sharedPreferences.edit()
            .putStringSet("saved_cards", cards.toSet())
            .apply()
    }

    fun getCards(): List<CardData> {
        val cardStrings = sharedPreferences.getStringSet("saved_cards", setOf()) ?: setOf()
        return cardStrings.map { parseCardData(it) }
    }

    private fun parseCardData(jsonString: String): CardData {
        // JSON 파싱을 위한 정규식
        val namePattern = "\"name\":\\s*\"([^\"]+)\"".toRegex()
        val idmPattern = "\"idm\":\\s*\"([^\"]+)\"".toRegex()
        val pmmPattern = "\"pmm\":\\s*\"([^\"]+)\"".toRegex()
        val systemCodePattern = "\"systemCode\":\\s*\"([^\"]+)\"".toRegex()

        return CardData(
            name = namePattern.find(jsonString)?.groupValues?.get(1) ?: "",
            idm = idmPattern.find(jsonString)?.groupValues?.get(1) ?: "",
            pmm = pmmPattern.find(jsonString)?.groupValues?.get(1) ?: "",
            systemCode = systemCodePattern.find(jsonString)?.groupValues?.get(1) ?: ""
        )
    }
}