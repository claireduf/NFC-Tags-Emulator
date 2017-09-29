package tech.fabernovel.nfctagsemulator

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatEditText
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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
    private lateinit var passwordPrefs: SharedPreferences
    private lateinit var authPrefs: SharedPreferences

    private var tagDialog: AlertDialog? = null

    private lateinit var adapter: DefaultAdapter
    private lateinit var layoutManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        passwordPrefs = getSharedPreferences("wifi_passwords", Context.MODE_PRIVATE)
        authPrefs = getSharedPreferences("wifi_auth", Context.MODE_PRIVATE)

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
                    refresh()
                }
            })
            .check()
    }

    private fun refresh() {
        val listWifis = listWifis()
        val finalList = getModels(listWifis)
        adapter.setData(finalList)
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
                waitForTag(model.network)
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

    private fun waitForTag(network: WifiNetwork) {
        val nfcIntent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        nfcIntent.putExtra("ssid", network.ssid)
        val pi = PendingIntent.getActivity(this, 0, nfcIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val tagDetected = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        nfcAdapter?.enableForegroundDispatch(this, pi, arrayOf(tagDetected), null)
        tagDialog = AlertDialog.Builder(this)
            .setTitle("Prêt à écrire le tag")
            .setMessage("Approcher le tag afin d'écrire les informations du réseau ${network.ssid}")
            .show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action != NfcAdapter.ACTION_TAG_DISCOVERED) {
            return
        }
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        val ssid = intent.getStringExtra("ssid")

        if (ssid != null) {
            val writeTag = NfcUtils.writeTag(getWifi(ssid), tag)
            if (writeTag) Toast.makeText(this, "Tag écrit", LENGTH_SHORT).show()
            else Toast.makeText(this, "Erreur lors de l'écriture", LENGTH_SHORT).show()
        }
        tagDialog?.dismiss()
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
        val editText = AppCompatEditText(this)
        editText.setText(model.key)
        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val inputLayout = TextInputLayout(this)
        inputLayout.addView(editText)
        inputLayout.isPasswordVisibilityToggleEnabled = true
        val padding = resources.getDimensionPixelSize(R.dimen.padding)
        inputLayout.setPadding(padding, 0, padding, 0)
        AlertDialog.Builder(this)
            .setTitle("Saisir le mot de passe")
            .setView(editText)
            .setPositiveButton("Enregistrer") { _, _ ->
                val key = if (editText.text.isEmpty()) null else editText.text.toString()
                updateWifi(model, key)
                refresh()
            }
            .setNegativeButton("Supprimer") { _, _ ->
                deleteWifi(model)
                refresh()
            }
            .show()
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
        val stored = getStoredWifis()
        val (known, reachable, connected) = listWifis

        return mergeStoredAndDeviceWifiLists(known, reachable, connected, stored)
    }

    private fun getStoredWifis(): List<WifiNetwork> {
        return authPrefs.all.map { WifiNetwork(it.key, AuthType.values()[it.value as Int], passwordPrefs.getString(it.key, null)) }
    }

    private fun deleteWifi(network: WifiNetwork) {
        authPrefs.edit().remove(network.ssid).apply()
        passwordPrefs.edit().remove(network.ssid).apply()
    }

    private fun updateWifi(network: WifiNetwork, key: String?) {
        authPrefs.edit().putInt(network.ssid, network.authType.ordinal).apply()
        passwordPrefs.edit().putString(network.ssid, key).apply()
    }

    private fun getWifi(ssid: String): WifiNetwork =
        WifiNetwork(ssid, AuthType.values()[authPrefs.getInt(ssid, 0)], passwordPrefs.getString(ssid, null))

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
            WifiNetwork(it.SSID.replace("\"",""), authType, null)
        }

        val list = wifiManager.configuredNetworks
        val listWifis = list.map {
            val keyManagement = it.allowedKeyManagement
            val authType = when {
                keyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP) -> AuthType.WPA2_EAP
                keyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK) -> AuthType.WPA2_PSK
                else -> AuthType.OPEN
            }
            WifiNetwork(it.SSID.replace("\"",""), authType, null)
        }

        val wifiInfo = wifiManager.connectionInfo

        return Triple(listWifis, reachableWifis, listWifis.find { it.ssid == wifiInfo.ssid.replace("\"","") })
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


