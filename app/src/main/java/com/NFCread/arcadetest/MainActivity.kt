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
            val idm = bytesToHexString(tag?.id)

            // 카드 정보를 StringBuilder에 저장
            val cardInfo = StringBuilder()
            cardInfo.append("카드 타입: Sony FeliCa Lite-S RC-S966\n")
            cardInfo.append("표준: JIS 6319-4\n")
            cardInfo.append("시리얼 번호: $idm\n")

            // PMm 정보 추가 (고정값)
            cardInfo.append("PMm: 0x00F100000000143D0\n")

            // 시스템 코드 추가
            cardInfo.append("시스템 코드: 0x88B4\n")

            // TextView에 정보 표시
            tvStatus.text = cardInfo.toString()
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
}