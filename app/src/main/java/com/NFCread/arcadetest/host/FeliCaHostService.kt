package com.NFCread.arcadetest.host

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.NFCread.arcadetest.models.CardData

class FeliCaHostService : HostApduService() {
    private var currentCardData: CardData? = null

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        return when {
            isPollingCommand(commandApdu) -> createPollingResponse()
            else -> ByteArray(0)
        }
    }

    override fun onDeactivated(reason: Int) {
        currentCardData = null
    }

    private fun isPollingCommand(command: ByteArray): Boolean {
        return command.size >= 6 &&
                command[0] == 0x06.toByte() &&
                command[1] == 0x00.toByte()
    }

    private fun createPollingResponse(): ByteArray {
        // 기본 응답 헤더
        val responseHeader = byteArrayOf(
            0x12.toByte(),  // 응답 길이
            0x01.toByte()   // 응답 코드
        )

        // IDm과 PMm을 바이트 배열로 직접 변환
        val idmBytes = ByteArray(8) { 0xFF.toByte() }  // 예시 IDm
        val pmmBytes = ByteArray(8) { 0xFF.toByte() }  // 예시 PMm

        // 응답 조합
        return responseHeader + idmBytes + pmmBytes
    }
}