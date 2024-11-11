package com.NFCread.arcadetest

import android.app.AlertDialog
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
import android.widget.Toast
import com.NFCread.arcadetest.models.CardData

import com.NFCread.arcadetest.models.CardDataManager

class MainActivity : AppCompatActivity() {
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var btnScan: Button
    private lateinit var tvStatus: TextView
    private var pendingIntent: PendingIntent? = null
    private val nfcFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))

    private lateinit var cardDataManager: CardDataManager

    private var currentIdm: String = ""
    private var currentPmm: String = ""
    private var currentSystemCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cardDataManager = CardDataManager(this)

        // 저장 버튼 추가
        val btnSave = findViewById<Button>(R.id.btnSave)
        btnSave.setOnClickListener {
            // 현재 스캔된 카드 데이터 저장
            saveCurrentCard()
        }

        // 저장된 카드 목록 보기 버튼
        val btnViewSaved = findViewById<Button>(R.id.btnViewSaved)
        btnViewSaved.setOnClickListener {
            showSavedCards()
        }

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

            // 카드 기술 확인
            val techList = tag?.techList ?: emptyArray()

            when {
                // FeliCa 카드 확인
                techList.contains("android.nfc.tech.NfcF") -> {
                    handleFeliCaCard(tag)
                }
                // 다른 타입의 카드
                else -> {
                    tvStatus.text = "지원하지 않는 카드 타입입니다.\n" +
                            "감지된 기술: ${techList.joinToString(", ")}"
                }
            }
        }
    }

    private fun handleFeliCaCard(tag: Tag?) {
        try {
            val nfcF = NfcF.get(tag)
            if (nfcF == null) {
                tvStatus.text = "FeliCa 카드를 읽을 수 없습니다."
                return
            }

            nfcF.connect()

            // FeliCa 명령어 생성
            val polling = byteArrayOf(
                0x06,          // 길이
                0x00,          // 명령 코드 (Poll)
                0x88.toByte(), // 시스템 코드
                0xB4.toByte(), // 시스템 코드
                0x01,          // 요청 코드
                0x0F           // 타임 슬롯
            )

            val response = nfcF.transceive(polling)
            if (response != null && response.size >= 16) {
                // 스캔된 정보를 변수에 저장
                currentIdm = bytesToHexString(response.copyOfRange(2, 10))
                currentPmm = bytesToHexString(response.copyOfRange(10, 18))
                currentSystemCode = "88B4"

                val cardInfo = StringBuilder().apply {
                    append("카드 타입: Sony FeliCa\n")
                    append("IDm: $currentIdm\n")
                    append("PMm: $currentPmm\n")
                    append("시스템 코드: $currentSystemCode")
                }

                tvStatus.text = cardInfo.toString()
            } else {
                tvStatus.text = "카드 응답이 올바르지 않습니다."
            }

        } catch (e: Exception) {
            tvStatus.text = "카드 읽기 실패: ${e.message}"
            e.printStackTrace()
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
    private fun saveCurrentCard() {
        if (currentIdm.isEmpty()) {
            Toast.makeText(this, "저장할 카드 정보가 없습니다", Toast.LENGTH_SHORT).show()
            return
        }

        val currentCard = CardData(
            idm = currentIdm,
            pmm = currentPmm,
            systemCode = currentSystemCode
        )
        cardDataManager.saveCard(currentCard)
        Toast.makeText(this, "카드가 저장되었습니다", Toast.LENGTH_SHORT).show()
    }

    private fun showSavedCards() {
        val cards = cardDataManager.getCards()
        // 저장된 카드 목록을 보여주는 다이얼로그 표시
        AlertDialog.Builder(this)
            .setTitle("저장된 카드 목록")
            .setItems(cards.toTypedArray()) { _, _ -> }
            .setPositiveButton("확인", null)
            .show()
    }
}