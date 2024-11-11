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
import android.os.CountDownTimer
import android.widget.EditText
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
    private fun enableButtons() {
        btnScan.isEnabled = true
        btnSave.isEnabled = true
        btnViewSaved.isEnabled = true
    }

    private fun disableButtons() {
        btnScan.isEnabled = false
        btnSave.isEnabled = false
        btnViewSaved.isEnabled = false
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateTimerUI(millisUntilFinished: Long) {
        val seconds = millisUntilFinished / 1000
        tvStatus.text = "카드 활성화: ${seconds}초"
        timerProgress.progress = seconds.toInt()
    }

    private fun activateEmulation(cardData: CardData) {
        isEmulationActive = true
        disableButtons()

        emulationTimer = object : CountDownTimer(20000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateTimerUI(millisUntilFinished)
            }

            override fun onFinish() {
                isEmulationActive = false
                enableButtons()
                updateTimerUI(0)
            }
        }.start()
    }

    private fun handleFeliCaCard(tag: Tag?) {
        if (!techList.contains("android.nfc.tech.NfcF")) {
            showMessage("해당 카드는 다른 카드입니다! 어뮤즈먼트 IC 카드만 스캔해 주세요!")
            return
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
            showMessage("저장할 카드 정보가 없습니다")
            return
        }

        // 카드 이름 입력 다이얼로그
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("카드 이름 입력")
            .setView(input)
            .setPositiveButton("저장") { _, _ ->
                val cardName = input.text.toString()
                val newCard = CardData(
                    name = cardName,
                    idm = currentIdm,
                    pmm = currentPmm,
                    systemCode = currentSystemCode
                )

                // 중복 체크
                if (cardDataManager.isCardExists(currentIdm)) {
                    showMessage("이미 해당 카드는 저장되어 있습니다!")
                } else {
                    cardDataManager.saveCard(newCard)
                    showMessage("카드가 저장되었습니다")
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showSavedCards() {
        if (isEmulationActive) {
            showMessage("현재 카드가 활성화되어 있습니다")
            return
        }

        val cards = cardDataManager.getCards()
        val items = cards.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("저장된 카드 선택")
            .setItems(items) { _, which ->
                val selectedCard = cards[which]
                activateEmulation(selectedCard)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // JSON 문자열을 CardData 객체로 변환하는 함수
    private fun parseCardData(jsonString: String): CardData {
        try {
            // JSON 파싱을 위한 정규식
            val idmPattern = "\"idm\":\\s*\"([^\"]+)\"".toRegex()
            val pmmPattern = "\"pmm\":\\s*\"([^\"]+)\"".toRegex()
            val systemCodePattern = "\"systemCode\":\\s*\"([^\"]+)\"".toRegex()

            // 각 값 추출
            val idm = idmPattern.find(jsonString)?.groupValues?.get(1) ?: ""
            val pmm = pmmPattern.find(jsonString)?.groupValues?.get(1) ?: ""
            val systemCode = systemCodePattern.find(jsonString)?.groupValues?.get(1) ?: ""

            return CardData(
                idm = idm,
                pmm = pmm,
                systemCode = systemCode
            )
        } catch (e: Exception) {
            // 파싱 실패 시 기본값 반환
            return CardData(
                idm = "",
                pmm = "",
                systemCode = ""
            )
        }
    }
}