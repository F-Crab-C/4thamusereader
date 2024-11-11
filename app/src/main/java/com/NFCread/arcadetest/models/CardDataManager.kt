package com.NFCread.arcadetest.models

import android.content.Context

class CardDataManager(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences(
        "card_data",
        Context.MODE_PRIVATE
    )

    fun saveCard(cardData: CardData) {
        val cardJson = """
            {
                "idm": "${cardData.idm}",
                "pmm": "${cardData.pmm}",
                "systemCode": "${cardData.systemCode}",
                "timestamp": ${cardData.timestamp}
            }
        """.trimIndent()

        val cards = getCards().toMutableList()
        cards.add(cardJson)

        sharedPreferences.edit().putStringSet("saved_cards", cards.toSet()).apply()
    }

    fun getCards(): List<String> {
        return sharedPreferences.getStringSet("saved_cards", setOf())?.toList() ?: emptyList()
    }
}