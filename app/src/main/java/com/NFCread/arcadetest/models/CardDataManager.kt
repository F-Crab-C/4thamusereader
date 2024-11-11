import android.content.Context
import com.NFCread.arcadetest.models.CardData

class CardDataManager(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences(
        "card_data",
        Context.MODE_PRIVATE
    )

    fun saveCard(cardData: CardData) {
        // 현재 저장된 카드 목록 가져오기
        val savedCards = sharedPreferences.getStringSet("saved_cards", mutableSetOf()) ?: mutableSetOf()
        val cardsList = savedCards.toMutableList()

        // 새 카드 데이터를 JSON 형식 문자열로 변환
        val newCardJson = """
            {
                "name": "${cardData.name}",
                "idm": "${cardData.idm}",
                "pmm": "${cardData.pmm}",
                "systemCode": "${cardData.systemCode}",
                "timestamp": ${cardData.timestamp}
            }
        """.trimIndent()

        // 카드 추가
        cardsList.add(newCardJson)

        // 저장
        sharedPreferences.edit()
            .putStringSet("saved_cards", cardsList.toSet())
            .apply()
    }

    fun getCards(): List<CardData> {
        val savedCards = sharedPreferences.getStringSet("saved_cards", setOf()) ?: setOf()
        return savedCards.map { parseCardData(it) }
    }

    fun isCardExists(idm: String): Boolean {
        return getCards().any { it.idm == idm }
    }

    private fun parseCardData(jsonString: String): CardData {
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