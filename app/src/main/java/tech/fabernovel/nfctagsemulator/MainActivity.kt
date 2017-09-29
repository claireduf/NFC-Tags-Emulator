package tech.fabernovel.nfctagsemulator

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_wifi.view.*


class MainActivity : AppCompatActivity() {


    private var nfcAdapter: NfcAdapter? = null

    private lateinit var adapter: DefaultAdapter
    private lateinit var layoutManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        layoutManager = LinearLayoutManager(this)
        recycler.layoutManager = layoutManager

        adapter = DefaultAdapter(
            { _, m -> handleWifi(m) },
            { v, m -> showMenu(v.more, m) })
        recycler.adapter = adapter

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        Dexter
            .withActivity(this)
            .withPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            .withListener(object : BasePermissionListener() {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    val listWifis = listWifis()
                    Log.d("###", "<" + listWifis.toString())
                    val finalList = getModels(listWifis)
                    Log.d("###", ">" + finalList.toString())
                    adapter.setData(finalList)
                }
            })
            .check()
    }

    private fun showMenu(anchor: View, model: WifiModel) {
        val popup = PopupMenu(this, anchor, Gravity.TOP or Gravity.END)
        popup.setOnMenuItemClickListener { when (it.itemId) {
            R.id.qrcode -> {
                showQrCode(model.network)
                true
            }
            R.id.edit -> {
                askForPassword(model.network)
                true
            }
            R.id.tag -> {
                writeTag(model.network)
                true
            }
            else -> false
        } }
        popup.inflate(R.menu.popup)
        if (nfcAdapter != null) {
            popup.menu.findItem(R.id.qrcode).isVisible = true
            popup.menu.findItem(R.id.tag).isVisible = true
        }
        popup.show()
    }

    private fun writeTag(network: WifiNetwork) {
        Toast.makeText(this, "writetag", LENGTH_SHORT).show()
    }

    private fun handleWifi(model: WifiModel) {
        when {
            model.missingPassword -> askForPassword(model.network)
            nfcAdapter == null -> showQrCode(model.network)
            else -> beam(model.network)
        }
    }

    private fun showQrCode(network: WifiNetwork) {
        val auth = if (network.authType == AuthType.OPEN) "" else "T:WPA;"
        val wifi = "WIFI:${auth}S:${network.ssid};P:${network.key};;"
        val bitmap = QRCodeGenerator.encodeAsBitmap(wifi)
        val imageView = ImageView(this)
        imageView.setImageBitmap(bitmap)
        AlertDialog.Builder(this)
            .setTitle("Prêt à scanner")
            .setView(imageView)
            .show()
    }

    private fun askForPassword(model: WifiNetwork) {
        Toast.makeText(this, "password", LENGTH_SHORT).show()
    }

    private fun beam(network: WifiNetwork) {
        val ndef = NfcUtils.generateNdefMessage(network)
        nfcAdapter?.setNdefPushMessage(ndef, this)
        AlertDialog.Builder(this)
            .setTitle("Prêt à beamer")
            .setMessage("Collez votre téléphone à celui du destinataire pour lui envoyer la configuration du réseau wifi ${network.ssid}")
            .show()
    }

    private fun getModels(listWifis: Triple<List<WifiNetwork>, List<WifiNetwork>, WifiNetwork?>): List<WifiModel> {
        val stored = emptyList<WifiNetwork>()
        val (known, reachable, connected) = listWifis

        return mergeStoredAndDeviceWifiLists(known, reachable, connected, stored)
    }

    private fun listWifis(): Triple<List<WifiNetwork>, List<WifiNetwork>, WifiNetwork?> {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val reachable = wifiManager.scanResults
        val reachableWifis = reachable.map {
            val capabs = it.capabilities
            val authType = when {
                capabs.contains("WPA") && capabs.contains("EAP") -> AuthType.WPA2_EAP
                capabs.contains("WPA") && capabs.contains("PSK") -> AuthType.WPA2_PSK
                else -> AuthType.OPEN
            }
            WifiNetwork(it.SSID, authType, null)
        }

        val list = wifiManager.configuredNetworks
        val listWifis = list.map {
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

    companion object {
        fun mergeStoredAndDeviceWifiLists(known: List<WifiNetwork>, reachable: List<WifiNetwork>, connected: WifiNetwork?, stored: List<WifiNetwork>): List<WifiModel>{
            val finalList = known.map { kElement ->
                val element = stored.find { (ssid) ->
                    kElement.ssid == ssid
                }

                WifiModel(element ?: kElement, element == null || element.key == null, Status.getReachable(reachable, kElement))
            }

            if (connected != null) {
                val element = stored.find{ (ssid) -> connected.ssid == ssid }
                val connectedModel = WifiModel(element ?: connected, element == null || element.key == null, Status.CONNECTED)

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
