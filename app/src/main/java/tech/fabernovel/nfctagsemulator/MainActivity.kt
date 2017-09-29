package tech.fabernovel.nfctagsemulator

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {


    private var nfcAdapter: NfcAdapter? = null

    private lateinit var adapter: DefaultAdapter
    private lateinit var layoutManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        layoutManager = LinearLayoutManager(this)
        recycler.layoutManager = layoutManager

        adapter = DefaultAdapter(emptyList())
        recycler.adapter = adapter

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val wifi = WifiNetwork("Zen-Alien", AuthType.WPA2_PSK, "Alienwantswifi")
        val ndef = NfcUtils.generateNdefMessage(wifi)
        nfcAdapter?.setNdefPushMessage(ndef, this)

        val stored = emptyList<WifiNetwork>()
        val (known, reachable, connected) = listWifis()

        val mergedList = mergeStoredAndDeviceWifiLists(known, reachable, connected, stored)
    }


    fun List<WifiNetwork>.exclude(other: List<WifiNetwork>) =
        this.filter { mine -> other.none { its -> its.ssid == mine.ssid } }

    private fun listWifis(): Triple<List<WifiNetwork>, List<WifiNetwork>, WifiNetwork?> {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val reachable = wifiManager.scanResults
        val listWifis = reachable.map {
            val capabs = it.capabilities
            val authType = when {
                capabs.contains("WPA") && capabs.contains("EAP") -> AuthType.WPA2_EAP
                capabs.contains("WPA") && capabs.contains("PSK") -> AuthType.WPA2_PSK
                else -> AuthType.OPEN
            }
            WifiNetwork(it.SSID, authType, null)
        }

        val list = wifiManager.configuredNetworks
        val reachableWifis = list.map {
            val keyManagement = it.allowedKeyManagement
            val authType = when {
                keyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP) -> AuthType.WPA2_EAP
                keyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK) -> AuthType.WPA2_PSK
                else -> AuthType.OPEN
            }
            WifiNetwork(it.SSID, authType, null)
        }

        val wifiInfo = wifiManager.connectionInfo

        return Triple(listWifis, reachableWifis, reachableWifis.find { it.ssid == wifiInfo.ssid })
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

    companion object {
        fun mergeStoredAndDeviceWifiLists(known: List<WifiNetwork>, reachable: List<WifiNetwork>, connected: WifiNetwork?, stored: List<WifiNetwork>): List<WifiModel>{
            val finalList = known.map { kElement ->
                val element = stored.find { (ssid) ->
                    kElement.ssid == ssid
                }

                WifiModel(element ?: kElement, element != null && element.key != null, Status.getReachable(reachable, kElement))
            }

            if (connected != null) {
                val element = stored.find{ (ssid) -> connected.ssid == ssid }
                val connectedModel = WifiModel(element ?: connected, element != null && element.key != null, Status.CONNECTED)

                val connectedInFinalList = finalList.find{ (network) -> network.ssid == connectedModel.network.ssid}

                val trimList = if (connectedInFinalList != null) {
                    finalList.minus(connectedInFinalList)
                } else {
                    finalList
                }

                return trimList.plus(connectedModel)
            } else {
                return finalList
            }

        }
    }
}
