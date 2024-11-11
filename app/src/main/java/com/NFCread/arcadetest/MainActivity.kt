package com.NFCread.arcadetest

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.provider.Settings

import android.content.IntentFilter
import android.nfc.Tag
import android.nfc.tech.NfcF

class MainActivity : AppCompatActivity() {
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var btnScan: Button
    private lateinit var tvStatus: TextView
    private var pendingIntent: PendingIntent? = null
    private val nfcFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 초기화
        btnScan = findViewById(R.id.btnScan)
        tvStatus = findViewById(R.id.tvStatus)

        // NFC 어댑터 초기화
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // PendingIntent 초기화 (Android 12 이상 대응)
        pendingIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                this, 0,
                Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getActivity(
                this, 0,
                Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        // NFC 지원 확인
        if (nfcAdapter == null) {
            tvStatus.text = "이 기기는 NFC를 지원하지 않습니다"
            btnScan.isEnabled = false
            return
        }

        btnScan.setOnClickListener {
            if (!nfcAdapter.isEnabled) {
                tvStatus.text = "NFC가 비활성화되어 있습니다"
                startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
            } else {
                tvStatus.text = "카드를 태그해주세요"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(
            this,
            pendingIntent,
            nfcFilters,
            null
        )
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            val nfcF = NfcF.get(tag)

            try {
                nfcF?.connect()

                // FeliCa 시스템 코드 읽기
                val systemCode = byteArrayOf(0xFF.toByte(), 0xFF.toByte())
                val pollingCommand = buildPollingCommand(systemCode)
                val pollingResponse = nfcF?.transceive(pollingCommand)

                // 카드 정보 구성
                val cardInfo = StringBuilder()
                cardInfo.append("카드 타입: Sony FeliCa Lite-S\n")

                // IDm (제조 ID) 읽기
                val idm = bytesToHexString(tag?.id)
                cardInfo.append("시리얼 번호: $idm\n")

                // PMm (제조 파라미터) 읽기
                if (pollingResponse != null && pollingResponse.size >= 16) {
                    val pmm = bytesToHexString(pollingResponse.copyOfRange(8, 16))
                    cardInfo.append("PMm: $pmm\n")
                }

                // 시스템 코드 표시
                val systemCodeHex = bytesToHexString(systemCode)
                cardInfo.append("시스템 코드: $systemCodeHex")

                tvStatus.text = cardInfo.toString()

            } catch (e: Exception) {
                tvStatus.text = "카드 읽기 실패: ${e.message}"
            } finally {
                try {
                    nfcF?.close()
                } catch (e: Exception) {
                    // 연결 종료 실패 처리
                }
            }
        }
        }

    private fun bytesToHexString(bytes: ByteArray?): String {
        if (bytes == null) return ""
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }
    private fun buildPollingCommand(systemCode: ByteArray): ByteArray {
        return byteArrayOf(
            0x00.toByte(),  // 데이터 길이
            0x00.toByte(),  // 명령 코드
            systemCode[0],  // 시스템 코드
            systemCode[1],  // 시스템 코드
            0x01.toByte(),  // 요청 코드
            0x0F.toByte()   // 타임 슬롯
        )
    }
}