package tech.fabernovel.nfctagsemulator

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.experimental.and

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

        val tagFromIntent = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        val tagId = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)

        content.text = tagFromIntent.toString() + "\n" + tagId.toHex()
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
