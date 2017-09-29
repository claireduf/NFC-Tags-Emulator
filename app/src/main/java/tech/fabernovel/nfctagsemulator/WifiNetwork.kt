package tech.fabernovel.nfctagsemulator

data class WifiModel(
    val network: WifiNetwork,
    val missingPassword: Boolean,
    val status: Status
)

enum class Status {
    CONNECTED, REACHABLE, UNREACHABLE
}

data class WifiNetwork(
    val ssid: String,
    val authType: AuthType,
    val key: String?
)

enum class AuthType {
    WPA_PSK,
    WPA2_PSK,
    WPA_EAP,
    WPA2_EAP,
    OPEN
}
