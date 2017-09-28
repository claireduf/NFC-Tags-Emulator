package tech.fabernovel.nfctagsemulator

/**
 * Created by Tarek Belkahia on 28/09/2017.
 */
data class WifiNetwork(val ssid: String, val authType: AuthType, val key: String)

enum class AuthType {
    WPA_PSK,
    WPA2_PSK,
    WPA_EAP,
    WPA2_EAP,
    OPEN
}
