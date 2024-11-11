package com.NFCread.arcadetest

import CardDataManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.ProgressBar

import android.content.IntentFilter
import android.nfc.Tag
import android.os.CountDownTimer
import android.widget.EditText
import android.widget.Toast
import com.NFCread.arcadetest.models.CardData

import android.nfc.tech.NfcF


class MainActivity : AppCompatActivity() {
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var btnScan: Button
    private lateinit var btnViewSaved: Button
    private lateinit var btnSave: Button
    private lateinit var tvStatus: TextView
    private lateinit var timerProgress: ProgressBar

    private var pendingIntent: PendingIntent? = null
    private val nfcFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))

    private lateinit var cardDataManager: CardDataManager

    private var isEmulationActive = false
    private var emulationTimer: CountDownTimer? = null

    private var currentIdm: String = ""
    private var currentPmm: String = ""
    private var currentSystemCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 뷰 초기화
        btnScan = findViewById(R.id.btnScan)
        btnSave = findViewById(R.id.btnSave)
        btnViewSaved = findViewById(R.id.btnViewSaved)
        tvStatus = findViewById(R.id.tvStatus)
        timerProgress = findViewById(R.id.timerProgress)

        // 타이머 최대값 설정
        timerProgress.max = 20
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
        val seconds = (millisUntilFinished / 1000).toInt()
        tvStatus.text = "카드 활성화: ${seconds}초"
        timerProgress.progress = seconds
    }

    private fun activateEmulation(cardData: CardData) {
        // 이전 타이머가 있다면 취소
        emulationTimer?.cancel()

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
    override fun onDestroy() {
        super.onDestroy()
        emulationTimer?.cancel()
    }

    private fun handleFeliCaCard(tag: Tag?) {
        if (tag == null) {
            tvStatus.text = "카드를 읽을 수 없습니다"
            return
        }

        // 카드의 기술 목록 가져오기
        val techList = tag.techList

        if (!techList.contains("android.nfc.tech.NfcF")) {
            tvStatus.text = "해당 카드는 다른 카드입니다! 어뮤즈먼트 IC 카드만 스캔해 주세요!"
            return
        }

        try {
            val nfcF = NfcF.get(tag)
            if (nfcF == null) {
                tvStatus.text = "FeliCa 카드를 읽을 수 없습니다."
                return
            }

            nfcF.connect()

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

            nfcF.close()

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
}