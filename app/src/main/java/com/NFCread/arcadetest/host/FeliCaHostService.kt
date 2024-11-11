package com.NFCread.arcadetest.host

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.NFCread.arcadetest.models.CardData

class FeliCaHostService : HostApduService() {
    private var currentCardData: CardData? = null

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        // 저장된 카드 데이터 기반으로 응답
        return when {
            isPollingCommand(commandApdu) -> {
                createPollingResponse()
            }
            else -> {
                ByteArray(0)
            }
        }
    }

    override fun onDeactivated(reason: Int) {
        // NFC 연결이 끊어졌을 때의 처리
        currentCardData = null
    }

    private fun isPollingCommand(command: ByteArray): Boolean {
        return command.size >= 6 &&
                command[0] == 0x06.toByte() &&
                command[1] == 0x00.toByte()
    }

    private fun createPollingResponse(): ByteArray {
        val idm = currentCardData?.idm?.hexToByteArray() ?: ByteArray(8)
        val pmm = currentCardData?.pmm?.hexToByteArray() ?: ByteArray(8)

        return byteArrayOf(
            0x12.toByte(),  // 응답 길이
            0x01.toByte()   // 응답 코드
        ) + idm + pmm       // IDm과 PMm 추가
    }
}