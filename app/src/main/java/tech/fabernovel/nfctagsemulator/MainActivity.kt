package tech.fabernovel.nfctagsemulator

import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var vibratorService: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        vibratorService = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        showIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        showIntent(intent)
    }

    private fun showIntent(intent: Intent) {
        if (intent.action != NfcAdapter.ACTION_TECH_DISCOVERED) {
            content.text = intent.toString()
            return
        }

        vibrate()

        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        val tagId = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)
        val ndefMessages = intent.getParcelableArrayListExtra<NdefMessage>(NfcAdapter.EXTRA_NDEF_MESSAGES)

        val ndef = NdefFormatable.get(tag)
        val nfca = NfcA.get(tag)
        val mfc = MifareClassic.get(tag)

        val atqa = nfca.atqa
        val sak = nfca.sak

        val sectorCount = mfc.sectorCount
        val blockCount = mfc.blockCount
        val size = mfc.size
        val type = mfc.type

        content.text =
            """$tag
               |ID: ${tagId.toHex()}
               |NDEF: $ndefMessages
               |atqa: ${atqa.toHex()}
               |sak: $sak
               |sectorCount: $sectorCount
               |blockCount: $blockCount
               |size: $size
               |type: $type
               |""".trimMargin()
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
            vibratorService.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibratorService.vibrate(40)
        }
    }


    fun ByteArray.toHex() : String{
        val hexChars = "0123456789ABCDEF".toCharArray()
        val result = StringBuffer()

        forEach {
            val octet = it.toInt()
            val firstIndex = (octet and 0xF0).ushr(4)
            val secondIndex = octet and 0x0F
            result.append(hexChars[firstIndex])
            result.append(hexChars[secondIndex])
        }

        return result.toString()
    }

}
