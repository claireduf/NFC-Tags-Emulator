package tech.fabernovel.nfctagsemulator

import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import android.os.Vibrator
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.Adapter


class MainActivity : AppCompatActivity() {


    private lateinit var vibratorService: Vibrator
    //private lateinit var nfcAdapter: NfcAdapter

    private lateinit var adapter: Adapter<DefaultAdapter.ViewHolder>
    private lateinit var layoutManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        vibratorService = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        /*nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val ndef = makeNdef()
        nfcAdapter.setNdefPushMessage(ndef, this)*/

        layoutManager = LinearLayoutManager(this)
        recycler.layoutManager = layoutManager

        adapter = DefaultAdapter(listOf("item 1", "item2", "item3"))
        recycler.adapter = adapter
    }

    private fun makeNdef(): NdefMessage {
        return NfcUtils.generateNdefMessage(
            WifiNetwork("Zen-Alien", AuthType.WPA2_PSK, "Alienwantswifi"))
    }


    override fun onResume() {
        super.onResume()
        processIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: Intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED != intent.action) {
            return
        }
        val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        val msg = rawMsgs[0] as NdefMessage
        //content.text = String(msg.records[0].payload)
    }
}
